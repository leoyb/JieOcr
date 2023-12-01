package ocr.sdk;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.repository.zoo.ZooModel;
import ocr.sdk.recognition.OcrV3Recognition;

import java.util.List;
import java.util.concurrent.Callable;

public class ImgTask implements Callable<List<DetectedObjects.DetectedObject>> {

    public static String score = "score";
    public static String price = "price";

    private String type;

    private Image image;
    private Rectangle rectangle;

    private OcrV3Recognition recognition;


    private ZooModel detectionModel;
    private ZooModel recognitionModel;

    public ImgTask(String type, Image image, Rectangle rectangle,OcrV3Recognition recognition, ZooModel detectionModel, ZooModel recognitionModel) {
        this.type = type;
        this.image = image;
        this.rectangle = rectangle;
        this.recognition = recognition;
        this.detectionModel = detectionModel;
        this.recognitionModel = recognitionModel;
    }

    @Override
    public List<DetectedObjects.DetectedObject> call() throws Exception {
        Predictor<Image, DetectedObjects> detector = detectionModel.newPredictor();
        Predictor<Image, String> recognizer = recognitionModel.newPredictor();
        // 模拟耗时操作
        Image img = image.getSubImage((int) (image.getWidth() * rectangle.getX()), (int) (image.getHeight() * rectangle.getY()), (int) (image.getWidth() * rectangle.getWidth()), (int) (image.getHeight() * rectangle.getHeight()));
        List<DetectedObjects.DetectedObject> result = recognition.predict(img, detector, recognizer).items();
        if(score.equals(type) && result.size() == 1) {
            DetectedObjects.DetectedObject detectedObject = new DetectedObjects.DetectedObject(result.get(0).getClassName(), result.get(0).getProbability(), rectangle);
            result.set(0, detectedObject);
        } else {
            for (int i = 0; i< result.size() ; i++) {
                Rectangle ori = (Rectangle)result.get(i).getBoundingBox();
                Rectangle last = new Rectangle(ori.getX(), ori.getY() *0.45 + 0.45 , ori.getWidth(), ori.getHeight() );
                DetectedObjects.DetectedObject detectedObject = new DetectedObjects.DetectedObject(result.get(i).getClassName(), result.get(i).getProbability(), last);
                result.set(i, detectedObject);
            }
        }
        return result;
    }
}
