package justintime.com.productscale;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.List;

import io.particle.android.sdk.cloud.ParticleDevice;

/**
 * Created by Justin on 2017-08-22.
 */

public class ParticleDeviceArrayAdapter extends BaseAdapter implements ListAdapter {

    private List<ParticleDevice> devices;
    private Context context;
    private int layoutID;


    public ParticleDeviceArrayAdapter(Context context, List<ParticleDevice> devices, int layoutID){
        this.devices = devices;
        this.context = context;
        this.layoutID = layoutID;
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public ParticleDevice getItem(int position) {
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if(view == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(layoutID, null);
        }

        TextView textView = (TextView) view.findViewById(R.id.list_item_string);
        textView.setText(devices.get(position).getName());
        TextView deviceTypeText = (TextView) view.findViewById(R.id.device_type_string);
        deviceTypeText.setText(devices.get(position).getDeviceType().toString());

        ImageView imageView = (ImageView) view.findViewById(R.id.statusImage);
        if(devices.get(position).isConnected()){
            imageView.setImageResource(R.drawable.online);
        }else{
            imageView.setImageResource(R.drawable.offline);
        }

        return view;
    }
}
