package home.my.jni_demo;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by legendmohe on 15/11/13.
 */
public class AudioRecorderRunnable implements Runnable {
    private static final String TAG = "AudioRecorderRunnable";

    private boolean mStopped = false;
    private LinkedBlockingQueue<AudioRawData> mBufferQueue;

    AudioRecorderRunnable(LinkedBlockingQueue<AudioRawData> queue){
        this.mBufferQueue = queue;
    }

    @Override
    public void run() {
        if (this.mStopped) {
            Log.w(TAG, "ProcessRunnable is running.");
            return;
        }
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        AudioRecord recorder = null;
        short[][]   buffers  = new short[256][160];
        int         ix       = 0;
        try {
            int n = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    8000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    n*10);
//                recorder.setPositionNotificationPeriod(8000);
            recorder.startRecording();
            while(!this.mStopped) {
                short[] buffer = buffers[ix++ % buffers.length];
                n = recorder.read(buffer,0,buffer.length);
                this.mBufferQueue.offer(new AudioRawData(buffer, n));
            }
        } catch(Throwable x) {
            Log.w(TAG, "Error reading voice audio", x);
        } finally {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
            }
            Log.d(TAG, "thread exit.");
        }
    }

    public void stop() {
        this.mStopped = true;
    }

}
