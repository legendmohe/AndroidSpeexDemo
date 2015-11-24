package home.my.jni_demo;

/**
 * Created by legendmohe on 15/11/18.
 */
public class AudioUtils {

    private static final float MAX_REPORTABLE_DB = 90.3087f;
    private static final float MAX_REPORTABLE_AMP = 32767f;


    private static int getRawAmplitude(byte[] data, int len) {
        if (len <= 0 || data == null || data.length <= 0) {
            return 0;
        }

        int sum = 0;
        for (int i = 0; i < len; i++) {
            sum += Math.abs(data[i]);
        }
        return sum / len;
    }

    public static float getAmplitude(byte[] data, int len) {
        return (float) (MAX_REPORTABLE_DB + (20 * Math.log10(getRawAmplitude(data, len) / MAX_REPORTABLE_AMP)));
    }
}
