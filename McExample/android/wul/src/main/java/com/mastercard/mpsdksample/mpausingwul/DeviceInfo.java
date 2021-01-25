/*
 *  Copyright (c) 2019, MasterCard International Incorporated and/or its
 *  affiliates. All rights reserved.
 *
 *  The contents of this file may only be used subject to the MasterCard
 *  Mobile Payment SDK for MCBP and/or MasterCard Mobile MPP UI SDK
 *  Materials License.
 *
 *  Please refer to the file LICENSE.TXT for full details.
 *
 *  TO THE EXTENT PERMITTED BY LAW, THE SOFTWARE IS PROVIDED "AS IS", WITHOUT
 *  WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NON INFRINGEMENT. TO THE EXTENT PERMITTED BY LAW, IN NO EVENT SHALL
 *  MASTERCARD OR ITS AFFILIATES BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 *  IN THE SOFTWARE.
 */

package com.mastercard.mpsdksample.mpausingwul;


import android.annotation.SuppressLint;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Build;
import android.provider.Settings;

import com.mastercard.mpsdk.utils.Utils;
import com.mastercard.mpsdk.walletusabilitylayer.log.WLog;
import com.mastercard.mpsdkwulcesplugin.Model;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import flexjson.JSON;

/**
 * @exclude
 */
public final class DeviceInfo extends Model<DeviceInfo> {
	@JSON (name = "deviceName")
	private String deviceName;
	@JSON (name = "serialNumber")
	private String serialNumber;
	@JSON (name = "deviceType")
	private String deviceType;
	@JSON (name = "osName")
	private String osName;
	@JSON (name = "imei")
	private String imei;
	@JSON (name = "msisdn")
	private String msisdn;
	@JSON (name = "nfcCapable")
	private String nfcCapable;


	public DeviceInfo() {
		super();
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getOsName() {
		return osName;
	}

	public void setOsName(String osName) {
		this.osName = osName;
	}

	public String getImei() {
		return imei;
	}

	public void setImei(String imei) {
		this.imei = imei;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	public boolean isNfcCapable() {
		return Boolean.valueOf(nfcCapable);
	}

	public void setNfcCapable(boolean nfcCapable) {
		this.nfcCapable = Boolean.toString(nfcCapable);
	}

	public static DeviceInfo getDeviceInfo(final Context con) {

		// get device info json
		try {
			@SuppressLint("HardwareIds")
			 final String deviceId = Settings.Secure.getString(con.getContentResolver(), Settings
					.Secure.ANDROID_ID);
			final NfcManager manager = (NfcManager) con.getSystemService(Context.NFC_SERVICE);
			final NfcAdapter adapter = manager != null ? manager.getDefaultAdapter() : null;

			DeviceInfo info = new DeviceInfo();
			info.setDeviceName(Build.MODEL);
			info.setSerialNumber(Build.FINGERPRINT);
			info.setDeviceType(Build.MANUFACTURER);
			info.setOsName("ANDROID");
			info.setImei(deviceId);
			info.setMsisdn(Build.DEVICE);
			info.setNfcCapable(adapter != null);
			return info;
		} catch (Exception e) {
			WLog.e(DeviceInfo.class, "Failed to collect DeviceInfo", e);
		}
		return null;
	}

	public String getDeviceFingerprint() {

		final byte[] dataBytes = (this.deviceName +
				this.deviceType +
				this.imei +
				this.msisdn +
				this.nfcCapable +
				this.osName).getBytes();

		final ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[dataBytes.length]);
		byteBuffer.put(dataBytes);

		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			WLog.e(DeviceInfo.class, "getDeviceFingerprint failed", e);
			return null;
		}

		// Hash the result
		byte[] hash = messageDigest.digest(byteBuffer.array());

		// Return Hex
		return Utils.fromByteArrayToHexString(hash);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
