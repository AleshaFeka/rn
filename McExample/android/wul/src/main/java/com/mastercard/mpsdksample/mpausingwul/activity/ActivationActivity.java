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

package com.mastercard.mpsdksample.mpausingwul.activity;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mastercard.mpsdk.componentinterface.CardholderValidator;
import com.mastercard.mpsdk.componentinterface.crypto.keys.CustomEncryptedData;
import com.mastercard.mpsdk.componentinterface.crypto.keys.WalletDekEncryptedData;
import com.mastercard.mpsdk.utils.bytes.ByteArray;
import com.mastercard.mpsdk.walletusabilitylayer.api.Wul;
import com.mastercard.mpsdk.walletusabilitylayer.api.WulActivationManager;
import com.mastercard.mpsdk.walletusabilitylayer.config.RegistrationRequestData;
import com.mastercard.mpsdk.walletusabilitylayer.config.UserAuthMode;
import com.mastercard.mpsdk.walletusabilitylayer.config.WalletRegistrationDataProvider;
import com.mastercard.mpsdk.walletusabilitylayer.config.WalletRegistrationRequestDataProvider;
import com.mastercard.mpsdk.walletusabilitylayer.log.WLog;
import com.mastercard.mpsdk.walletusabilitylayer.manager.WulDataManager;
import com.mastercard.mpsdk.walletusabilitylayer.util.WalletUtils;
import com.mastercard.mpsdksample.mpausingwul.BuildConfig;
import com.mastercard.mpsdksample.mpausingwul.DeviceInfo;
import com.mastercard.mpsdksample.mpausingwul.R;
import com.mastercard.mpsdksample.mpausingwul.util.Utils;
import com.mastercard.mpsdkwulcesplugin.CesPaymentAppServer;
import com.mastercard.mpsdkwulcesplugin.RegistrationRequest;
import com.mastercard.mpsdkwulcesplugin.SignUpRequest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;


