/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/roxana/Desktop/3dupb/yaxim-master/src/org/yaxim/androidclient/IXMPPRosterCallback.aidl
 */
package org.yaxim.androidclient;
/*
	IPC interface for XMPPService to send broadcasts to UI
*/
public interface IXMPPRosterCallback extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.yaxim.androidclient.IXMPPRosterCallback
{
private static final java.lang.String DESCRIPTOR = "org.yaxim.androidclient.IXMPPRosterCallback";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.yaxim.androidclient.IXMPPRosterCallback interface,
 * generating a proxy if needed.
 */
public static org.yaxim.androidclient.IXMPPRosterCallback asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.yaxim.androidclient.IXMPPRosterCallback))) {
return ((org.yaxim.androidclient.IXMPPRosterCallback)iin);
}
return new org.yaxim.androidclient.IXMPPRosterCallback.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_connectionStateChanged:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.connectionStateChanged(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.yaxim.androidclient.IXMPPRosterCallback
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public void connectionStateChanged(int connectionstate) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(connectionstate);
mRemote.transact(Stub.TRANSACTION_connectionStateChanged, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_connectionStateChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
public void connectionStateChanged(int connectionstate) throws android.os.RemoteException;
}
