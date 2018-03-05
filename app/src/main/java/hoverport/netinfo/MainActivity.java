package hoverport.netinfo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private boolean  permission= false;
    private Thread thread;
    private String t;
    private TextView textView = null;
    private PrintWriter printWriter;
    private BufferedReader in;
    private ExecutorService mExecutorService = null;
    private String receiveMsg;
    Handler textViewHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1 && textView!=null) {
                textView.setText(t);
            }
            super.handleMessage(msg);
        }
    };
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        switch (requestCode) {
            case 0:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    permission= true;
                }
                break;
            default:
                break;
        }
    }
    @Override
    protected void onDestroy() {
        thread.interrupt();
        super.onDestroy();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }else{
            permission = true;
        }
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        mExecutorService = Executors.newCachedThreadPool();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket = null;

                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    if (!permission) continue;
            /* first you wanna get a telephony manager by asking the system service */
                    TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

            /* then you can query for all the neighborhood cells */
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        List<CellInfo> cellInfo = tm.getAllCellInfo();
                        t = "";
                        for (int i = 0; i < cellInfo.size(); i++) {
                            int cid = 0, strength = 0;

                            if(cellInfo.get(i).isRegistered()) {
                                if (cellInfo.get(i) instanceof CellInfoWcdma) {
                                    CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfo.get(i);
                                    strength = cellInfoWcdma.getCellSignalStrength().getDbm();
                                    cid = cellInfoWcdma.getCellIdentity().getCid();
                                    Log.wtf("wcdma", cellInfoWcdma.toString());
                                    t += cellInfoWcdma.toString();
                                } else if (cellInfo.get(i) instanceof CellInfoGsm) {
                                    CellInfoGsm cellInfogsm = (CellInfoGsm) cellInfo.get(i);
                                    strength = cellInfogsm.getCellSignalStrength().getDbm();
                                    cid = cellInfogsm.getCellIdentity().getCid();
                                    Log.wtf("gsm", cellInfogsm.toString());
                                    t += cellInfogsm.toString();
                                } else if (cellInfo.get(i) instanceof CellInfoLte) {
                                    CellInfoLte cellInfoLte = (CellInfoLte) cellInfo.get(i);
                                    strength = cellInfoLte.getCellSignalStrength().getDbm();
                                    cid = cellInfoLte.getCellIdentity().getPci();
                                    Log.wtf("lte", cellInfoLte.toString());
                                    t += cellInfoLte.toString();
                                } else if (cellInfo.get(i) instanceof CellInfoCdma) {
                                    CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfo.get(i);
                                    strength = cellInfoCdma.getCellSignalStrength().getCdmaDbm();
                                    cid = cellInfoCdma.getCellIdentity().getNetworkId();
                                    Log.wtf("cdma", cellInfoCdma.toString());
                                    t += cellInfoCdma.toString();
                                }
                                t += "\n";
                            }
                        }
                        Message msg =textViewHandler.obtainMessage();
                        msg.what = 1;
                        msg.sendToTarget();
                        try{
                            socket = new Socket(getUSBTetheredIP(),5099);
                            printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                                    socket.getOutputStream(), "UTF-8")), true);
                            printWriter.println(t);
                            socket.close();
                        }catch (Exception e){

                        }

                    }
                }

            }
        });
        thread.start();
        Log.wtf("ip",getUSBTetheredIP());
        textView.setText(getUSBTetheredIP());
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(getUSBTetheredIP(),5099);
                    printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                            socket.getOutputStream(), "UTF-8")), true);
                    printWriter.println("hahahahahhahahahahah");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        //thread1.start();
    }
    public static String getUSBTetheredIP() {

        BufferedReader bufferedReader = null;
        String ips="";

        try {
            bufferedReader = new BufferedReader(new FileReader("/proc/net/arp"));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Log.wtf("haha",line);
                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    String ip = splitted[0];
                    String mac = splitted[3];
                    if (mac.matches("..:..:..:..:..:..")) {
                        if (mac.matches("00:00:00:00:00:00")) {
                            //Log.d("DEBUG", "Wrong:" + mac + ":" + ip);
                        } else {
                            //Log.d("DEBUG", "Correct:" + mac + ":" + ip);
                            ips = ip;
                            break;
                        }
                    }
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            try {
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ips;
    }
}
