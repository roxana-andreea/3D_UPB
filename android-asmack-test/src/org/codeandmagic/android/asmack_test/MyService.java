package org.codeandmagic.android.asmack_test;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MyService extends Service {
	private static final String TAG = "MyService";
	private static final String DOMAIN = "192.168.1.87";
	private static final String USERNAME = "evelyne";
	private static final String PASSWORD = "password";

	private XMPPConnection mXmppConnection;

	@Override
	public IBinder onBind(final Intent intent) {
		return new LocalBinder<MyService>(this);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mXmppConnection = new XMPPConnection(DOMAIN);
		try {
			mXmppConnection.connect();
			mXmppConnection.login(USERNAME, PASSWORD);
		}
		catch (final XMPPException e) {
			Log.e(TAG, "Could not connect to Xmpp server.", e);
			return;
		}

		if (!mXmppConnection.isConnected()) {
			Log.e(TAG, "Could not connect to the Xmpp server.");
			return;
		}

		Log.i(TAG, "Yey! We're connected to the Xmpp server!");
		mXmppConnection.getChatManager().addChatListener(new ChatManagerListener() {

			@Override
			public void chatCreated(final Chat chat, final boolean createdLocally) {
				if (!createdLocally) {
					chat.addMessageListener(new MyMessageListener());
				}
			}
		});

	}

	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		return Service.START_NOT_STICKY;
	}

	@Override
	public boolean onUnbind(final Intent intent) {
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mXmppConnection.disconnect();
	}

	private class MyMessageListener implements MessageListener {

		@Override
		public void processMessage(final Chat chat, final Message message) {
			Log.i(TAG, "Xmpp message received: '" + message.getBody() + "' on thread: " + getThreadSignature());
			// --> this is another thread ('Smack Listener Processor') not the
			// main thread!
			// you can parse the content of the message here
			// if you need to download something from the Internet, spawn a new
			// thread here and then sync with the main thread (via a
			// Handler)
		}
	}

	public static String getThreadSignature() {
		final Thread t = Thread.currentThread();
		return new StringBuilder(t.getName()).append("[id=").append(t.getId()).append(", priority=")
				.append(t.getPriority()).append("]").toString();
	}

}
