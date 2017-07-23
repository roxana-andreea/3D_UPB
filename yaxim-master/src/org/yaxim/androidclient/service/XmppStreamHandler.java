package org.yaxim.androidclient.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.UnknownPacket;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;

import android.util.Log;

public class XmppStreamHandler {
	private static final String TAG = "yaxim.StreamHandler";
	private static final String URN_SM_2 = "urn:xmpp:sm:2";
	private static final int MAX_OUTGOING_QUEUE_SIZE = 200;
	private static final int REQUEST_ACK_AFTER_STANZAS = 10;
	private ExtXMPPConnection mConnection;
	private boolean isSmAvailable = false;
	private boolean isSmEnabled = false;
	private boolean isOutgoingSmEnabled = false;
	private boolean debugStanzas = false;
	private long previousIncomingStanzaCount = 0;
	private String sessionId;
	private long incomingStanzaCount = 0;
	private long outgoingStanzaCount = 0;
	private long outgoingStanzasSinceAckRequest = 0;
	private Queue<Packet> outgoingQueue;
	private int maxOutgoingQueueSize = MAX_OUTGOING_QUEUE_SIZE;

	protected final Collection<AckReceivedListener> ackListeners =
	    new CopyOnWriteArraySet<AckReceivedListener>();

	public XmppStreamHandler(XMPPConnection connection, boolean debug_stanzas) {
		mConnection = (ExtXMPPConnection)connection;
		debugStanzas = debug_stanzas;
		startListening();
	}

	/** Perform a quick shutdown of the XMPPConnection if a resume is possible */
	public void quickShutdown() {
		if (isResumePossible()) {
			mConnection.quickShutdown();
			// We will not necessarily get any notification from a quickShutdown, so adjust our state here.
			closeOnError();
		} else {
			// XXX: shutdown() would cause the connectionClosed listener to be called!
			mConnection.quickShutdown();
			close();
		}
	}

	public void setMaxOutgoingQueueSize(int maxOutgoingQueueSize) {
		this.maxOutgoingQueueSize = maxOutgoingQueueSize;
	}

	public boolean isResumePossible() {
		return sessionId != null;
	}

	public boolean isSmEnabled() {
		return isSmEnabled;
	}

	public static void addExtensionProviders() {
		addSimplePacketExtension("sm", URN_SM_2);
		addSimplePacketExtension("r", URN_SM_2);
		addSimplePacketExtension("a", URN_SM_2);
		addSimplePacketExtension("enabled", URN_SM_2);
		addSimplePacketExtension("resumed", URN_SM_2);
		addSimplePacketExtension("failed", URN_SM_2);
	}

	public void notifyInitialLogin() {
		Log.d(TAG, "notifyInitialLogin(): " + isSmAvailable);
		if (isSmAvailable) {
			sendEnablePacket();
		}
	}

	private void sendEnablePacket() {
		if (isOutgoingSmEnabled) {
			Log.d(TAG, "duplicate sendEnablePacket()");
			return;
		}
		if (sessionId != null) {
			isOutgoingSmEnabled = true;
			outgoingStanzasSinceAckRequest = 0;
			// TODO binding
			Log.d(TAG, "sendResume(): " + sessionId);
			StreamHandlingPacket resumePacket = new StreamHandlingPacket("resume", URN_SM_2);
			resumePacket.addAttribute("h", String.valueOf(previousIncomingStanzaCount));
			resumePacket.addAttribute("previd", sessionId);
			mConnection.sendPacket(resumePacket);
		} else {
			Log.d(TAG, "sendEnable()");
			outgoingStanzaCount = 0;
			outgoingStanzasSinceAckRequest = 0;
			outgoingQueue = new ConcurrentLinkedQueue<Packet>();
			isOutgoingSmEnabled = true;

			StreamHandlingPacket enablePacket = new StreamHandlingPacket("enable", URN_SM_2);
			enablePacket.addAttribute("resume", "true");
			mConnection.sendPacket(enablePacket);
		}
		synchronized(this) {
			try {
				this.wait(30000); // HACK: wait for SM answer to arrive
			}
			catch (InterruptedException e) {
				// Ignore.
			}
		}
	}

	private void closeOnError() {
		if (isSmEnabled && sessionId != null) {
			previousIncomingStanzaCount = incomingStanzaCount;
		}
		isSmEnabled = false;
		isOutgoingSmEnabled = false;
		isSmAvailable = false;
	}

