package ocr.sdk;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ImgTask implements Callable<List<DetectedObjects.DetectedObject>> {

    public static String score = "score";
    public static String price = "price";

    private String type;

    private Image image;
    private Rectangle rectangle;


    private ZooModel detectionModel;
    private ZooModel recognitionModel;

    public ImgTask(String type, Image image, Rectangle rectangle, ZooModel detectionModel, ZooModel recognitionModel) {
        this.type = type;
        this.image = image;
        this.rectangle = rectangle;
        this.detectionModel = detectionModel;
        this.recognitionModel = recognitionModel;
    }

    @Override
    public List<DetectedObjects.DetectedObject> call() throws Exception {
        Predictor<Image, DetectedObjects> detector = detectionModel.newPredictor();
        Predictor<Image, String> recognizer = recognitionModel.newPredictor();
        // 模拟耗时操作
        Image img = image.getSubImage((int) (image.getWidth() * rectangle.getX()), (int) (image.getHeight() * rectangle.getY()), (int) (image.getWidth() * rectangle.getWidth()), (int) (image.getHeight() * rectangle.getHeight()));
        List<DetectedObjects.DetectedObject> result = predict(img, detector, recognizer).items();
        if (score.equals(type) && result.size() == 1) {
            DetectedObjects.DetectedObject detectedObject = new DetectedObjects.DetectedObject(result.get(0).getClassName(), result.get(0).getProbability(), rectangle);
            result.set(0, detectedObject);
        } else {
            for (int i = 0; i < result.size(); i++) {
                Rectangle ori = (Rectangle) result.get(i).getBoundingBox();
                Rectangle last = new Rectangle(ori.getX(), ori.getY() * 0.45 + 0.45, ori.getWidth(), ori.getHeight());
                DetectedObjects.DetectedObject detectedObject = new DetectedObjects.DetectedObject(result.get(i).getClassName(), result.get(i).getProbability(), last);
                result.set(i, detectedObject);
            }
        }
        detector.close();
        recognizer.close();
        return result;
    }


    private DetectedObjects predict(Image image, Predictor<Image, DetectedObjects> detector, Predictor<Image, String> recognizer)
            throws TranslateException {
        DetectedObjects detections = detector.predict(image);

        List<DetectedObjects.DetectedObject> boxes = detections.items();
        List<String> names = new ArrayList<>();
        List<Double> prob = new ArrayList<>();
        List<BoundingBox> rect = new ArrayList<>();

        for (int i = 0; i < boxes.size(); i++) {
            Image subImg = getSubImage(image, boxes.get(i).getBoundingBox());
            String name = recognizer.predict(subImg);
            System.out.println(name);
            names.add(name);
            prob.add(-1.0);
            rect.add(boxes.get(i).getBoundingBox());
        }

        DetectedObjects detectedObjects = new DetectedObjects(names, prob, rect);
        return detectedObjects;
    }

    private Image getSubImage(Image img, BoundingBox box) {
        Rectangle rect = box.getBounds();
        double[] extended = extendRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        int width = img.getWidth();
        int height = img.getHeight();
        int[] recovered = {
                (int) (extended[0] * width),
                (int) (extended[1] * height),
                (int) (extended[2] * width),
                (int) (extended[3] * height)
        };
        return img.getSubImage(recovered[0], recovered[1], recovered[2], recovered[3]);
    }

    private double[] extendRect(double xmin, double ymin, double width, double height) {
        double centerx = xmin + width / 2;
        double centery = ymin + height / 2;
        if (width > height) {
            width += height * 2.0;
            height *= 3.0;
        } else {
            height += width * 2.0;
            width *= 3.0;
        }
        double newX = centerx - width / 2 < 0 ? 0 : centerx - width / 2;
        double newY = centery - height / 2 < 0 ? 0 : centery - height / 2;
        double newWidth = newX + width > 1 ? 1 - newX : width;
        double newHeight = newY + height > 1 ? 1 - newY : height;
        return new double[]{newX, newY, newWidth, newHeight};
    }
}
