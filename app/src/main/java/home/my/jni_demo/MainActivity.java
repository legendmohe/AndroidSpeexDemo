package home.my.jni_demo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity
        implements ProcessSpeexRunnable.ProcessSpeexListener , WriteSpeexOggFileRunnable.WriteSpeexOggListener{

    private static final String TAG = "MainActivity";
    private TextView mTextView;
    private Button btn_RecordStart;
    private Button btn_RecordStop;
    private Button btn_RecordPlay;

    private boolean isRecording;
    List<byte[]> mCurrentRecordData = null;

    private AudioRecorderRunnable mAudioRunnable;
    private ProcessSpeexRunnable mProcessSpeexRunnable;
    private WriteSpeexOggFileRunnable mWriteSpeexOggFileRunnable;

    private OkHttpClient mHttpClient = new OkHttpClient();
    private MediaType mMediaType = MediaType.parse("text/html; charset=UTF-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_scrolling);

        mTextView = (TextView) this.findViewById(R.id.jni_text_view);
        mTextView.setText("hello world");

        btn_RecordStart = (Button) findViewById(R.id.btn_RecordStart);
        btn_RecordPlay = (Button) findViewById(R.id.btn_PlayRecord);
        btn_RecordStop = (Button) findViewById(R.id.btn_RecordStop);
        btn_RecordStop.setEnabled(false);

        btn_RecordStart.setOnClickListener(click);
        btn_RecordStop.setOnClickListener(click);
        btn_RecordPlay.setOnClickListener(click);
    }

    private View.OnClickListener click = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_RecordStart:
                    start();
                    break;
                case R.id.btn_RecordStop:
                    stopRecording();
                    break;
                case R.id.btn_PlayRecord:
                    playRecord();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 开始录音
     */
    protected void start() {
        try {
            isRecording=true;
            btn_RecordStart.setEnabled(false);
            btn_RecordStop.setEnabled(true);

            if (mAudioRunnable != null) {
                mAudioRunnable.stop();
                mAudioRunnable = null;
            }
            if (mProcessSpeexRunnable != null) {
                mProcessSpeexRunnable.stop();
                mProcessSpeexRunnable = null;
            }

            if (mWriteSpeexOggFileRunnable != null) {
                mWriteSpeexOggFileRunnable.stop();
                mWriteSpeexOggFileRunnable = null;
            }

            LinkedBlockingQueue<AudioRawData> blockingDeque = new LinkedBlockingQueue<>();
            File rootDir = Environment.getExternalStorageDirectory();

            mWriteSpeexOggFileRunnable = new WriteSpeexOggFileRunnable(new File(rootDir, "test.spx"), this);
            mProcessSpeexRunnable = new ProcessSpeexRunnable(blockingDeque, this);
            mAudioRunnable = new AudioRecorderRunnable(blockingDeque);

            new Thread(mWriteSpeexOggFileRunnable).start();
            new Thread(mProcessSpeexRunnable).start();
            new Thread(mAudioRunnable).start();

            Toast.makeText(MainActivity.this, "开始录音", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 录音结束
     */
    protected void stopRecording() {
        if (isRecording) {
            if (mAudioRunnable != null) {
                mAudioRunnable.stop();
                mAudioRunnable = null;
            }
            if (mProcessSpeexRunnable != null) {
                mProcessSpeexRunnable.stop();
                mProcessSpeexRunnable = null;
            }
            isRecording=false;

            btn_RecordStart.setEnabled(true);
            btn_RecordStop.setEnabled(false);
            Toast.makeText(MainActivity.this, "录音结束", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (isRecording) {
            if (mAudioRunnable != null) {
                mAudioRunnable.stop();
                mAudioRunnable = null;
            }
            if (mProcessSpeexRunnable != null) {
                mProcessSpeexRunnable.stop();
                mProcessSpeexRunnable = null;
            }
            if (mWriteSpeexOggFileRunnable != null) {
                mWriteSpeexOggFileRunnable.stop();
                mWriteSpeexOggFileRunnable = null;
            }
        }
        super.onDestroy();
    }

    @Override
    public void onProcess(byte[] data, int len) {
        mWriteSpeexOggFileRunnable.putData(data, len);
    }

    @Override
    public void onProcessFinish(List<byte[]> data) {
        mCurrentRecordData = data;
        mWriteSpeexOggFileRunnable.stop();
        Log.d(TAG, "finish process speex data frames: " + data.size());
    }

    private Request generateRequest(String payload, String type) {
        String url = "http://119.29.102.249:8888/mqtt_base64?t=12345678&type=" + type;
        RequestBody body = RequestBody.create(mMediaType, payload);
        return new Request.Builder()
                .url(url)
                .post(body)
                .build();
    }

    private void sendMessageRequest(Request request) {
        mHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTextView.setText("http error");
                    }
                });
            }

            @Override public void onResponse(Response response) throws IOException {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                final String str = response.body().string();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTextView.setText(str);
                    }
                });
            }
        });
    }

    private void playRecord() {
        btn_RecordPlay.setEnabled(false);
        if (mCurrentRecordData == null || mCurrentRecordData.size() == 0)
            return;

        int bufferSizeInBytes = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 2 * bufferSizeInBytes, AudioTrack.MODE_STREAM);
        audioTrack.play();

        Speex speex = new Speex();
        speex.init();

        int maxFrameSize = speex.getFrameSize();
        for (byte[] bytes: mCurrentRecordData) {
            short[] decData = new short[maxFrameSize];
            int dec = speex.decode(bytes, decData, bytes.length);
            if (dec > 0) {
                audioTrack.write(decData, 0, dec);
            }
        }
        audioTrack.stop();
        audioTrack.release();
        speex.close();
        btn_RecordPlay.setEnabled(true);
    }

    @Override
    public void onWriteSpeexOggFileFinished(File file) {
        String payload = FileUtils.FileToBase64(file);
        Log.d(TAG, "base 64:" + payload);
        Log.d(TAG, "file path:" + file.getAbsolutePath());
        Request request = generateRequest(payload, "message");
        sendMessageRequest(request);
    }
}