public class ActivationActivity extends BaseActivity implements
                                                     View.OnClickListener,
                                                     CesPaymentAppServer.SignUpCallback,
                                                     CesPaymentAppServer.RegistrationCallback,
                                                     WulActivationManager.ActivationCallback {

    private EditText mUserIdView;
    private EditText mActivationCodeView;
    private EditText mActivationPinView;
    private CheckBox mPasCheckboxView;
    private ViewGroup mUrlLayout;
    private EditText mPasUrlView;
    private Button mActivationButton;

    private String mUserId;
    private String mActivationCode;
    private WalletDekEncryptedData mActivationEncryptedPin;
    private String mPasUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        WLog.d(this, "**** onCreate");

        setContentView(R.layout.activity_activation);
        super.onCreate(savedInstanceState);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowHomeEnabled(false);

        TextView versionView = (TextView) findViewById(R.id.version);
        mUserIdView = (EditText) findViewById(R.id.user_id);
        mActivationCodeView = (EditText) findViewById(R.id.activation_code);
        mActivationPinView = (EditText) findViewById(R.id.activation_pin);
        mPasCheckboxView = (CheckBox) findViewById(R.id.activation_pas_checkbox);
        mPasCheckboxView.setOnClickListener(this);
        mUrlLayout = (ViewGroup) findViewById(R.id.activation_urls_layout);
        mPasUrlView = (EditText) findViewById(R.id.activation_pas_url);
        mActivationButton = (Button) findViewById(R.id.activation_button);
        mActivationButton.setOnClickListener(this);

        if (Wul.getWalletConfiguration().getUserAuthMode() == UserAuthMode.WALLET_PIN) {
            findViewById(R.id.activation_pin_layout).setVisibility(View.VISIBLE);
        }

        mPasUrl = Utils.getPasUrl(this);
        mPasUrlView.setText(mPasUrl);

        versionView.setText(getString(R.string.version, BuildConfig.APP_VERSION));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activation_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.main_menu_reset) {
            Utils.resetAppAndRelaunch(this);
            return true;
        } else if (id == R.id.main_menu_about) {
            Utils.showAboutDialog(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {

        if (view.equals(mPasCheckboxView)) {

            if (mPasCheckboxView.isChecked()) {
                mUrlLayout.setVisibility(View.VISIBLE);
            } else {
                mUrlLayout.setVisibility(View.GONE);
            }

        } else if (view.equals(mActivationButton)) {

            mUserId = mUserIdView.getText().toString();
            mActivationCode = mActivationCodeView.getText().toString();

            if (mPasCheckboxView.isChecked()) {
                mPasUrl = mPasUrlView.getText().toString().trim();
            }

            if (mUserId.length() == 0 || mActivationCode.length() == 0) {
                Utils.showErrorMessage(this, getRootView(),
                                       getString(R.string.error_user_id_missing));
                return;
            }
            if (mPasUrl == null || mPasUrl.length() == 0) {
                Utils.showErrorMessage(this, getRootView(), getString(R.string.error_pas_url));
                return;
            }
            if (mActivationPinView.getText().length() >= 4 && mActivationPinView.getText().length
                    () <= 8) {
                CustomEncryptedData customEncryptedPin =
                        new CustomEncryptedData(Utils.encryptData(mActivationPinView.getText
                                ().toString().getBytes(), BuildConfig.SHARED_CRYPTO_KEY));
                mActivationEncryptedPin = WalletUtils.encryptWalletData(customEncryptedPin);
                mActivationPinView.setText("");
            }

            Utils.setPasUrl(this, mPasUrl);

            mActivationButton.setEnabled(false);
            Utils.hideKeyboard(this, mActivationButton);
            showProgressDialog();

            startSignUp();
        }
    }

    /**
     * 1. Sign-up with your service to get the Payment App Instance Id
     * and the Payment App Provider Id.
     */
    private void startSignUp() {
        /*
        We are using the CES Plug-in as our PAS. You will need to replace
		this action with your own PAS service.
		 */
        String fcmToken = Utils.getFcmToken(getApplicationContext());

        if(TextUtils.isEmpty(fcmToken)){

            // Either the Firebase Messaging Service is not auto initialized and onFcmTokenReceived()
            // callback is not raised or the FCM token is not stored on onFcmTokenReceived() callback


           // Explicitly fetch the FCM token and do SignUp
           new FetchFcmTokenAndDoSignUpTask(this).execute();
           return;

        }

        SignUpRequest signUpRequest =
                new SignUpRequest(mUserId,
                                  mActivationCode,
                                  fcmToken,
                                  DeviceInfo
                                          .getDeviceInfo(ActivationActivity.this).toJson(),
                                  "2.0",
                                  getCardHolderValidator(),
                                  WulDataManager.getInstance().getCvmModel().name());

        final CesPaymentAppServer ces =
                new CesPaymentAppServer(this, Utils.getPasUrl(this));
        ces.signUp(signUpRequest, ActivationActivity.this);
    }

    private String getCardHolderValidator() {

        WulDataManager wulDataManager = WulDataManager.getInstance();
        String cardHolderValidator = CardholderValidator.MOBILE_PIN.name();
        final UserAuthMode authMode = wulDataManager.getUserAuthMode();
        if (authMode == UserAuthMode.CUSTOM || authMode == UserAuthMode.NONE) {
            cardHolderValidator = CardholderValidator.LOCALLY_VERIFIED_CDCVM.name();
        }
        return cardHolderValidator;
    }

    /**
     * 2. Create the WalletPinRegistrationRequestProvider based on the
     * payment app ids. If your wallet type is WALLET_PIN you can also
     * supply a PIN here if the user has already entered one (optional).
     *
     * @param paymentAppInstanceId your Payment App Instance Id from CES
     * @param paymentAppProviderId your Payment App Provider Id from CES
     */
    @Override
    public void cesPasSignUpComplete(final String paymentAppInstanceId,
            final String paymentAppProviderId) {

        final WalletDekEncryptedData mEncryptedPin = mActivationEncryptedPin;
        // remove local pin immediately
        mActivationEncryptedPin = null;

        final WalletRegistrationRequestDataProvider registrationRequestDataProvider =
                new WalletRegistrationRequestDataProvider() {

                    @Override
                    public String getPaymentAppInstanceId() {
                        // If known, otherwise return null.
                        return paymentAppInstanceId;
                    }

                    @Override
                    public String getPaymentAppProviderId() {
                        return paymentAppProviderId;
                    }

                    @Override
                    public WalletDekEncryptedData getWalletDekEncryptedMobilePin() {
                        // you can supply a PIN here if it is known at this point (Wallet PIN
                        // types only)
                        // otherwise leave it as null. Do not hardcode it! It should come from
                        // the user.
                        return mEncryptedPin;
                    }
                };

        downloadCmsdCertificateAndGenerateRegistrationRequest(registrationRequestDataProvider);
    }

    @Override
    public void cesPasSignUpFailed(String message, Exception e) {
        showErrorMessage("Sign up failed: " + message);
    }

    /**
     * 3. Get a byte array of the Mastercard public certificate
     *
     * @param registrationRequestDataProvider the data from your sign-up process
     */
    private void downloadCmsdCertificateAndGenerateRegistrationRequest(
            final WalletRegistrationRequestDataProvider registrationRequestDataProvider) {

        CesPaymentAppServer ces = new CesPaymentAppServer(this, Utils.getPasUrl(this));

        ces.downloadCmsdCertificate(new CesPaymentAppServer.DownloadCmsdCertificateCallback() {
            @Override
            public void certificateDownloadComplete(final byte[] certificateBytes) {

                getRegistrationRequestData(certificateBytes, registrationRequestDataProvider);

            }

            @Override
            public void certificateDownloadFailed(final String s, final Exception e) {

                showErrorMessage(s + ", " + e.getMessage());

            }
        });
    }

    /**
     * 4. Get the RegistrationRequestData
     *
     * @param certificateBytes                bytes array of the public cert
     * @param registrationRequestDataProvider the data from your sign-up process
     */
    public void getRegistrationRequestData(final byte[] certificateBytes,
            final WalletRegistrationRequestDataProvider
                    registrationRequestDataProvider) {

        final WulActivationManager.RegistrationRequestDataCallback callback =
                new WulActivationManager.RegistrationRequestDataCallback() {
                    @Override
                    public void registrationRequestDataGenerated(
                            RegistrationRequestData registrationRequestData) {
                        startRegistrationWithCesPas(registrationRequestData);
                    }
                };

        // request the registration data
        Wul.getActivationManager().generateRegistrationRequestData(ActivationActivity.this,
                                                                   certificateBytes,
                                                                   registrationRequestDataProvider,
                                                                   callback);

    }

    /**
     * 5. Send the RegistrationRequestData to your PAS
     *
     * @param registrationRequestData the data to use in your PAS request
     */
    private void startRegistrationWithCesPas(final RegistrationRequestData
            registrationRequestData) {

        // Using CES Plug-in. You will use your own PAS service at this point.
        CesPaymentAppServer ces = new CesPaymentAppServer(this, mPasUrl);

        RegistrationRequest registrationRequest =
                new RegistrationRequest(String.valueOf(System.currentTimeMillis()),
                                        mPasUrl,
                                        registrationRequestData.getPaymentAppProviderId(),
                                        registrationRequestData.getPaymentAppInstanceId(),
                                        DeviceInfo.getDeviceInfo(this).getDeviceFingerprint(),
                                        Utils.getFcmToken(getApplicationContext()),
                                        registrationRequestData.getCmsDEncryptedRgk(),
                                        registrationRequestData.getCmsDEncryptedMobilePin(),
                                        registrationRequestData
                                                .getCmsDCertificateSha256Fingerprint());

        ces.register(registrationRequest, this);
    }

    /**
     * 6. Activate the WUL with the WalletRegistrationDataProvider instance
     * (you will perform this after your own PAS response)
     *
     * @param registrationDataProvider the data to use to initialize the WUL
     */
    @Override
    public void cesPasRegistrationComplete(WalletRegistrationDataProvider
            registrationDataProvider) {
        if (registrationDataProvider != null) {

            final DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(this);
            byte[] deviceFingerprint = ByteArray.of(deviceInfo.getDeviceFingerprint()).getBytes();
            byte[] encryptedDeviceFingerprint =
                    Utils.encryptData(deviceFingerprint, BuildConfig.SHARED_CRYPTO_KEY);
            Wul.getActivationManager().activate(registrationDataProvider,
                                                WalletUtils.encryptWalletData(
                                                        new CustomEncryptedData
                                                                (encryptedDeviceFingerprint)),
                                                this);
        } else {
            showErrorMessage("CES activation failed.");
        }
    }

    @Override
    public void cesPasRegistrationFailed(String s, Exception e) {
        showErrorMessage("CES Registration failed.\n" + s + "\n" + e.getMessage());
    }

    /**
     * 7. Activation is now fully complete
     *
     * @param success      true if successful
     * @param errorMessage message if an error occurred
     */
    @Override
    public void activationComplete(boolean success, String errorMessage) {
        dismissProgressDialog();
        if (success) {
            Intent i = new Intent(this, WalletActivity.class);
            startActivity(i);
            finish();
        } else {
            showErrorMessage(errorMessage);
        }
    }

    private void showErrorMessage(final String message) {
        dismissProgressDialog();
        mActivationButton.setEnabled(true);
        Utils.showErrorMessage(this, getRootView(), message);
    }

    private final class FetchFcmTokenAndDoSignUpTask extends AsyncTask<Void, String, String>{


        private WeakReference<Activity> activityReference;

        FetchFcmTokenAndDoSignUpTask(Activity activity){

            activityReference = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(final Void... params) {

            /**
             * Get the default sender id by name
             */
            String gcmDefaultSenderId = getString(
                    getResources().getIdentifier("gcm_defaultSenderId",
                                                 "string",
                                                 getApplicationContext().getPackageName()));
            WLog.d(this, "gcmDefaultSenderId = " + gcmDefaultSenderId);
            String fcmToken = null;
            try {
                fcmToken = FirebaseInstanceId.getInstance()
                                             .getToken(gcmDefaultSenderId,
                                                       FirebaseMessaging.INSTANCE_ID_SCOPE);
            } catch (IOException e) {
                WLog.e(this,
                       activityReference.get().getString(R.string.unable_to_fetch_fcm_token),
                       e);
            }

            return fcmToken;
        }

        @Override
        protected void onPostExecute(final String fcmToken) {
            Context context = activityReference.get();
            if (TextUtils.isEmpty(fcmToken)) {

                showErrorMessage(context.getString(R.string.fcm_token_null_or_empty));

            } else {
                Utils.setFcmToken(fcmToken, context);
                SignUpRequest signUpRequest =
                        new SignUpRequest(mUserId,
                                          mActivationCode,
                                          fcmToken,
                                          DeviceInfo.getDeviceInfo(
                                                  ActivationActivity.this).toJson(),
                                          "2.0",
                                          getCardHolderValidator(),
                                          WulDataManager.getInstance().getCvmModel().name());

                final CesPaymentAppServer ces =
                        new CesPaymentAppServer(context, Utils.getPasUrl(context));
                ces.signUp(signUpRequest, ActivationActivity.this);
            }
        }
    }
}