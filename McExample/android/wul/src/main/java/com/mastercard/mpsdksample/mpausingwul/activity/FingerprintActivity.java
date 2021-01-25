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

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat.AuthenticationCallback;
import androidx.core.os.CancellationSignal;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mastercard.mchipengine.walletinterface.walletcommonenumeration.TransactionOutcome;
import com.mastercard.mchipengine.walletinterface.walletdatatypes.ContactlessLog;
import com.mastercard.mpsdk.componentinterface.PaymentContext;
import com.mastercard.mpsdk.walletusabilitylayer.api.Wul;
import com.mastercard.mpsdk.walletusabilitylayer.model.WulCard;
import com.mastercard.mpsdk.walletusabilitylayer.model.transaction.PaymentData;
import com.mastercard.mpsdk.walletusabilitylayer.receiver.WalletAuthEventReceiver;
import com.mastercard.mpsdk.walletusabilitylayer.receiver.WalletContactlessTransactionOutcomeEventReceiver;
import com.mastercard.mpsdksample.mpausingwul.Constants;
import com.mastercard.mpsdksample.mpausingwul.R;
import com.mastercard.mpsdksample.mpausingwul.util.Utils;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

import static com.mastercard.mpsdk.walletusabilitylayer.log.WLog.d;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_CARD_ID;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_PAYMENT_CONTEXT;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_PAYMENT_DATA;

/**
 * PinActivity is used for:
 * 1. Entering a PIN for payment
 * 2. Setting a PIN on a card or wallet
 * 3. Changing a PIN on a card or wallet
 */
@TargetApi (23)
public class FingerprintActivity extends BaseActivity {

    private static final String KEY_NAME = "mpa_wul_fingerprint_key";
    private static final int FINGERPRINT_AUTH_REQUEST = 1;

    private ViewGroup mFingerprintMainLayout;
    private TextView mChargeMessage1View;
    private TextView mChargeMessage2View;
    private TextView mFingerprintLabelView;
    private ImageView mFingerprintImageView;
    private TextView mMessageView;
    private TextView mAotCounterView;

    private String mCardId;
    private PaymentData mPaymentData;
    private PaymentContext mPaymentContext;

    private boolean isWaiting;

    private FingerprintManagerCompat mFingerprintManager;
    private FingerprintManagerCompat.CryptoObject mCryptoObject;
    private FingerprintManagerCompat.AuthenticationCallback mAuthCallback;

    // receivers
    private class LocalWalletTransactionOutcomeEventReceiver extends WalletContactlessTransactionOutcomeEventReceiver {

        @Override
        public void onContactlessTransactionCompleted(WulCard card, ContactlessLog contactlessLog) {
            d(this, "**** [FingerprintActivity] onContactlessPaymentCompleted mCardId=" + card
                    .getCardId());

            TransactionOutcome transactionOutcome = contactlessLog.getTransactionOutcome();
            d(this, "Outcome: " + transactionOutcome.name());

            if (transactionOutcome.equals(TransactionOutcome.AUTHORIZE_ONLINE)) {
                close(0);
            }
        }

        @Override
        public void onContactlessTransactionIncident(WulCard card, Exception e) {
            d(this, "**** [FingerprintActivity] onContactlessPaymentIncident mCardId=" + card);
            showMessage("Contactless payment was not successful", R.color.red);
        }
    }

    private class LocalWalletAuthEventReceiver extends WalletAuthEventReceiver {
        @Override
        public void onAuthTimerStarted(final int secondsRemaining) {
            if (mPaymentContext == PaymentContext.CONTACTLESS) {
                mAotCounterView.setVisibility(View.VISIBLE);
                mAotCounterView.setText(secondsRemaining + " seconds remaining");
            }
        }

        @Override
        public void onAuthTimerUpdated(final int secondsRemaining) {
            if (mPaymentContext == PaymentContext.CONTACTLESS) {
                mAotCounterView.setVisibility(View.VISIBLE);
                mAotCounterView.setText(secondsRemaining + " seconds remaining");
            }
        }

        @Override
        public void onAuthTimerExpired() {
            mAotCounterView.setVisibility(View.INVISIBLE);
            isWaiting = false;
            cancelAuth();
            close(Constants.RESULT_AOT_COUNTER_EXPIRED);
        }
    }

    private LocalWalletTransactionOutcomeEventReceiver transactionEventReceiver = new
			LocalWalletTransactionOutcomeEventReceiver();
    private LocalWalletAuthEventReceiver localAuthEventReceiver = new
            LocalWalletAuthEventReceiver();


