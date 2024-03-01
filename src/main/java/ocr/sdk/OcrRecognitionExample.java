package ocr.sdk;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.Rectangle;
import ocr.sdk.common.DetectedObject;
import ocr.sdk.common.ImageUtils;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * OCR 文字识别
 * OCR model for text recognition.
 *
 * @author owen
 * @date 20223-11-07
 */
public final class OcrRecognitionExample {


    public static void main(String[] args) throws Exception {
        //视频流地址，目前rtmp流可用
        String urlString = "rtmp://iklive.gndiwei.com/live/wsyT";

        //这里要用while循环一直监控这路视频，如果有多路视频监控，则要开多线程调用
        while (true) {
            try {
                //服务器部署
//                detectVideo(urlString);

                //本地测试使用
                testVideo(urlString);
            } catch (Exception e) {
                System.out.printf("throw error", e.getMessage());
            }
        }
    }


    /**
     * 从视频流识别内容
     *
     * @param urlString
     * @throws Exception
     */
    public static void detectVideo(String urlString) throws Exception {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(urlString);
        grabber.setOption("rtsp_transport", "tcp");
        grabber.setImageWidth(960);
        grabber.setImageHeight(540);
        grabber.setFrameRate(25);
        grabber.start();

        // Read and process the video frames
        Frame frame;
        long count = 0;

        //对视频进行处理
        while ((frame = grabber.grab()) != null) {
            if (count % 25 == 0) {
                //每隔25帧识别一次
                Java2DFrameConverter java2DFrameConverter = new Java2DFrameConverter();
                BufferedImage bufferedImage = java2DFrameConverter.getBufferedImage(frame);

                if (null == bufferedImage) continue;

                Image image = ImageFactory.getInstance().fromImage(bufferedImage);

                //文字识别
                long timeInferStart = System.currentTimeMillis();
                //对图像进行识别，并获取返回内容和坐标
                List<DetectedObject> items = new OcrRecognition().recognizeImg(image);
                long timeInferEnd = System.currentTimeMillis();
                System.out.println("time: " + (timeInferEnd - timeInferStart));

            }
            count++;
        }

        // Stop the FFmpegFrameGrabber
        grabber.stop();

        // Release the resources
        grabber.close();
    }


    public static void testVideo(String urlString) throws Exception {
        CanvasFrame canvasFrame = new CanvasFrame("播放器");
        canvasFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(urlString);
        grabber.setOption("rtsp_transport", "tcp");
        grabber.setImageWidth(960);
        grabber.setImageHeight(540);
        grabber.setFrameRate(25);
        grabber.start();

        // Read and process the video frames
        Frame frame;
        long count = 0;

        //对视频进行处理
        while ((frame = grabber.grab()) != null) {
            if (count % 25 == 0) {
                //每隔25帧识别一次
                Java2DFrameConverter java2DFrameConverter = new Java2DFrameConverter();
                BufferedImage bufferedImage = java2DFrameConverter.getBufferedImage(frame);

                if (null == bufferedImage) continue;

                Image image = ImageFactory.getInstance().fromImage(bufferedImage);

                //文字识别
                long timeInferStart = System.currentTimeMillis();
                //对图像进行识别，并获取返回内容和坐标
                List<DetectedObject> items = new OcrRecognition().recognizeImg(image);
                long timeInferEnd = System.currentTimeMillis();
                System.out.println("time: " + (timeInferEnd - timeInferStart));

                bufferedImage = drawPic(items, image);
                Frame fr = java2DFrameConverter.getFrame(bufferedImage);

                canvasFrame.showImage(fr); //播放

            }
            count++;
        }

        // Stop the FFmpegFrameGrabber
        grabber.stop();

        // Release the resources
        grabber.close();
    }
    /**
     * 将识别结果画在图片上
     *
     * @param items
     * @param image
     * @return
     * @throws Exception
     */
    public static BufferedImage drawPic(List<DetectedObject> items, Image image) throws Exception {
        BufferedImage bufferedImage = (BufferedImage) image.getWrappedImage();
        for (DetectedObject item : items) {
            Rectangle rectangle = item.getBoundingBox().getBounds();
            int x = (int) (rectangle.getX() * image.getWidth());
            int y = (int) (rectangle.getY() * image.getHeight());
            int width = (int) (rectangle.getWidth() * image.getWidth());
            int height = (int) (rectangle.getHeight() * image.getHeight());

            //打印坐标
            ImageUtils.drawImageRect(bufferedImage, x, y, width, height);
            //打印文字
            ImageUtils.drawImageText(bufferedImage, item.getText(), x, y);
        }

        return bufferedImage;
    }


    /**
     * 对本地图片进行识别
     *
     * @throws Exception
     */
    public static void detectPic() throws Exception {
        CanvasFrame canvasFrame = new CanvasFrame("播放器");
        canvasFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        String path = "/Users/leo/Desktop/pic";
        File folder = new File(path); // 替换为你的文件夹路径
        List<String> imageFiles = listImageFiles(folder);
        for (int i = 0; i < 500; i++) {
            for (String fileName : imageFiles) {
                Path imageFile = Paths.get(path + "/" + fileName);
                System.out.println(fileName);
                Java2DFrameConverter java2DFrameConverter = new Java2DFrameConverter();
                Image image = ImageFactory.getInstance().fromFile(imageFile);
                List<DetectedObject> items = new OcrRecognition().recognizeImg(image);
                BufferedImage bufferedImage = drawPic(items, image);
                Frame fr = java2DFrameConverter.getFrame(bufferedImage);

                canvasFrame.showImage(fr); //播放
            }
        }
    }


    public static List<String> listImageFiles(File folder) {
        List<String> imageFiles = new ArrayList<>();

        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        imageFiles.addAll(listImageFiles(file)); // 递归调用以处理子文件夹
                    } else {
                        if (isImageFile(file)) {
                            imageFiles.add(file.getName());
                        }
                    }
                }
            }
        }

        return imageFiles;
    }

    public static boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".bmp");
    }
}
