package ocr.sdk.common;

import ai.djl.modality.cv.output.BoundingBox;

public class DetectedObject {

    private BoundingBox boundingBox;
    private String text;

    private int person = -1;


    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getPerson() {
        return person;
    }

    public void setPerson(int person) {
        this.person = person;
    }
}
