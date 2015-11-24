package home.my.jni_demo;

/**
 * Created by legendmohe on 15/11/13.
 */
public class AudioRawData {
    public final short[] data;
    public final int len;

    AudioRawData(short[] data, int len) {
        this.data = data;
        this.len = len;
    }
}
