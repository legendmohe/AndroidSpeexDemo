package home.my.jni_demo;

import android.util.Base64;
import android.util.Base64OutputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by legendmohe on 15/11/24.
 */
public class FileUtils {

    public static String FileToBase64(File srcFile) {
        InputStream inputStream = null;//You can get an inputStream using any IO API
        try {
            inputStream = new FileInputStream(srcFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            return null;
        }
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Base64OutputStream output64 = new Base64OutputStream(output, Base64.DEFAULT);
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output64.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            return null;
        }
        try {
            output64.close();
        } catch (IOException e) {
            return null;
        }

        String outputString = output.toString();
        return outputString;
    }
}
