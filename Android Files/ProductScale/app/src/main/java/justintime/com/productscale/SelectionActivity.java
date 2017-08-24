package justintime.com.productscale;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.util.List;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.SetupCompleteIntentBuilder;
import io.particle.android.sdk.devicesetup.SetupResult;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class SelectionActivity extends AppCompatActivity {

    List<ParticleDevice> devices;
    ListView deviceListView;
    ListAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);

        final SharedPreferences sharedPreferences = this.getSharedPreferences("justintime.com.productscale", Context.MODE_PRIVATE);

        final String username = sharedPreferences.getString("lastUsername","");
        final String password = sharedPreferences.getString("lastPassword", "");

        deviceListView = (ListView) findViewById(R.id.deviceListView);

        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Integer>() {
            @Override
            public Integer callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                ParticleCloudSDK.getCloud().logIn(username,password);
                final List<ParticleDevice> devices = ParticleCloudSDK.getCloud().getDevices();
                if(devices != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addToArray(devices);
                        }
                    });

                }else{
                    return -1;
                }

                return 1;
            }

            @Override
            public void onSuccess(Integer result) {

            }

            @Override
            public void onFailure(ParticleCloudException exception) {

            }
        });


        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ParticleDevice mDevice = devices.get(position);
                sharedPreferences.edit().putString("deviceID", mDevice.getID()).apply();
                Intent intent = new Intent(getApplicationContext(), DisplayActivity.class);
                intent.putExtra("Device", mDevice);
                startActivity(intent);
            }
        });

    }

    void addToArray(List<ParticleDevice> d){
        try{
            devices = d;
            adapter = new ParticleDeviceArrayAdapter(this,devices,R.layout.particle_list_layout);
            deviceListView.setAdapter(adapter);
            Log.i("Size", devices.size() + "");
        }catch(NullPointerException e){
            e.printStackTrace();
        }
    }
}
