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
import ocr.sdk.common.DetectedObject;
import ocr.sdk.common.FixedSizeQueue;
import ocr.sdk.detection.PpWordDetectionTranslator;
import ocr.sdk.recognition.PpWordRecognitionTranslator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
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


    static int poolSize = Runtime.getRuntime().availableProcessors() * 2;
    static BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(512);
    static RejectedExecutionHandler policy = new ThreadPoolExecutor.DiscardPolicy();
    static ThreadPoolExecutor executorService = new ThreadPoolExecutor(poolSize, poolSize,
            0, TimeUnit.SECONDS,
            queue,
            policy);

    static ZooModel detectionModel;
    static ZooModel recognitionModel;

    static String[] dictionary = {"锁链封印", "魔法召唤", "魔旋弹", "驱魔烈焰", "骷髅暴君", "巨石炎魔", "幻影死神", "修罗巨魔", "狂暴飞龙", "千年南瓜怪"};

    static Set<String> extinct = new HashSet<String>() {{
        add("锁链封印");
        add("魔旋弹");
        add("魔法召唤");
        add("驱魔烈焰");
    }};
    static FixedSizeQueue awardQueue = new FixedSizeQueue(5);


    //1.识别大奖
    Rectangle rectangle0 = new Rectangle(0, 0.5, 1, 0.4);
    //2.识别下面4个分数
    private static final Rectangle rectangle1 = new Rectangle(0.098, 0.945, 0.045, 0.054);
    private static final Rectangle rectangle2 = new Rectangle(0.353, 0.945, 0.043, 0.054);
    private static final Rectangle rectangle3 = new Rectangle(0.61, 0.945, 0.043, 0.054);
    private static final Rectangle rectangle4 = new Rectangle(0.86, 0.945, 0.043, 0.054);

    private static final Set<Integer> POS = new HashSet<Integer>() {{
        add(1);
        add(2);
        add(3);
        add(4);
    }};
    private static final Map<Integer, Rectangle> rectangleMap = new HashMap<Integer, Rectangle>() {{
        put(1, rectangle1);
        put(2, rectangle2);
        put(3, rectangle3);
        put(4, rectangle4);
    }};


    static {
        try {
            //只需要加载一次，不用每次都加载
            detectionModel = ModelZoo.loadModel(Criteria.builder()
                    .optEngine("PaddlePaddle")
                    .optModelName("inference")
                    .setTypes(Image.class, DetectedObjects.class)
                    .optModelPath(Paths.get("src/models/ch_PP-OCRv3_det_infer"))
                    .optTranslator(new PpWordDetectionTranslator())
                    .optProgress(new ProgressBar())
                    .build());
            recognitionModel = ModelZoo.loadModel(
                    Criteria.builder()
                            .optEngine("PaddlePaddle")
                            .optModelName("inference")
                            .setTypes(Image.class, String.class)
                            .optModelPath(Paths.get("src/models/ch_PP-OCRv3_rec_infer"))
                            .optProgress(new ProgressBar())
                            .optTranslator(new PpWordRecognitionTranslator())
                            .build());
        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            throw new RuntimeException(e);
        }
    }



    public List<DetectedObject> recognizeImg(Image image) throws Exception {
        //先识别上面的大奖
        List<DetectedObjects.DetectedObject> result = new ImgTask(ImgTask.price, image, rectangle0, detectionModel, recognitionModel).call();
        List<DetectedObject> results = filterAward(result);
        List<DetectedObject> finalObj = new ArrayList<>();
        finalObj.addAll(results);

        //对有大奖的玩家识别倍数
        for (DetectedObject obj : results) {
            result = new ImgTask(ImgTask.score, image, rectangleMap.get(obj.getPerson()), detectionModel, recognitionModel).call();
            finalObj.addAll(filterScore(result, obj.getPerson()));
        }
        return finalObj;
    }



    /**
     * 对中奖结果处理
     * @param input
     * @return
     */
    List<DetectedObject> filterAward(List<DetectedObjects.DetectedObject> input) {
        List<DetectedObject> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(input)) {
            return result;
        }

        Set<Integer> appeal = new HashSet<>();
        for (DetectedObjects.DetectedObject object : input) {
            Rectangle rectangle = object.getBoundingBox().getBounds();
            if (rectangle.getY() < 0.5) {
                continue;
            }

            String text = object.getClassName();
            System.out.println("input is:" + text);
            text = findClosestString(text);
            System.out.println("ClosestString is:" + text);
            if (StringUtils.isBlank(text) || extinct.contains(text)) {
                continue;
            }

            int pos = getPartition(rectangle.getX());
            appeal.add(pos);
            LinkedList<String> de = awardQueue.getDeque(pos);
            text = awardQueue.doMatch(pos, text, true);
            System.out.println("match is:" + text);
            if (StringUtils.isBlank(text)) {
                continue;
            }
            DetectedObject detectedObject = new DetectedObject();
            detectedObject.setText(text);
            detectedObject.setBoundingBox(object.getBoundingBox());
            detectedObject.setPerson(pos);
            result.add(detectedObject);

        }
        Set<Integer> diff = new HashSet<>(POS);
        // 移除所有已出现的元素
        diff.removeAll(appeal);
        for (Integer pos : diff) {
            awardQueue.doMatch(pos, "NA", true);
        }
        return result;
    }

    /**
     * 对倍数处理
     * @param input
     * @return
     */
    List<DetectedObject> filterScore(List<DetectedObjects.DetectedObject> input, int pos) {
        List<DetectedObject> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(input)) {
            return result;
        }

        for (DetectedObjects.DetectedObject object : input) {
            String text = object.getClassName();
            if (StringUtils.isBlank(text) || "0".equals(text)) {
                text = "100";
            }
            text = text.replace(")", "");
            text = text.replace("(", "");
            if (text.length() > 4) {
                text = "1000";
            }
            DetectedObject detectedObject = new DetectedObject();
            detectedObject.setText(text);
            detectedObject.setBoundingBox(object.getBoundingBox());
            detectedObject.setPerson(pos);
            result.add(detectedObject);
        }

        return result;
    }

    // 找出字典中与新字符串最接近的字符串
    public static String findClosestString(String newString) {
        String closestString = "";
        int minDistance = Integer.MAX_VALUE;

        for (String dictString : dictionary) {
            int distance = calculateEditDistance(newString, dictString);
            if (distance < minDistance) {
                minDistance = distance;
                closestString = dictString;
            }
        }

        int count = 0;
        for (int i = 0; i < newString.length(); i++) {
            if (closestString.contains(String.valueOf(newString.charAt(i)))) {
                count++;
            }
        }

        return count > 0 ? closestString : null;
    }

    private static int calculateEditDistance(String s, String t) {
        int[][] dp = new int[s.length() + 1][t.length() + 1];

        for (int i = 0; i <= s.length(); i++) {
            for (int j = 0; j <= t.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else if (s.charAt(i - 1) == t.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j], Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
                }
            }
        }

        return dp[s.length()][t.length()];
    }


    public int getPartition(double boxX) {
        if (boxX >= 0 && boxX < 0.25) {
            return 1; // 第一份
        } else if (boxX >= 0.25 / 4 && boxX < 0.5) {
            return 2; // 第二份
        } else if (boxX >= 0.5 && boxX < 0.75) {
            return 3; // 第三份
        } else if (boxX >= 0.75 && boxX < 1) {
            return 4; // 第四份
        } else {
            return -1; // 方框位置超出图片高度范围
        }
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
