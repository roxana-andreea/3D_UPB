package org.codeandmagic.android.asmack_test;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	private boolean mBounded;
	private MyService mService;

	private final ServiceConnection mConnection = new ServiceConnection() {

		@SuppressWarnings("unchecked")
		@Override
		public void onServiceConnected(final ComponentName name, final IBinder service) {
			mService = ((LocalBinder<MyService>) service).getService();
			mBounded = true;
			Log.d(TAG, "onServiceConnected");
		}

		@Override
		public void onServiceDisconnected(final ComponentName name) {
			mService = null;
			mBounded = false;
			Log.d(TAG, "onServiceDisconnected");
		}
	};

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		doBindService();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
    }

	void doBindService() {
		bindService(new Intent(this, MyService.class), mConnection, Context.BIND_AUTO_CREATE);
	}

	void doUnbindService() {
		if (mBounded) {
			unbindService(mConnection);
		}
	}
}