	private void startListening() {
		mConnection.addConnectionListener(new ConnectionListener() {
			public void reconnectionSuccessful() {
				synchronized (XmppStreamHandler.this) {
					if (isResumePossible() && !isSmAvailable) {
						Log.d(TAG, "reconnected, waiting for SM packet to arrive...");
						try {
							XmppStreamHandler.this.wait(30000); // HACK: wait for SM packet to arrive
						} catch (InterruptedException e) {}
					}
				}
				Log.d(TAG, "reconnection: " + isSmAvailable);
				if (isSmAvailable) {
					sendEnablePacket();
				} else if (isResumePossible()) {
					// only tear down re-connect if we intended to resume
					close();
					mConnection.disconnect();
				}
			}

			public void reconnectionFailed(Exception e) {
			}

			public void reconnectingIn(int seconds) {
			}

			public void connectionClosedOnError(Exception e) {
				Log.d(TAG, "connectionClosedOnError: " + e.getLocalizedMessage());
				if (e instanceof XMPPException &&
					((XMPPException)e).getStreamError() != null) {
					// Non-resumable stream error
					close();
				} else {
					// Resumable
					closeOnError();
				}
			}

			public void connectionClosed() {
				Log.d(TAG, "connectionClosed.");
				previousIncomingStanzaCount = -1;
				close();
			}
		});

		mConnection.addPacketSendingListener(new PacketListener() {
			public void processPacket(Packet packet) {
				// Ignore our own request for acks - they are not counted
				if (!isStanza(packet)) {
					if (debugStanzas) Log.d(TAG, "send non-stanza " + packet.toXML());
					return;
				}

				if (isOutgoingSmEnabled && !outgoingQueue.contains(packet)) {
					outgoingStanzaCount++;
					outgoingQueue.add(packet);

					if (debugStanzas) Log.d(TAG, "adding " + outgoingStanzaCount + " : " + packet.toXML());

					outgoingStanzasSinceAckRequest++;
					// Don't let the queue grow beyond max size.  Request acks and drop old packets
					// if acks are not coming.
					if (outgoingStanzasSinceAckRequest >= REQUEST_ACK_AFTER_STANZAS) {
						requestAck();
					}

					if (outgoingQueue.size() > maxOutgoingQueueSize) {
						Log.e(TAG, "not receiving acks?  outgoing queue full");
						outgoingQueue.remove();
					}
				} else {
					if (debugStanzas) Log.d(TAG, "sending " + packet.toXML());
				}
			}
		}, new PacketFilter() {
			public boolean accept(Packet packet) {
				return true;
			}
		});

		mConnection.addPacketListener(new PacketListener() {
			public void processPacket(Packet packet) {
				if (isSmEnabled && isStanza(packet)) {
					incomingStanzaCount++;
					if (debugStanzas) Log.d(TAG, "recv " + incomingStanzaCount + " : " + packet.toXML());
				} else {
					if (debugStanzas) Log.d(TAG, "recv " + packet.toXML());
				}

				if (packet instanceof StreamHandlingPacket) {
					StreamHandlingPacket shPacket = (StreamHandlingPacket) packet;
					String name = shPacket.getElementName();
					if ("sm".equals(name)) {
						Log.d(TAG, "SM available!");
						synchronized(XmppStreamHandler.this) {
							isSmAvailable = true;
							XmppStreamHandler.this.notify();
						}
					} else if ("r".equals(name)) {
						StreamHandlingPacket ackPacket = new StreamHandlingPacket("a", URN_SM_2);
						ackPacket.addAttribute("h", String.valueOf(incomingStanzaCount));
						mConnection.sendPacket(ackPacket);
					} else if ("a".equals(name)) {
						long ackCount = Long.valueOf(shPacket.getAttribute("h"));
						removeOutgoingAcked(ackCount);
						Log.d(TAG, outgoingQueue.size() + " in outgoing queue after ack");
					} else if ("enabled".equals(name)) {
						Log.d(TAG, "SM enabled: " + shPacket.getAttribute("id"));
						incomingStanzaCount = 0;
						isSmEnabled = true;
						mConnection.getRoster().setOfflineOnError(false);
						String resume = shPacket.getAttribute("resume");
						if ("true".equals(resume) || "1".equals(resume)) {
							sessionId = shPacket.getAttribute("id");
						}
						synchronized(XmppStreamHandler.this) { XmppStreamHandler.this.notify(); }
					} else if ("resumed".equals(name)) {
						Log.d(TAG, "SM resumed: " + sessionId);
						incomingStanzaCount = previousIncomingStanzaCount;
						long resumeStanzaCount = Long.valueOf(shPacket.getAttribute("h"));
						// Removed acked packets
						removeOutgoingAcked(resumeStanzaCount);
						Log.d(TAG, outgoingQueue.size() + " in outgoing queue after resume");

						// Resend any unacked packets
						for (Packet resendPacket : outgoingQueue) {
							mConnection.sendPacket(resendPacket);
						}

						// Enable only after resend, so that the interceptor does not
						// queue these again or increment outgoingStanzaCount.
						isSmEnabled = true;
						synchronized(XmppStreamHandler.this) { XmppStreamHandler.this.notify(); }
					} else if ("failed".equals(name)) {
						// Failed, shutdown and the parent will retry
						Log.d(TAG, "SM failed! :(");
						mConnection.getRoster().setOfflineOnError(true);
						mConnection.getRoster().setOfflinePresences();
						sessionId = null;
						// normally, we would <bind> the connection here, but
						// this is not supported in XMPPConnection's workflow.
						mConnection.causeException(new Exception("XEP-0198 stream resumption failed"));
						// isSmEnabled / isOutgoingSmEnabled are already false
						synchronized(XmppStreamHandler.this) { XmppStreamHandler.this.notify(); }
					}
				}
			}
		}, new PacketFilter() {
			public boolean accept(Packet packet) {
				return true;
			}
		});
	}


