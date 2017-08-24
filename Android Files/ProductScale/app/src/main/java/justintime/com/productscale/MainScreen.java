package justintime.com.productscale;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.IOException;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class MainScreen extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        ParticleCloudSDK.init(this);

        final SharedPreferences sharedPreferences = this.getSharedPreferences("justintime.com.productscale", Context.MODE_PRIVATE);

        final EditText passwordText = (EditText) findViewById(R.id.passwordText);
        final EditText usernameText = (EditText) findViewById(R.id.emailText);
        final TextView errorText = (TextView) findViewById(R.id.errorTextView);
        final ProgressBar loginProgressBar = (ProgressBar) findViewById(R.id.loginProgressBar);
        final CheckBox keepLoggedInBox = (CheckBox) findViewById(R.id.keeploggedInBox);
        keepLoggedInBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(buttonView.isChecked()){
                    sharedPreferences.edit().putString("keepLoggedIn", "true").apply();
                }else{
                    sharedPreferences.edit().putString("keepLoggedIn", "false").apply();
                }
            }
        });

        if(sharedPreferences.getString("keepLoggedIn", "false").equals("true")){
            keepLoggedInBox.setChecked(true);
        }else{
            keepLoggedInBox.setChecked(false);
        }

        final Button loginBtn = (Button) findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginProgressBar.setVisibility(View.VISIBLE);
                errorText.setVisibility(View.INVISIBLE);
                loginBtn.setClickable(false);

                Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Integer>() {

                    private ParticleDevice mDevice;
                    private String username = null;
                    private String password = null;

                    @Override
                    public Integer callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {

                        username = usernameText.getText().toString();
                        password = passwordText.getText().toString();

                        if(password.length() >= 1 && username.length() >=3){
                            particleCloud.logIn(username, password);
                            if(sharedPreferences.getString("deviceID", " ").length() > 1){
                                mDevice = particleCloud.getDevice(sharedPreferences.getString("deviceID", " "));
                            }
                        }else{
                            return -1;
                        }

                        return 1;
                    }

                    @Override
                    public void onSuccess(@NonNull Integer result) {
                        if(result == 1) {
                            //Toaster.s(MainScreen.this, "Logged In!");
                            sharedPreferences.edit().putString("lastUsername", username).apply();
                            sharedPreferences.edit().putString("lastPassword", password).apply();

                            if(sharedPreferences.getString("deviceID", " ").length() > 1){
                                Intent intent = new Intent(getApplicationContext(), DisplayActivity.class);
                                intent.putExtra("Device", mDevice);
                                startActivity(intent);
                            }else {
                                Intent intent = new Intent(getApplicationContext(), SelectionActivity.class);
                                startActivity(intent);
                            }
                        }else if(result == -1){
                            loginProgressBar.setVisibility(View.INVISIBLE);
                            errorText.setText("Please enter a username and password!");
                            errorText.setVisibility(View.VISIBLE);
                            loginBtn.setClickable(true);
                        }
                    }

                    @Override
                    public void onFailure(ParticleCloudException exception) {
                        loginProgressBar.setVisibility(View.INVISIBLE);
                        Toaster.s(MainScreen.this, exception.getBestMessage());
                        errorText.setText("Incorrect Username or Password!");
                        errorText.setVisibility(View.VISIBLE);
                        loginBtn.setClickable(true);
                        exception.printStackTrace();
                    }
                });
            }
        });


        Bundle bundle = getIntent().getExtras();
        if(bundle != null && !bundle.isEmpty()) {
            if (sharedPreferences.contains("lastUsername") && sharedPreferences.getString("keepLoggedIn", "false").equals("false")) {
                usernameText.setText(sharedPreferences.getString("lastUsername", null));
            } else if (sharedPreferences.contains("lastUsername") && sharedPreferences.contains("lastPassword") && sharedPreferences.getString("keepLoggedIn", "false").equals("true") && bundle.getString("fromLogOut", "false").equals("false")) {
                usernameText.setText(sharedPreferences.getString("lastUsername", null));
                passwordText.setText(sharedPreferences.getString("lastPassword", null));
                loginBtn.callOnClick();
                loginBtn.setClickable(false);
            }else if(sharedPreferences.contains("lastUsername") && sharedPreferences.contains("lastPassword") && sharedPreferences.getString("keepLoggedIn", "false").equals("true") && bundle.getString("fromLogOut", "false").equals("true")){
                usernameText.setText(sharedPreferences.getString("lastUsername", null));
                passwordText.setText(sharedPreferences.getString("lastPassword", null));
            }
        }else{
            if(sharedPreferences.contains("lastUsername") && sharedPreferences.contains("lastPassword") && sharedPreferences.getString("keepLoggedIn", "false").equals("true")){
                usernameText.setText(sharedPreferences.getString("lastUsername", null));
                passwordText.setText(sharedPreferences.getString("lastPassword", null));
                loginBtn.callOnClick();
                loginBtn.setClickable(false);
            }else{
                usernameText.setText(sharedPreferences.getString("lastUsername", null));
            }
        }

    }
}
