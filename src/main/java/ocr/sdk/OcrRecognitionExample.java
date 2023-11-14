package ocr.sdk;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.opencv.OpenCVImageFactory;
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
import java.util.List;

/**
 * OCR 文字识别
 * OCR model for text recognition.
 *
 * @author owen
 * @date 20223-11-07
 */
public final class OcrRecognitionExample {

    private static final Logger logger = LoggerFactory.getLogger(OcrRecognitionExample.class);

    private OcrRecognitionExample() {
    }

    public static void main(String[] args) throws IOException, ModelException, TranslateException {
        Path imageFile = Paths.get("src/main/resources/4.jpg");
        Image image = OpenCVImageFactory.getInstance().fromFile(imageFile);
        //只识别下半张图片提升识别速度
        image=image.getSubImage(0, image.getHeight()/2, image.getWidth(), image.getHeight()/2);
        OcrV3Detection detection = new OcrV3Detection();
        OcrV3Recognition recognition = new OcrV3Recognition();
        try (ZooModel detectionModel = ModelZoo.loadModel(detection.detectCriteria());
             Predictor<Image, DetectedObjects> detector = detectionModel.newPredictor();
             ZooModel recognitionModel = ModelZoo.loadModel(recognition.recognizeCriteria());
             Predictor<Image, String> recognizer = recognitionModel.newPredictor()) {

            long timeInferStart = System.currentTimeMillis();
            DetectedObjects detections = recognition.predict(image, detector, recognizer);

            long timeInferEnd = System.currentTimeMillis();
            System.out.println("time: " + (timeInferEnd - timeInferStart));

            List<DetectedObjects.DetectedObject> items = detections.items();

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

            logger.info("{}", detections);
        }
    }
}
