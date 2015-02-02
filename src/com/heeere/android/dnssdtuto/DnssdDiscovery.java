package com.heeere.android.dnssdtuto;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
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

		handler.postDelayed(new Runnable() {
			public void run() {
				setUp();
			}
		}, 1000);

	}

	/** Called when the activity is first created. */

	// private String type = "_test._tcp.local.";
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
					jmdns.addServiceListener("_dynamix._tcp.local.",
							new ServiceListener() {
								public void serviceResolved(ServiceEvent ev) {
									Log.d(LOGTAG, "Service resolved: "
											+ ev.getInfo().getQualifiedName()
											+ " port:" + ev.getInfo().getPort());
									Log.d(LOGTAG, "Service Type : "
											+ ev.getInfo().getType());
								}

								public void serviceRemoved(ServiceEvent ev) {
									Log.d(LOGTAG,
											"Service removed: " + ev.getName());
								}

								public void serviceAdded(ServiceEvent event) {
									// Required to force serviceResolved to be
									// called again
									// (after the first search)
									jmdns.requestServiceInfo(event.getType(),
											event.getName(), 1);
								}
							});
					serviceInfo = ServiceInfo.create("_dynamix._tcp.local.",
							"DynamixInstance", 7433,
							"Service Advertisement for Ambient Dynamix");
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
	protected void onStart() {
		super.onStart();
		// new Thread(){public void run() {setUp();}}.start();
	}

	@Override
	protected void onStop() {
		if (jmdns != null) {
			// if (listener != null) {
			// jmdns.removeServiceListener(type, listener);
			// listener = null;
			// }
			jmdns.unregisterAllServices();
			try {
				jmdns.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			jmdns = null;
		}
		lock.release();
		super.onStop();
	}

	public InetAddress getLocalIpAddress() {
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		InetAddress address = null;

		try {
			address = InetAddress.getByName(String.format("%d.%d.%d.%d",
					(ipAddress & 0xff), (ipAddress >> 8 & 0xff),
					(ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff)));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		return address;
	}

	private Map<String, String> getDeviceDetailsMap() {
		Map<String, String> infomap = new HashMap<String, String>();
		infomap.put("device_name", "device_name");
		return infomap;
	}
}