    private class FingerprintCallback extends AuthenticationCallback {

        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult
                                                              result) {
            d(this, "**** [FingerprintActivity] onAuthenticationSucceeded");

            // we have successful authentication, tell the WUL that we're done
            WulCard card = Wul.getCardManager().findCardById(mCardId);
            Wul.getPaymentManager().setCustomAuthSuccessForPayment(card);

            mFingerprintLabelView.setVisibility(View.INVISIBLE);
            mFingerprintImageView.setVisibility(View.GONE);

            if (mPaymentContext==PaymentContext.QRC) {
                close(Constants.RESULT_QRC_AUTH_SUCCESS);
            } else if (mPaymentContext == PaymentContext.CONTACTLESS) {
                // contactless second tap is required
                isWaiting = true;
                showMessage("SUCCESS\n\nPlease tap to\n complete transaction", R.color.green);
            } else {
                close(Constants.RESULT_PIN_DSRP_AUTH_SUCCESS);
            }
        }

        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            //WLog.d(this,"**** onAuthenticationError "+errMsgId+" "+errString);
        }

        @Override
        public void onAuthenticationFailed() {
            d(this, "**** [FingerprintActivity] onAuthenticationFailed");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_fingerprint);
        super.onCreate(savedInstanceState);

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab=getSupportActionBar();
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowHomeEnabled(false);

        // views
        mFingerprintMainLayout = (ViewGroup) findViewById(R.id.fingerprint_main_layout);
        mChargeMessage1View = (TextView) findViewById(R.id.fingerprint_charge_message_1);
        mChargeMessage2View = (TextView) findViewById(R.id.fingerprint_charge_message_2);
        mFingerprintLabelView = (TextView) findViewById(R.id.fingerprint_label);
        mFingerprintImageView = (ImageView) findViewById(R.id.fingerprint_image);
        mMessageView = (TextView) findViewById(R.id.fingerprint_message);
        mAotCounterView = (TextView) findViewById(R.id.fingerprint_aot_counter);

		/*
         User can tap the screen to restart the auth timer. This will
		 delay timer expiry allowing the user more time if required.
		 */
        mFingerprintMainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isWaiting) {
                    Wul.getPaymentManager().restartAuthTimer();
                }
            }
        });

        // intent
        Intent i = getIntent();
        mCardId = i.getStringExtra(EXTRA_CARD_ID);
        mPaymentData = (PaymentData) i.getSerializableExtra(EXTRA_PAYMENT_DATA);
        mPaymentContext = (PaymentContext) i.getSerializableExtra(EXTRA_PAYMENT_CONTEXT);

        if (mPaymentContext == null) {
            mPaymentContext = PaymentContext.CONTACTLESS;
        }

        if (mPaymentData != null) {
            mChargeMessage1View.setVisibility(View.VISIBLE);
            String paymentText = mPaymentData.getCurrency().getSymbol() + mPaymentData
                    .getAuthorizedAmount().toString();
            mChargeMessage2View.setText(paymentText);
            mChargeMessage2View.setVisibility(View.VISIBLE);
        }

        // fingerprint
        mFingerprintManager = FingerprintManagerCompat.from(this);
        mCryptoObject = new FingerprintManagerCompat.CryptoObject(getFingerprintCipher());
        mAuthCallback = new FingerprintCallback();

        transactionEventReceiver.register(this);
        localAuthEventReceiver.register(this);

        isWaiting = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        startAuth();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //stopCounter();
        transactionEventReceiver.unregister(this);
        localAuthEventReceiver.unregister(this);
    }

    @Override
    public void onBackPressed() {
        cancelAuth();
        close(Constants.RESULT_CANCELLED);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    public void startAuth() {
        d(this, "**** startAuth");

        // check runtime permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) !=
                PackageManager.PERMISSION_GRANTED) {
            d(this, "We need fingerprint permission...");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission
                    .USE_FINGERPRINT}, FINGERPRINT_AUTH_REQUEST);
            return;
        }

        if (Utils.hasFingerprintHardware(this)) {
            if (Utils.hasFingerprintsEnrolledOnDevice(this)) {
                if (Utils.isLockscreenEnabled(this)) {
                    // call fingerprint auth
                    CancellationSignal cancellationSignal = new CancellationSignal();
                    mFingerprintManager.authenticate(mCryptoObject, 0, cancellationSignal,
                            mAuthCallback, null);
                    return;
                }
            }
        }
        // TODO error messages
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        d(this, "**** onRequestPermissionsResult");
        if (requestCode == FINGERPRINT_AUTH_REQUEST && grantResults.length > 0 && grantResults[0]
                == PackageManager.PERMISSION_GRANTED) {
            d(this, "Fingerprint permission granted!");
            // permission granted - proceed with auth
            startAuth();
        } else {
            d(this, "Fingerprint permission NOT granted!");
            close(Constants.RESULT_FINGERPRINT_PERMISSION_REFUSED);
        }
    }

    private void cancelAuth() {
        final WulCard card = Wul.getCardManager().findCardById(mCardId);
        Wul.getPaymentManager().stopContactlessTransaction(card);
        Wul.getCardManager().clearSelectedCard();
        Wul.getPaymentManager().clearAuthForPayment();
    }

    private void showMessage(final String message, final int colorId) {
        mMessageView.setVisibility(View.VISIBLE);
        mMessageView.setTextColor(getResources().getColor(colorId));
        mMessageView.setText(message);
    }

    private Cipher getFingerprintCipher() {
        try {

            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(false)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();

            Cipher cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/" +
                            KeyProperties.BLOCK_MODE_CBC + "/" +
                            KeyProperties.ENCRYPTION_PADDING_PKCS7
            );
            cipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey(KEY_NAME, null));
            return cipher;
        } catch (NoSuchAlgorithmException |
                NoSuchPaddingException |
                NoSuchProviderException |
                InvalidAlgorithmParameterException |
                IOException |
                CertificateException |
                UnrecoverableKeyException |
                KeyStoreException |
                InvalidKeyException e
                ) {
            throw new RuntimeException("Failed to create cipher", e);
        }
    }

    private void close(int result) {
        transactionEventReceiver.unregister(this);
        localAuthEventReceiver.unregister(this);
        mAotCounterView.setVisibility(View.INVISIBLE);
        setResult(result);
        finish();
    }

}
