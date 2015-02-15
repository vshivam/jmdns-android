package com.heeere.android.dnssdtuto;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

public class DnssdDiscovery extends Activity {

	android.net.wifi.WifiManager.MulticastLock lock;
	android.os.Handler handler = new android.os.Handler();
	private String LOGTAG = getClass().getSimpleName();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}

	@Override
	protected void onStart() {
		super.onStart();
		setUp();
	}

	/** Called when the activity is first created. */

	private String type = "_test._tcp.local.";
	private JmDNS jmdns = null;
	private ServiceListener listener = null;
	private ServiceInfo serviceInfo;

	private void setUp() {

		new Thread(new Runnable() {

			@Override
			public void run() {
				android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
				lock = wifi.createMulticastLock(getClass().getSimpleName());
				lock.setReferenceCounted(false);
				try {
					InetAddress addr = getLocalIpAddress();
					String hostname = addr.getHostName();
					lock.acquire();
					Log.d(LOGTAG, "Addr : " + addr);
					Log.d(LOGTAG, "Hostname : " + hostname);
					jmdns = JmDNS.create(addr, hostname);
					listener = new ServiceListener() {

						/*
						 * Note:This event is only the service added event. The
						 * service info associated with this event does not
						 * include resolution information.
						 */
						@Override
						public void serviceAdded(ServiceEvent event) {
							/*
							 * Request service information. The information
							 * about the service is requested and the
							 * ServiceListener.resolveService method is called
							 * as soon as it is available.
							 */
							jmdns.requestServiceInfo(event.getType(),
									event.getName(), 1000);
						}

						/*
						 * A service has been resolved. Its details are now
						 * available in the ServiceInfo record.
						 */
						@Override
						public void serviceResolved(ServiceEvent ev) {
							Log.d(LOGTAG, "Service resolved: "
									+ ev.getInfo().getQualifiedName()
									+ " port:" + ev.getInfo().getPort());
							Log.d(LOGTAG, "Service Type : "
									+ ev.getInfo().getType());
						}

						@Override
						public void serviceRemoved(ServiceEvent ev) {
							Log.d(LOGTAG, "Service removed: " + ev.getName());
						}

					};
					jmdns.addServiceListener("_dynamix._tcp.local.", listener);

					/**
					 * Advertising a JmDNS Service Construct a service
					 * description for registering with JmDNS. 
					 * Parameters: 
					 * type : fully qualified service type name, such as _dynamix._tcp.local
					 * name : unqualified service instance name, such as DynamixInstance 
					 * port : the local port on which the service runs text string describing the service
					 * text : text describing the service
					 */
					serviceInfo = ServiceInfo.create("_dynamix._tcp.local.",
							"DynamixInstance", 7433,
							"Service Advertisement for Ambient Dynamix");
					
					/*A Key value map that can be advertised with the service*/
					serviceInfo.setText(getDeviceDetailsMap());
					jmdns.registerService(serviceInfo);
					Log.d(LOGTAG, "Service Type : " + serviceInfo.getType());
					Log.d(LOGTAG, "Service Registration thread complete");
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		}).start();

	}

	@Override
	protected void onStop() {
		//Unregister services
		if (jmdns != null) {
			if (listener != null) {
				jmdns.removeServiceListener(type, listener);
				listener = null;
			}
			jmdns.unregisterAllServices();
			try {
				jmdns.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			jmdns = null;
		}
		//Release the lock
		lock.release();
		super.onStop();
	}

	public InetAddress getLocalIpAddress() {
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		InetAddress address = null;
		try {
			address = InetAddress.getByName(String.format(Locale.ENGLISH,
					"%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
					(ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff)));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return address;
	}

	private Map<String, String> getDeviceDetailsMap() {
		Map<String, String> info = new HashMap<String, String>();
		info.put("device_name", "");
		return info;
	}
}