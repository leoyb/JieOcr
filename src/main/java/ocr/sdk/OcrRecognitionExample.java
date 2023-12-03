package ocr.sdk;

import ai.djl.MalformedModelException;
import ai.djl.ModelException;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.opencv.OpenCVImageFactory;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;

import ocr.sdk.common.ImageUtils;
import ocr.sdk.detection.OcrV3Detection;
import ocr.sdk.recognition.OcrV3Recognition;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
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
public final class OcrRecognitionExample {

    private static final Logger logger = LoggerFactory.getLogger(OcrRecognitionExample.class);

    private OcrRecognitionExample() throws ModelNotFoundException, MalformedModelException, IOException {
    }

    static int poolSize = Runtime.getRuntime().availableProcessors() * 2;
    static BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(512);
    static RejectedExecutionHandler policy = new ThreadPoolExecutor.DiscardPolicy();
    static ThreadPoolExecutor executorService = new ThreadPoolExecutor(poolSize, poolSize,
            0, TimeUnit.SECONDS,
            queue,
            policy);

    static OcrV3Detection detection = new OcrV3Detection();
    static OcrV3Recognition recognition = new OcrV3Recognition();
    static ZooModel detectionModel;
    static ZooModel recognitionModel;


    static {
        try {
            //只需要加载一次，不用每次都加载
            detectionModel = ModelZoo.loadModel(detection.detectCriteria());
            recognitionModel = ModelZoo.loadModel(recognition.recognizeCriteria());
        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            throw new RuntimeException(e);
        }
    }


    private static List<DetectedObjects.DetectedObject> dealImg(Image image) throws TranslateException, InterruptedException {

        //1.识别大奖
        Rectangle rectangle0 = new Rectangle(0, 0.5, 1, 0.4);
        //2.识别下面4个分数
        Rectangle rectangle1 = new Rectangle(0.1, 0.945, 0.043, 0.054);
        Rectangle rectangle2 = new Rectangle(0.353, 0.945, 0.043, 0.054);
        Rectangle rectangle3 = new Rectangle(0.61, 0.945, 0.043, 0.054);
        Rectangle rectangle4 = new Rectangle(0.86, 0.945, 0.043, 0.054);

        List<Callable> tasks = new ArrayList<>();
        tasks.add(new ImgTask(ImgTask.price, image, rectangle0, recognition, detectionModel, recognitionModel));
        tasks.add(new ImgTask(ImgTask.score, image, rectangle1, recognition, detectionModel, recognitionModel));
        tasks.add(new ImgTask(ImgTask.score, image, rectangle2, recognition, detectionModel, recognitionModel));
        tasks.add(new ImgTask(ImgTask.score, image, rectangle3, recognition, detectionModel, recognitionModel));
        tasks.add(new ImgTask(ImgTask.score, image, rectangle4, recognition, detectionModel, recognitionModel));

        return batchExecute(executorService, tasks);
    }


    public static List<DetectedObjects.DetectedObject> batchExecute(ThreadPoolExecutor executorService, List<Callable> tasks) {

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


    public static void main(String[] args) throws IOException, ModelException, TranslateException, InterruptedException {
        Path imageFile = Paths.get("src/main/resources/1e14f50c-3981-4d84-90e9-b6076670adcftest.png");
        Image image = OpenCVImageFactory.getInstance().fromFile(imageFile);


        long timeInferStart = System.currentTimeMillis();
        List<DetectedObjects.DetectedObject> items = dealImg(image);
        long timeInferEnd = System.currentTimeMillis();
        System.out.println("time: " + (timeInferEnd - timeInferStart));

        // 转 BufferedImage 解决中文乱码问题
        Mat wrappedImage = (Mat) image.getWrappedImage();
        BufferedImage bufferedImage = ImageUtils.mat2Image(wrappedImage);
        for (DetectedObjects.DetectedObject item : items) {
            Rectangle rectangle = item.getBoundingBox().getBounds();
            int x = (int) (rectangle.getX() * image.getWidth());
            int y = (int) (rectangle.getY() * image.getHeight());
            int width = (int) (rectangle.getWidth() * image.getWidth());
            int height = (int) (rectangle.getHeight() * image.getHeight());

            ImageUtils.drawImageRect(bufferedImage, x, y, width, height);
            ImageUtils.drawImageText(bufferedImage, item.getClassName(), x, y);
        }

        Mat image2Mat = ImageUtils.image2Mat(bufferedImage);
        image = OpenCVImageFactory.getInstance().fromImage(image2Mat);
        ImageUtils.saveImage(image, "ocr_result.png", "src/main/resources/");

        wrappedImage.release();
        image2Mat.release();

    }
}
