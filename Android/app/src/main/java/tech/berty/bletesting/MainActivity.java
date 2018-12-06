package tech.berty.bletesting;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.app.Activity;
import android.widget.Button;
import android.view.View;

import tech.berty.bletesting.Manager;


public class MainActivity extends AppCompatActivity {

    static String ma = "d5406e06-ad83-4622-a7af-38dbe8c4fabe";
    static String peerID = "QmNXdnmutcuwAG4BcvgrEwc3znArQc9JTq6ALFote1rBoU";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        final Manager manager = Manager.getInstance();
        final Activity curActivity = this;

        manager.setmContext(this.getApplicationContext());
        manager.setMa(ma);
        manager.setPeerID(peerID);

        final Button startButton = findViewById(R.id.button4);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                manager.initScannerAndAdvertiser(curActivity);
            }
        });

        final Button stopButton = findViewById(R.id.button2);
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                manager.closeScannerAndAdvertiser();
            }
        });
    }
}
