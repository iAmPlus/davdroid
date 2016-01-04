package at.bitfire.davdroid.ui.setup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DeviceSetupReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent addAccount) {
		Intent serviceIntent = new Intent(addAccount);
		serviceIntent.setClassName(context, AddAccountService.class.getName());
		context.startService(serviceIntent);
	}

}
