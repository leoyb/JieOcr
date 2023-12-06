package ocr.sdk.recognition;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.util.Utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class PpWordRecognitionTranslator implements Translator<Image, String> {
    private List<String> table;

    public PpWordRecognitionTranslator() throws FileNotFoundException {
        FileInputStream fis = new FileInputStream("src/models/ch_PP-OCRv3_rec_infer/ppocr_keys_v1.txt");
        table = Utils.readLines(fis, true);
        table.add(0, "blank");
        table.add("");
    }

    @Override
    public void prepare(TranslatorContext ctx) throws IOException {
    }

    @Override
    public String processOutput(TranslatorContext ctx, NDList list) throws IOException {
        StringBuilder sb = new StringBuilder();
        NDArray tokens = list.singletonOrThrow();

        long[] indices = tokens.get(0).argMax(1).toLongArray();
        boolean[] selection = new boolean[indices.length];
        Arrays.fill(selection, true);
        for (int i = 1; i < indices.length; i++) {
            if (indices[i] == indices[i - 1]) {
                selection[i] = false;
            }
        }

        int lastIdx = 0;
        for (int i = 0; i < indices.length; i++) {
            if (selection[i] == true && indices[i] > 0 && !(i > 0 && indices[i] == lastIdx)) {
                sb.append(table.get((int) indices[i]));
            }
        }
        list.close();
        ctx.close();
        return sb.toString();
    }

    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
        NDArray img = input.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);
        int imgC = 3;
        int imgH = 48;
        int imgW = 320;

        float max_wh_ratio = (float) imgW / (float) imgH;

        int h = input.getHeight();
        int w = input.getWidth();
        float wh_ratio = (float) w / (float) h;

        max_wh_ratio = Math.max(max_wh_ratio,wh_ratio);
        imgW = (int)(imgH * max_wh_ratio);

        int resized_w;
        if (Math.ceil(imgH * wh_ratio) > imgW) {
            resized_w = imgW;
        } else {
            resized_w = (int) (Math.ceil(imgH * wh_ratio));
        }
        NDArray resized_image = NDImageUtils.resize(img, resized_w, imgH);
        resized_image = resized_image.transpose(2, 0, 1).toType(DataType.FLOAT32,false);
        resized_image.divi(255f).subi(0.5f).divi(0.5f);
        NDArray padding_im = ctx.getNDManager().zeros(new Shape(imgC, imgH, imgW), DataType.FLOAT32);
        padding_im.set(new NDIndex(":,:,0:" + resized_w), resized_image);

        padding_im = padding_im.flip(0);
        padding_im = padding_im.expandDims(0);
        return new NDList(padding_im);
    }

    @Override
    public Batchifier getBatchifier() {
        return null;
    }

}
