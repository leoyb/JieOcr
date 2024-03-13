package ocr.sdk;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ocr.sdk.common.DetectedObject;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.List;

public class ImgTest {

    public static void main(String[] args) throws Exception {
        String imageFile = "/Users/leo/Desktop/220.jpeg";
        Image image = ImageFactory.getInstance().fromFile(Paths.get(imageFile));
        image = image.resize(1260, 580, true);
        CanvasFrame canvasFrame = new CanvasFrame("播放器");
        Java2DFrameConverter java2DFrameConverter = new Java2DFrameConverter();


//        for(int i = 0; i< 20; i++) {
            long timeInferStart = System.currentTimeMillis();
            List<DetectedObject> items = new OcrRecognition().recognize2(image);
            long timeInferEnd = System.currentTimeMillis();
            System.out.println("time: " + (timeInferEnd - timeInferStart));
//        }

        BufferedImage bufferedImage =OcrRecognitionExample.drawPic(items, image);
        Frame fr = java2DFrameConverter.getFrame(bufferedImage);

        canvasFrame.showImage(fr); //播放

    }
}
