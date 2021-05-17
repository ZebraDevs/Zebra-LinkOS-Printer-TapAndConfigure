package nl.rkeb.TapAndConfigure;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.TapAndConfigure.R;
import com.zebra.printconnectintentswrapper.*;
/*import com.zebra.printconnectintentswrapper.PCIntentsBaseSettings;
import com.zebra.printconnectintentswrapper.PCPassthroughPrint;
import com.zebra.printconnectintentswrapper.PCPassthroughPrintSettings;
import com.zebra.printconnectintentswrapper.PCPrinterStatus;
import com.zebra.printconnectintentswrapper.PCUnselectPrinter;
 */

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView et_results;
    private ScrollView sv_results;
    private String mStatus = "";
    private static boolean mOptmizeRefresh = true;
    /*
    Handler and runnable to scroll down textview
 */
    private Handler mScrollDownHandler = null;
    private Runnable mScrollDownRunnable = null;


    @Override
    protected void onResume() {
        super.onResume();
        mScrollDownHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onPause() {
        if(mScrollDownRunnable != null)
        {
            mScrollDownHandler.removeCallbacks(mScrollDownRunnable);
            mScrollDownRunnable = null;
            mScrollDownHandler = null;
        }
        super.onPause();
    }


    private void askPermission(String permission,int requestCode) {
        if (ContextCompat.checkSelfPermission(this,permission)!= PackageManager.PERMISSION_GRANTED){
            // We Don't have permission
            ActivityCompat.requestPermissions(this,new String[]{permission},requestCode);
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @SuppressLint("DefaultLocale")
    private StringBuilder getFontFile(File sdcard, String fileName) {

        File file = new File(sdcard, fileName);
        StringBuilder fileData = new StringBuilder();

        // write the header and data to the printer
        fileData.append("! CISDFCRC16\n");
        fileData.append(String.format("%04d\n", 0));
        fileData.append(fileName + "\n");
        int size = (int) file.length();
        fileData.append(String.format("%08X\n", size));
        fileData.append(String.format("%04d\n", 0));  // checksum, 0000 => ignore checksum

        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();

        } catch (IOException e) {
            addLineToResults(e.toString());
        }
        String str = new String(bytes, StandardCharsets.UTF_16);
        fileData.append(str);

        return fileData;
    }

    private StringBuilder getConfigFile(File sdcard, String fileName) {
        //Get the text file
        File file = new File(sdcard, fileName);
        StringBuilder fileData = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                fileData.append(line);
                fileData.append('\n');
            }
            br.close();
        } catch (IOException e) {

        }
        return fileData;
    }

    private void myLooper () {
//        if (!myLooperActive) {
//            myLooperActive = true;
//            Looper.prepare();
//        }
    }

    private void myLooperQuit() {
//        if (myLooperActive) {
//            myLooperActive = false;
//            getMainLooper().quitSafely();
//        }
    }

    private void printerPassthrough(StringBuilder prnData, String whichData) {


        PCPassthroughPrint passthroughPrint = new PCPassthroughPrint(MainActivity.this);
        PCPassthroughPrintSettings settings = new PCPassthroughPrintSettings() {{
            mPassthroughData = prnData.toString();
            mEnableTimeOutMechanism = true;
        }};

        passthroughPrint.execute(settings, new PCPassthroughPrint.onPassthroughResult() {
            @Override
            public void success(PCPassthroughPrintSettings settings) {
                addLineToResults(getCurrentDateTime() + "Succeeded: " + whichData);
            }

            @Override
            public void error(String errorMessage, int resultCode, Bundle resultData, PCPassthroughPrintSettings settings) {
                addLineToResults(getCurrentDateTime() + "Error: " + whichData);
                addLineToResults(getCurrentDateTime() + errorMessage);
            }

            @Override
            public void timeOut(PCPassthroughPrintSettings settings) {
                addLineToResults(getCurrentDateTime() + "Timeout: " + whichData);
            }
        });
    }

    private void printerStatus() {

        PCPrinterStatus printerStatus = new PCPrinterStatus(MainActivity.this);
        PCIntentsBaseSettings settings = new PCIntentsBaseSettings()
        {{
            mCommandId = "printerstatus";
        }};

        printerStatus.execute(settings, new PCPrinterStatus.onPrinterStatusResult() {
            @Override
            public void success(PCIntentsBaseSettings settings, HashMap<String, String> printerStatusMap) {
                addLineToResults(getCurrentDateTime() + "Succeeded: Printer Status");
                addLineToResults(getCurrentDateTime() + "Printer Status:");
                for (HashMap.Entry<String, String> entry : printerStatusMap.entrySet()) {
                    addLineToResults(getCurrentDateTime() + entry.getKey() + " = " + entry.getValue());
                }
            }

            @Override
            public void error(String errorMessage, int resultCode, Bundle resultData, PCIntentsBaseSettings settings) {
                addLineToResults(getCurrentDateTime() + "Printer status Error");
                addLineToResults(getCurrentDateTime() + errorMessage);
            }

            @Override
            public void timeOut(PCIntentsBaseSettings settings) {
                addLineToResults(getCurrentDateTime() + "Timeout: Printer status");
            }
        });
    }

    private void unselectPrinter() {

        PCUnselectPrinter unselectPrinter = new PCUnselectPrinter(MainActivity.this);
        PCIntentsBaseSettings settings = new PCIntentsBaseSettings()
        {{
            mCommandId = "unselectPrinter";
        }};

        unselectPrinter.execute(settings, new PCUnselectPrinter.onUnselectPrinterResult() {
            @Override
            public void success(PCIntentsBaseSettings settings) {
                addLineToResults(getCurrentDateTime() + "Unselect Printer succeeded");
            }

            @Override
            public void error(String errorMessage, int resultCode, Bundle resultData, PCIntentsBaseSettings settings) {
                addLineToResults(getCurrentDateTime() + "Error while trying to unselect printer: " + errorMessage);
            }

            @Override
            public void timeOut(PCIntentsBaseSettings settings) {
                addLineToResults(getCurrentDateTime() + "Timeout while trying to unselect printer");
            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        askPermission(Manifest.permission.READ_EXTERNAL_STORAGE,1);
        askPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,1);

        et_results = findViewById(R.id.et_results);
        sv_results = findViewById(R.id.sv_results);

        Button setup = findViewById(R.id.button);
        setup.setOnClickListener(v -> {

            addLineToResults(getCurrentDateTime() + "****************************");
            addLineToResults(getCurrentDateTime() + "*** START PRINTER CONFIG ***");
            printerStatus();

            //*Don't* hardcode "/sdcard"
            File sdcard = Environment.getExternalStorageDirectory();
            printerPassthrough(getFontFile(sdcard,"Alvania.ttf"),"Font File Alvania.ttf");
            printerPassthrough(getConfigFile(sdcard, "config.txt"), "Config File config.txt");
        });
    }

    private String getCurrentDateTime() {

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss - ", Locale.getDefault());
        return sdf.format(new Date());
    }


    private void addLineToResults(final String lineToAdd)
    {
        mStatus += lineToAdd + "\n";
        updateAndScrollDownTextView();
    }


    private void updateAndScrollDownTextView()
    {
        if(mOptmizeRefresh)
        {
            if(mScrollDownRunnable == null)
            {
                mScrollDownRunnable = () -> {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            et_results.setText(mStatus);
                            sv_results.post(() -> sv_results.fullScroll(ScrollView.FOCUS_DOWN));
                        }
                    });
                };
            }
            else
            {
                // A new line has been added while we were waiting to scroll down
                // reset handler to repost it....
                mScrollDownHandler.removeCallbacks(mScrollDownRunnable);
            }
            mScrollDownHandler.postDelayed(mScrollDownRunnable, 300);
        }
        else
        {
            MainActivity.this.runOnUiThread(() -> {
                et_results.setText(mStatus);
                sv_results.fullScroll(ScrollView.FOCUS_DOWN);
            });
        }
    }
}
