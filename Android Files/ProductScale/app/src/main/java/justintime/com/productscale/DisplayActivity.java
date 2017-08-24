package justintime.com.productscale;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.ParticleEvent;
import io.particle.android.sdk.cloud.ParticleEventHandler;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class DisplayActivity extends AppCompatActivity {

    static ProgressBar progressBar;
    static ParticleDevice globalDevice;
    static TextView globalValueText;
    TextView deviceNameText;
    static TextView progressBarText;
    ImageView lockImage;
    private boolean locked;
    public static int fullWeight = 100;
    long weightChangeID;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch(item.getItemId()) {
            case R.id.logoutItem:
                Intent intent = new Intent(getApplicationContext(), MainScreen.class);
                intent.putExtra("fromLogOut", "true");
                ParticleCloudSDK.getCloud().logOut();
                startActivity(intent);
                break;
            case R.id.setFullWeight:
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            List<String> list = new ArrayList<String>();
                            list.add("a");
                            int value = Math.abs(globalDevice.callFunction("setFull", list));
                            progressBar.setMax(value);
                            progressBar.setProgress(value);
                            if(value != 0)
                                fullWeight = value;
                            else
                                fullWeight = 1;

                            final SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("justintime.com.productscale", Context.MODE_PRIVATE);
                            sharedPreferences.edit().putString("fullWeight", String.valueOf(fullWeight)).apply();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateScaleReading();
                                }
                            });
                        } catch (ParticleCloudException | ParticleDevice.FunctionDoesNotExistException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                t.start();
                break;
            case R.id.renameDevice:

                final Dialog dialog = new Dialog(DisplayActivity.this);
                dialog.setContentView(R.layout.cutom_dialog);

                TextView title = (TextView) dialog.findViewById(R.id.dialogTitle);
                TextView description = (TextView) dialog.findViewById(R.id.dialogDescription);
                final EditText nameText = (EditText) dialog.findViewById(R.id.dialogEditText);
                Button negativeBtn = (Button) dialog.findViewById(R.id.dialogNegativeBtn);
                Button positiveBtn = (Button) dialog.findViewById(R.id.dialogPositiveBtn);

                title.setText("Rename Device");
                description.setText("Give your scale a new name below. App will relaunch after renaming.\n\nNote: Name cannot contain spaces!");
                negativeBtn.setText("Cancel");
                positiveBtn.setText("Rename");
                negativeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                    }
                });

                positiveBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if(!nameText.getText().toString().contains(" ")) {
                                        globalDevice.setName(nameText.getText().toString());
                                    }else{
                                        Toaster.s(DisplayActivity.this,"Name Cannot Contain Spaces!");
                                    }
                                } catch (ParticleCloudException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        t.start();
                        //deviceNameText.setText(globalDevice.getName());
                        dialog.dismiss();
                        deviceNameText.setText("Device: " + nameText.getText().toString());
                    }
                });
                dialog.show();
                break;

            case R.id.changeDevice:
                Intent selectionIntent = new Intent(this, SelectionActivity.class);
                startActivity(selectionIntent);
                break;
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        final SharedPreferences sharedPreferences = this.getSharedPreferences("justintime.com.productscale", Context.MODE_PRIVATE);

        if(sharedPreferences.contains("fullWeight")){
            fullWeight = Integer.decode(sharedPreferences.getString("fullWeight", "100"));
        }

        final TextView valueText = (TextView) findViewById(R.id.displayText);
        globalValueText = valueText;
        deviceNameText = (TextView) findViewById(R.id.deviceNameText);

        Button updateBtn = (Button) findViewById(R.id.refreshBtn);
        Button tareBtn = (Button) findViewById(R.id.tareBtn);

        lockImage = (ImageView) findViewById(R.id.lockImageView);


        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setMax(100);
        progressBarText = (TextView) findViewById(R.id.progressText);
        //Get Device
        Bundle bundle = getIntent().getExtras();
        final ParticleDevice mDevice;
        if(!bundle.isEmpty()) {
            mDevice = (ParticleDevice) bundle.get("Device");
            globalDevice = mDevice;
            deviceNameText.setText("Device: " + mDevice.getName());
            sharedPreferences.edit().putString("deviceID", mDevice.getID());
        }
        else
            mDevice = null;


        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateScaleReading();
            }
        });

        tareBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                tareScale(mDevice);
                updateScaleReading();
            }
        });

        lockImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final int value = globalDevice.callFunction("lockScale");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(value == -1){
                                        locked = false;
                                        lockImage.setImageResource(R.drawable.custom_unlock);
                                    }else{
                                        locked = true;
                                        lockImage.setImageResource(R.drawable.custom_lock);
                                    }
                                }
                            });
                        } catch (ParticleCloudException | IOException | ParticleDevice.FunctionDoesNotExistException e) {
                            e.printStackTrace();
                        }
                    }
                });
                t.start();
            }
        });


        updateLockValue();
        updateScaleReading();
    }

    public void updateLockValue(){
        Async.executeAsync(globalDevice, new Async.ApiWork<ParticleDevice, Integer>() {
            @Override
            public Integer callApi(ParticleDevice particleDevice) throws ParticleCloudException, IOException {
                try {
                    return particleDevice.getIntVariable("lockedInt");
                } catch (ParticleDevice.VariableDoesNotExistException e) {
                    e.printStackTrace();
                    Log.i("Error", "Variable not found!");
                    return -1;
                }
            }

            @Override
            public void onSuccess(Integer value) {
                //Toaster.s(DisplayActivity.this, value.toString());
                if(value == 0){
                    locked = false;
                    lockImage.setImageResource(R.drawable.custom_unlock);
                }else if(value == 1){
                    locked = true;
                    lockImage.setImageResource(R.drawable.custom_lock);
                }
            }

            @Override
            public void onFailure(ParticleCloudException exception) {
                Log.i("Error", "Some Error Occured");
                exception.printStackTrace();
            }
        });
    }

    public void updateScaleReading(){

        Async.executeAsync(globalDevice, new Async.ApiWork<ParticleDevice, Double>() {
            @Override
            public Double callApi(ParticleDevice particleDevice) throws ParticleCloudException, IOException {
                try {
                    return particleDevice.getDoubleVariable("scaleValue");
                } catch (ParticleDevice.VariableDoesNotExistException e) {
                    e.printStackTrace();
                    Log.i("Error", "Variable not found!");
                    return (double)-1;
                }
            }

            @Override
            public void onSuccess(Double scaleValue) {
                scaleValue = Math.abs(scaleValue);
                if(scaleValue != -1 && fullWeight != 0){
                    globalValueText.setText("Weight: " + String.format("%.2f",scaleValue) + " g");
                    progressBar.setProgress(scaleValue.intValue());
                    double percent = (scaleValue/fullWeight)*100;
                    progressBarText.setText(String.format("%.0f",percent) + "%");
                }else{
                    globalValueText.setText("Weight: Error");
                }
            }

            @Override
            public void onFailure(ParticleCloudException exception) {
                Log.i("Error", "Some Error Occured");
                exception.printStackTrace();
            }
        });

    }

    public void tareScale(final ParticleDevice mDevice){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<String> list = new ArrayList<String>();
                    list.add("a");
                    mDevice.callFunction("tareScale", list);
                } catch (ParticleCloudException | ParticleDevice.FunctionDoesNotExistException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    public void subscribeToEvents(){

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    weightChangeID = ParticleCloudSDK.getCloud().subscribeToDeviceEvents("weightChange", globalDevice.getID(), new ParticleEventHandler() {
                        @Override
                        public void onEventError(Exception e) {
                            Toaster.s(DisplayActivity.this, "Error Subscribing to change event!");
                        }

                        @Override
                        public void onEvent(String eventName, ParticleEvent particleEvent) {
                            updateScaleReading();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (NotificationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        try {
            ParticleCloudSDK.getCloud().unsubscribeFromEventWithID(weightChangeID);
        } catch (ParticleCloudException e) {
            e.printStackTrace();
        }

        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateScaleReading();
        updateLockValue();
        subscribeToEvents();

        if (!isMyServiceRunning()){
            Intent serviceIntent = new Intent(getApplicationContext(), NotificationService.class);
            getApplicationContext().startService(serviceIntent);
        }else{
            Log.i("Service", "Service is already running!");
        }
    }
}