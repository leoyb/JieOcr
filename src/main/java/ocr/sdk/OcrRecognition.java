package ocr.sdk;

import ai.djl.MalformedModelException;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ocr.sdk.detection.PpWordDetectionTranslator;
import ocr.sdk.recognition.PpWordRecognitionTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * OCR 文字识别
 * OCR model for text recognition.
 *
 * @author owen
 * @date 20223-11-07
 */
public class OcrRecognition {

    private static final Logger logger = LoggerFactory.getLogger(OcrRecognition.class);


    static final int poolSize = Runtime.getRuntime().availableProcessors() * 2;
    static final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100);
    static final RejectedExecutionHandler policy = new ThreadPoolExecutor.DiscardPolicy();
    static final ThreadPoolExecutor executorService = new ThreadPoolExecutor(poolSize, poolSize, 0, TimeUnit.SECONDS, queue, policy);

    static final ZooModel detectionModel;
    static final ZooModel recognitionModel;


    //1.识别大奖
    static final Rectangle rectangle0 = new Rectangle(0, 0.5, 1, 0.4);
    //2.识别下面4个分数
    static final Rectangle rectangle1 = new Rectangle(0.092, 0.945, 0.048, 0.054);
    static final Rectangle rectangle2 = new Rectangle(0.353, 0.945, 0.043, 0.054);
    static final Rectangle rectangle3 = new Rectangle(0.61, 0.945, 0.043, 0.054);
    static final Rectangle rectangle4 = new Rectangle(0.86, 0.945, 0.043, 0.054);


    static {
        try {
            //只需要加载一次，不用每次都加载
            detectionModel = ModelZoo.loadModel(Criteria.builder().optEngine("PaddlePaddle").optModelName("inference").setTypes(Image.class, DetectedObjects.class).optModelPath(Paths.get("src/models/ch_PP-OCRv3_det_infer")).optTranslator(new PpWordDetectionTranslator()).optProgress(new ProgressBar()).build());
            recognitionModel = ModelZoo.loadModel(Criteria.builder().optEngine("PaddlePaddle").optModelName("inference").setTypes(Image.class, String.class).optModelPath(Paths.get("src/models/ch_PP-OCRv3_rec_infer")).optProgress(new ProgressBar()).optTranslator(new PpWordRecognitionTranslator()).build());
        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            throw new RuntimeException(e);
        }
    }


    public List<DetectedObjects.DetectedObject> recognizeImg(Image image) throws Exception {
        List<DetectedObjects.DetectedObject> results = new ArrayList<>();
        List<DetectedObjects.DetectedObject> result = new ImgTask(ImgTask.price, image, rectangle0, detectionModel, recognitionModel).call();
        results.addAll(result);
        result = new ImgTask(ImgTask.score, image, rectangle1, detectionModel, recognitionModel).call();
        results.addAll(result);
        result = new ImgTask(ImgTask.score, image, rectangle2, detectionModel, recognitionModel).call();
        results.addAll(result);
        result = new ImgTask(ImgTask.score, image, rectangle3, detectionModel, recognitionModel).call();
        results.addAll(result);
        result = new ImgTask(ImgTask.score, image, rectangle4, detectionModel, recognitionModel).call();
        results.addAll(result);

        return results;
    }


    public List<DetectedObjects.DetectedObject> batchExecute(ThreadPoolExecutor executorService, List<Callable> tasks) {

        // 创建任务列表
        List<Future<List>> futures = new ArrayList<>();

        // 多线程执行
        for (Callable task : tasks) {
            Future<List> future = executorService.submit(task);
            futures.add(future);
        }

        // 设置超时时间
        long timeout = 5; // 超时时间为5秒

        // 汇总结果
        List<DetectedObjects.DetectedObject> results = new ArrayList<>();
        for (Future<List> future : futures) {
            try {
                List<DetectedObjects.DetectedObject> result = future.get(timeout, TimeUnit.SECONDS);
                results.addAll(result);
            } catch (InterruptedException e) {
                System.err.println("任务被中断");
            } catch (ExecutionException e) {
                System.err.println("任务执行出错");
            } catch (TimeoutException e) {
                System.err.println("任务超时");
                future.cancel(true); // 取消超时任务
            }
        }

        return results;
    }
}