	private void removeOutgoingAcked(long ackCount) {
		if (ackCount > outgoingStanzaCount) {
			Log.e(TAG,
					"got ack of " + ackCount + " but only sent " + outgoingStanzaCount);
			// Reset the outgoing count here in a feeble attempt to re-sync.  All bets
			// are off.
			outgoingStanzaCount = ackCount;
		}

		int size = outgoingQueue.size();
		while (size > outgoingStanzaCount - ackCount) {
			outgoingQueue.remove();
			size--;
		}

		for (AckReceivedListener l : ackListeners) {
		    l.ackReceived(ackCount, outgoingStanzaCount);
		}
	}

	public long requestAck() {
		outgoingStanzasSinceAckRequest = 0;
		mConnection.sendPacket(new StreamHandlingPacket("r", URN_SM_2));
		return outgoingStanzaCount;
	}

	public void addAckReceivedListener(AckReceivedListener l) {
	    ackListeners.add(l);
	}

	public void removeAckReceivedListener(AckReceivedListener l) {
	    ackListeners.remove(l);
	}

	private static void addSimplePacketExtension(final String name, final String namespace) {
		ProviderManager.getInstance().addExtensionProvider(name, namespace,
				new PacketExtensionProvider() {
					public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
						StreamHandlingPacket packet = new StreamHandlingPacket(name, namespace);
						int attributeCount = parser.getAttributeCount();
						for (int i = 0; i < attributeCount; i++) {
							packet.addAttribute(parser.getAttributeName(i),
								parser.getAttributeValue(i));
						}
						return packet;
					}
				});
	}

	static class StreamHandlingPacket extends UnknownPacket {
		private String name;
		private String namespace;
		Map<String, String> attributes;

		StreamHandlingPacket(String name, String namespace) {
			this.name = name;
			this.namespace = namespace;
			attributes = Collections.emptyMap();
		}

		public void addAttribute(String name, String value) {
			if (attributes == Collections.EMPTY_MAP)
				attributes = new HashMap<String, String>();
			attributes.put(name, value);
		}

		public String getAttribute(String name) {
			return attributes.get(name);
		}

		public String getNamespace() {
			return namespace;
		}

		public String getElementName() {
			return name;
		}

		public String toXML() {
			StringBuilder buf = new StringBuilder();
			buf.append("<").append(getElementName());

			// TODO Xmlns??
			if (getNamespace() != null) {
				buf.append(" xmlns=\"").append(getNamespace()).append("\"");
			}
			for (String key : attributes.keySet()) {
				buf.append(" ").append(key).append("=\"")
					.append(StringUtils.escapeForXML(attributes.get(key))).append("\"");
			}
			buf.append("/>");
			return buf.toString();
		}

	}

	/** Returns true if the packet is a Stanza as defined in RFC-6121 - a Message, IQ or Presence packet. */
	public static boolean isStanza(Packet packet) {
		if (packet instanceof Message)
			return true;
		if (packet instanceof IQ)
			return true;
		if (packet instanceof Presence)
			return true;
		return false;
	}

	public void queue(Packet packet) {
		if (outgoingQueue.size() >= maxOutgoingQueueSize) {
			Log.e(TAG, "outgoing queue full");
			return;
		}
		outgoingStanzaCount++;
		outgoingQueue.add(packet);
	}

	protected void close() {
		isSmEnabled = false;
		isOutgoingSmEnabled = false;
		sessionId = null;
	}

    /** Interface to allow application-level monitoring of the send queue.
     */
    public static interface AckReceivedListener {
	/** Called on a received <resumed>/<a> packet with an h element.
	 *
	 * @param handled number of stanzas handled by the other side
	 * @param total number of stanzas sent to other side
	 */
	public void ackReceived(long handled, long total);
    }

    public static class ExtXMPPConnection extends XMPPConnection {

        public ExtXMPPConnection(org.jivesoftware.smack.ConnectionConfiguration config) {
            super(config);
        }

        public void shutdown() {

            try {
                // Be forceful in shutting down since SSL can get stuck
                //try {
                //    socket.shutdownInput();
                //} catch (Exception e) {
                //}
                //socket.close();
                shutdown(new org.jivesoftware.smack.packet.Presence(
                        org.jivesoftware.smack.packet.Presence.Type.unavailable));

            } catch (Exception e) {
                Log.e(TAG, "error on shutdown()", e);
            }
        }
	public void causeException(Exception e) {
	    // notifyConnectionError(e);
	    try {
		java.lang.reflect.Method nce = XMPPConnection.class.getDeclaredMethod("notifyConnectionError",
			new Class[] { Exception.class });
		nce.setAccessible(true);
		nce.invoke(this, new Object[] { e });
	    } catch (Exception fail) {
		fail.printStackTrace();
	    }
	}
    }

}
