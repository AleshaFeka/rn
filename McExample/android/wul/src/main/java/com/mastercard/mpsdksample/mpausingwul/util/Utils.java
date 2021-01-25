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

package com.mastercard.mpsdksample.mpausingwul.util;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.appcompat.app.AlertDialog;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.mastercard.mpsdk.walletusabilitylayer.api.Wul;
import com.mastercard.mpsdk.walletusabilitylayer.config.UserAuthMode;
import com.mastercard.mpsdk.walletusabilitylayer.config.WalletConfiguration;
import com.mastercard.mpsdk.walletusabilitylayer.log.WLog;
import com.mastercard.mpsdksample.mpausingwul.BuildConfig;
import com.mastercard.mpsdksample.mpausingwul.Constants;
import com.mastercard.mpsdksample.mpausingwul.MpaApplication;
import com.mastercard.mpsdksample.mpausingwul.R;
import com.mastercard.mpsdksample.mpausingwul.receiver.CardManagerEventReceiver;
import com.mastercard.mpsdksample.mpausingwul.receiver.PaymentReceiver;
import com.mastercard.mpsdksample.mpausingwul.receiver.WalletApplicationFcmReceiver;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Utils {

    public static final SimpleDateFormat DATE_TS_SHORT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public static void popToast(final Context con, final String message) {
        Toast.makeText(con, message, Toast.LENGTH_SHORT).show();
    }

    public static void showErrorMessage(final Context con,
                                        final View rootView,
                                        final String message) {

        final Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);

        View snackbarView = snackbar.getView();
        TextView textView =
                (TextView) snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        snackbarView.setBackgroundColor(ContextCompat.getColor(con, R.color.mastercard_red));
        textView.setMaxLines(5);
        snackbar.setAction("Dismiss", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }

    public static void showErrorMessage(final Context con,
            final View rootView,
            final String message,
            final View.OnClickListener onClickListener) {

        final Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_INDEFINITE);

        View snackbarView = snackbar.getView();
        TextView textView =
                (TextView) snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        snackbarView.setBackgroundColor(ContextCompat.getColor(con, R.color.mastercard_red));
        textView.setMaxLines(5);
        snackbar.setAction("Dismiss", onClickListener);
        snackbar.show();
    }

    public static void showSuccessMessage(final Context con,
                                          final View rootView,
                                          final String message) {

        final Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        TextView textView =
                (TextView) snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        snackbarView.setBackgroundColor(ContextCompat.getColor(con, R.color.colorAccent));
        textView.setMaxLines(5);
        snackbar.setAction("Dismiss", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }


    public static AlertDialog.Builder getModalDialog(final Context con,
                                                     final String message) {
        AlertDialog.Builder d = new AlertDialog.Builder(con);
        d.setCancelable(false);
        d.setMessage(message).setCancelable(true).setTitle(null);
        d.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        return d;
    }

    public static AlertDialog getErrorDialog(final Context con,
                                             final String title,
                                             final String message) {
        AlertDialog.Builder d = new AlertDialog.Builder(con);
        d.setMessage(message).setCancelable(true).setTitle(title);
        d.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        return d.create();
    }

    public static void showNotification(final Context con,
                                        final String title,
                                        final String message) {

        NotificationManager notificationManager =
                (NotificationManager) con.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder =null;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            String channelId = con.getString(R.string.notification_channel_id);
            String channelDescription = con.getString(R.string.notification_channel_description);

            NotificationChannel notificationChannel =
                    notificationManager.getNotificationChannel(channelId);

            if (notificationChannel == null) {
                //Set the importance level
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                notificationChannel = new NotificationChannel(channelId,
                                                              channelDescription,
                                                              importance);
                // create notification channel
                notificationManager.createNotificationChannel(notificationChannel);
            }
            builder = new Notification.Builder(con, notificationChannel.getId());
        } else {
            builder = new Notification.Builder(con);
        }

        builder.setSmallIcon(R.drawable.ic_account_balance_wallet)
               .setLargeIcon(BitmapFactory.decodeResource(con.getResources(),
                                                          R.drawable.ic_account_balance_wallet))
               .setContentTitle(title)
               .setContentText(message);

        notificationManager.notify(new Random().nextInt(), builder.build());

    }

    public static void showKeyboard(final Context con, final View view) {
        InputMethodManager i = (InputMethodManager) con.getSystemService(Context
                                                                                 .INPUT_METHOD_SERVICE);
        i.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    public static void hideKeyboard(final Context con, final View view) {
        InputMethodManager i = (InputMethodManager) con.getSystemService(Context
                                                                                 .INPUT_METHOD_SERVICE);
        i.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void showAboutDialog(final Context con) {

        final WalletConfiguration config = Wul.getWalletConfiguration();

        // create view
        View v = LayoutInflater.from(con).inflate(R.layout.view_about, null);
        TextView versionView = (TextView) v.findViewById(R.id.about_version);
        TextView flavorView = (TextView) v.findViewById(R.id.about_flavor);
        TextView authModeView = (TextView) v.findViewById(R.id.about_auth_mode);
        TextView cdCvmView = (TextView) v.findViewById(R.id.about_cdcvm);
        TextView validationView = (TextView) v.findViewById(R.id.about_validation);
        TextView instanceView = (TextView) v.findViewById(R.id.about_instance_id);
        TextView emailView = (TextView) v.findViewById(R.id.about_email);

        // version
        String version = "Version " +
                         BuildConfig.APP_VERSION +
                         " (" +
                         getStringFromDate(new Date(BuildConfig.TIMESTAMP)) +
                         ")";
        versionView.setText(version);

        // flavor
        flavorView.setText(con.getString(R.string.about_flavor, BuildConfig.FLAVOR));

        // auth mode
        authModeView.setText(con.getString(R.string.about_auth_mode, config.getUserAuthMode().name
                ()));

        // cd cvm
        cdCvmView.setText(con.getString(R.string.about_cdcvm, config.getCvmModel().name()));

        // validation
        final String validation = (config.getUserAuthMode() == UserAuthMode.WALLET_PIN || config
                .getUserAuthMode() == UserAuthMode.CARD_PIN ? "Mobile PIN" : "Locally verified");
        validationView.setText(con.getString(R.string.about_validation, validation));

        // instance id
        String instanceId = Wul.getActivationManager().getPaymentAppInstanceId();
        if (instanceId != null) {
            instanceView.setText(con.getString(R.string.about_instance_id, new String
                    (instanceId)));
        } else {
            instanceView.setText(con.getString(R.string.about_instance_id, "n/a"));
        }

        // email
        emailView.setMovementMethod(LinkMovementMethod.getInstance());
        String addr = con.getString(R.string.about_email);
        emailView.setText(Html.fromHtml("<a href=\"mailto:" + addr + "\">" + addr + "</a>"));

        // dialog
        AlertDialog.Builder d = new AlertDialog.Builder(con);
        d.setCancelable(true).setTitle(null);
        d.setView(v);
        d.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        d.create().show();
    }

    public static String getStringFromDate(Date d) {
        return getStringFromDate(d, DATE_TS_SHORT);
    }

    public static String getStringFromDate(Date d, SimpleDateFormat f) {
        if (d != null) {
            try {
                return f.format(d);
            } catch (Exception e) {
            }
        }
        return null;
    }

    public static void startGlobalReceivers(final Context con) {
        WLog.d("Utils", "**** startGlobalReceivers");

        try {
            // events that should be triggered even if the app
            // is not running or is not in the foreground

            // contactless payment: launch TransactionActivity
            PaymentReceiver paymentReceiver = new PaymentReceiver();
            paymentReceiver.register(con);

            paymentReceiver.getOutcomeEventReceiver().register(con);


            // card event: show notification
            CardManagerEventReceiver cardReceiver = new CardManagerEventReceiver();
            cardReceiver.register(con);

            //fcm event receiver
            WalletApplicationFcmReceiver walletGcmReceiver = new WalletApplicationFcmReceiver();
            walletGcmReceiver.register(con);

        } catch (Exception e) {
            WLog.e("Utils", "Failed to register global receivers", e);
        }
    }

    public static void resetAppAndRelaunch(final Activity act) {
        WLog.d("Utils", "**** resetAppAndRelaunch");

        // reset PAS/Issuer URLs
        setPasUrl(act, null);
        //setIssuerIp(act, null);
        //setIssuerPort(act, 0);

        // reset WUL
        Wul.reset();

        // re-initialize WUL
        ((MpaApplication)Wul.getApplication()).init();

        // restart app
        Intent i = act.getPackageManager().getLaunchIntentForPackage(act.getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        act.startActivity(i);
    }

    public static void setPasUrl(final Context context, final String url) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_DB, MODE_PRIVATE);
        prefs.edit().putString(Constants.PREFS_PAS_URL, url).apply();
    }

    public static String getPasUrl(final Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_DB, MODE_PRIVATE);
        return prefs.getString(Constants.PREFS_PAS_URL, BuildConfig.PAS_URL);
    }

    public static boolean hasFingerprintPermission(final Context con) {
        try {
            if (ActivityCompat.checkSelfPermission(con, Manifest.permission.USE_FINGERPRINT) !=
                PackageManager.PERMISSION_GRANTED) {
                WLog.d("Utils", "Fingerprint authentication permission: not enabled");
                return false;
            }
            WLog.d("Utils", "Fingerprint authentication permission: enabled");
            return true;
        } catch (Exception e) {
            WLog.e("Utils", "Failed to determine fingerprint permission", e);
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    public static boolean hasFingerprintHardware(final Context con) {
        try {
            FingerprintManagerCompat fm = FingerprintManagerCompat.from(con);
            return fm.isHardwareDetected();
        } catch (NullPointerException | SecurityException e) {
            WLog.e("Utils", "Failed to determine fingerprint hardware", e);
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    public static boolean hasFingerprintsEnrolledOnDevice(final Context con) {
        try {
            FingerprintManagerCompat fm = FingerprintManagerCompat.from(con);
            return fm.hasEnrolledFingerprints();
        } catch (NullPointerException | SecurityException e) {
            WLog.e("Utils", "Failed to determine fingerprint hardware", e);
        }
        return false;
    }

    public static boolean isScreenLocked(final Context context) {
        try {
            final KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService
                    (Context.KEYGUARD_SERVICE);
            return keyguardManager.inKeyguardRestrictedInputMode();
        } catch (Exception e) {
            WLog.e("Utils", "Failed to determine lockscreen state", e);
        }
        return false;
    }

    public static boolean isLockscreenEnabled(final Context con) {
        try {
            KeyguardManager km = (KeyguardManager) con.getSystemService(Context.KEYGUARD_SERVICE);
            return km.isKeyguardSecure();
        } catch (Exception e) {
            WLog.e("Utils", "Failed to determine lockscreen secure state", e);
        }
        return false;
    }

    public static boolean isNetworkConnected(final Context con) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) con.getSystemService
                    (Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mobile = connectivityManager.getNetworkInfo(ConnectivityManager
                                                                            .TYPE_MOBILE);
            boolean isConnected = (wifi != null && wifi.isAvailable() && wifi.isConnected()) ||
                                  (mobile != null && mobile.isAvailable() && mobile.isConnected());
            WLog.d("Utils", "isConnected=" + isConnected);
            return isConnected;
        } catch (Exception e) {
        }
        return false;
    }

    public static String randomizePan() {
        String testPanBin = "54133300";
        int panLength = 16;
        // let's build the rest of the PAN
        StringBuilder randomPan = new StringBuilder(testPanBin);
        Random random = new Random();
        // Build the the PAN from given bin range. We need to generate up to the last digit (which
        // will be the Luhn Check Digit)
        for (int i = 0; i < panLength - testPanBin.length() - 1; i++) {
            int digit = Math.abs(random.nextInt()) % 10;
            randomPan.append(digit);
        }
        LuhnCheckDigit luhnValidator = new LuhnCheckDigit();
        try {
            randomPan.append(luhnValidator.calculate(randomPan.toString()));
        } catch (CheckDigitException e) {
            throw new RuntimeException("Unable to calculate the Luhn Check for the given PAN");
        }
        return randomPan.toString();
    }

    public static boolean isValidPan(String pan) {
        return new LuhnCheckDigit().isValid(pan);
    }

    public static void dumpIntent(Intent i) {
        Bundle b = i.getExtras();
        if (b != null) {
            for (String key : b.keySet()) {
                Object value = b.get(key);
                if (value != null) {
                    WLog.d("Utils", String.format("%s=%s [%s]", key, value.toString(), value
                            .getClass().getName()));
                }
            }
        }
    }

    /**
     * API to encrypt the data using the custom algorithm shared with the sample Crypto module
     *
     * @param data data to encrypt (e.g. Mobile Pin , DeviceFingerprint, etc.)
     * @param bKey Key to be used for encryption
     * @return encrypted data
     */
    public static byte[] encryptData(final byte[] data, final byte[] bKey) {
        final SecretKey secretKey = new SecretKeySpec(bKey, "AES");
        try {
            final Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            final byte[] result;
            // Encrypt the data
            byte[] dataWithPadding = addIso7816Padding(data, 16);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(dataWithPadding);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static byte[] addIso7816Padding(final byte[] input, final int blockSize) {

        final int paddedLength = input.length + blockSize - (input.length % blockSize);
        final byte[] dataWithPadding = new byte[paddedLength];
        System.arraycopy(input, 0, dataWithPadding, 0, input.length);
        dataWithPadding[input.length] = (byte) 0x80;
        return dataWithPadding;
    }

    public static String getFcmToken(Context applicationContext) {
        SharedPreferences sharedPref =
                applicationContext.getSharedPreferences("Wallet_Shared_Pref", MODE_PRIVATE);

        String fcmToken =
                sharedPref.getString(
                        applicationContext.getString(R.string.shared_pref_fcm_token_key),
                        null);
        return fcmToken;
    }

    public static void setFcmToken(String fcmToken, Context applicationContext) {
        SharedPreferences sharedPref =
                applicationContext.getSharedPreferences("Wallet_Shared_Pref", MODE_PRIVATE);
        Editor editor = sharedPref.edit();
        editor.putString(applicationContext.getString(R.string.shared_pref_fcm_token_key),
                         fcmToken);
        editor.commit();
    }
}
