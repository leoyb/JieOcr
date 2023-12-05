package ocr.sdk;

import ai.djl.MalformedModelException;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.ModelNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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

    private static final Logger logger = LoggerFactory.getLogger(OcrRecognitionExample.class);

    private OcrRecognitionExample() throws ModelNotFoundException, MalformedModelException, IOException {
    }


    public static List<String> listImageFiles(File folder) {
        List<String> imageFiles = new ArrayList<>();

        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
//                        imageFiles.addAll(listImageFiles(file)); // 递归调用以处理子文件夹
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

    public static void main(String[] args) throws Exception {

        String path = "E:\\jieji";
        File folder = new File(path); // 替换为你的文件夹路径

        List<String> imageFiles = listImageFiles(folder);

        for (String fileName : imageFiles) {
            Path imageFile = Paths.get(path + "\\" + fileName);
            System.out.println(fileName);

            Image image = ImageFactory.getInstance().fromFile(imageFile);
            image = image.resize(828, 466, true);
            long timeInferStart = System.currentTimeMillis();
            List<DetectedObjects.DetectedObject> items = new OcrRecognition().recognizeImg(image);
            long timeInferEnd = System.currentTimeMillis();
            System.out.println("time: " + (timeInferEnd - timeInferStart));

        }
    }
}
