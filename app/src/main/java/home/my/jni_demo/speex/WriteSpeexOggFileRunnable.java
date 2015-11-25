package home.my.jni_demo.speex;

import android.util.Log;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;


public class WriteSpeexOggFileRunnable implements Runnable {
	private final static String TAG = "SpeexWriter";
	private final Object mutex = new Object();
    private final File mFile;

    private SpeexWriteClient mSpeexWriteClient = new SpeexWriteClient();
	private volatile boolean mIsRecording;
	private processedData mData;
	private ConcurrentLinkedQueue<processedData> mDataQueue;

	public static int write_packageSize = 1024;

	public WriteSpeexOggFileRunnable(File file) {
        this.mFile = file;
		mDataQueue = new ConcurrentLinkedQueue<>();
		mSpeexWriteClient.start(file, SpeexWriteClient.MODE_NB, SpeexWriteClient.SAMPLERATE_8000, true);
	}

	public void run() {
		Log.d(TAG, "write thread runing");

        mIsRecording = true;
		while (this.isRecording()) {
            mData = mDataQueue.poll();
            if (mData != null) {
                Log.d(TAG, "mData size=" + mData.size);
                mSpeexWriteClient.writePacket(mData.processed, mData.size);
            }
		}
        mSpeexWriteClient.stop();

        Log.d(TAG, "write thread exit");
	}

	public void putData(final byte[] buf, int size) {
//		Log.d(TAG, "after convert. size=====================[640]:" + size);
		processedData data = new processedData(buf, size);
		mDataQueue.offer(data);
	}

	public void stop() {
        synchronized (mutex) {
            mIsRecording = false;
        }
	}

    public File getOutputFile() {
        return this.mFile;
    }

	public boolean isRecording() {
        synchronized (mutex) {
            return mIsRecording;
        }
	}

	class processedData {
        processedData(byte[] buf, int size) {
            System.arraycopy(buf, 0, this.processed, 0, size);
            this.size = size;
        }
		private int size;
		private byte[] processed = new byte[write_packageSize];
	}

}