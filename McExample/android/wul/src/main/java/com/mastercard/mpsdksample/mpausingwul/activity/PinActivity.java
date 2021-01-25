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

import com.mastercard.mchipengine.walletinterface.walletcommonenumeration.TransactionOutcome;
import com.mastercard.mchipengine.walletinterface.walletdatatypes.ContactlessLog;
import com.mastercard.mpsdk.componentinterface.PaymentContext;
import com.mastercard.mpsdk.componentinterface.crypto.keys.CustomEncryptedData;
import com.mastercard.mpsdk.componentinterface.crypto.keys.WalletDekEncryptedData;
import com.mastercard.mpsdk.walletusabilitylayer.api.Wul;
import com.mastercard.mpsdk.walletusabilitylayer.config.UserAuthMode;
import com.mastercard.mpsdk.walletusabilitylayer.model.WulCard;
import com.mastercard.mpsdk.walletusabilitylayer.model.transaction.PaymentData;
import com.mastercard.mpsdk.walletusabilitylayer.receiver.WalletAuthEventReceiver;
import com.mastercard.mpsdk.walletusabilitylayer.receiver.WalletContactlessTransactionOutcomeEventReceiver;
import com.mastercard.mpsdk.walletusabilitylayer.util.WalletUtils;
import com.mastercard.mpsdksample.mpausingwul.Constants;
import com.mastercard.mpsdksample.mpausingwul.R;
import com.mastercard.mpsdksample.mpausingwul.util.Utils;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import static com.mastercard.mpsdk.walletusabilitylayer.log.WLog.d;
import static com.mastercard.mpsdksample.mpausingwul.BuildConfig.SHARED_CRYPTO_KEY;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_CARD_ID;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_PAYMENT_CONTEXT;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_PAYMENT_DATA;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_PIN_MODE;

/**
 * PinActivity is used for:
 * 1. Entering a PIN for payment
 * 2. Setting a PIN on a card or wallet
 * 3. Changing a PIN on a card or wallet
 */
public class PinActivity extends BaseActivity {

    public static final int PIN_CONFIRM_EXISTING = 1;    // change PIN
    public static final int PIN_ENTER_NEW = 2;    // change/set PIN
    public static final int PIN_CONFIRM_NEW = 3;    // change/set PIN
    public static final int PIN_ENTER_ONCE = 4;    // payment

    private static final int PIN_LENGTH = 4;

    private TextView mPinChargeMessage1View;
    private TextView mPinChargeMessage2View;
    private TextView mPinLabelView;
    private LinearLayout mPinLayout;
    private EditText[] mPinView;
    private TextView mMessageView;
    private TextView mAotCounterView;

    private int mMode;
    private int mStartingMode;
    private String mCardId;
    private PaymentData mPaymentData;
    private PaymentContext mPaymentContext;

    private int pinIndex;
    private boolean isWaiting;

    // pins to use in response (always protected)
    private WalletDekEncryptedData mExistingProtectedPin;
    private WalletDekEncryptedData mNewProtectedPin;

    // receivers
    private class LocalWalletTransactionOutcomeEventReceiver extends WalletContactlessTransactionOutcomeEventReceiver {
        @Override
        public void onContactlessTransactionCompleted(WulCard card, ContactlessLog contactlessLog) {
            d(this, "**** [PinActivity] onContactlessPaymentCompleted mCardId=" + card.getCardId());

            TransactionOutcome transactionOutcome = contactlessLog.getTransactionOutcome();
            d(this, "Outcome: " + transactionOutcome.name());

            if (transactionOutcome == TransactionOutcome.AUTHORIZE_ONLINE) {
                // TransactionActivity will show notification
                close(0);
            } else if (transactionOutcome == TransactionOutcome.DECLINE_BY_CARD) {
                Utils.showErrorMessage(PinActivity.this,
                        getRootView(),
                        getString(R.string.transaction_declined_did_you_set_wallet_pin));
            } else if (transactionOutcome == TransactionOutcome.DECLINE_BY_TERMINAL) {
                Utils.showErrorMessage(PinActivity.this,
                        getRootView(),
                        getString(R.string.transaction_declined_by_issuer));
            }
        }

        @Override
        public void onContactlessTransactionIncident(WulCard card, Exception e) {
            d(this, "**** [PinActivity] onContactlessPaymentIncident mCardId=" + card.getCardId());
            Utils.showErrorMessage(PinActivity.this,
                    getRootView(),
                    getString(R.string.contactless_payment_unsuccessful));
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

    private LocalWalletTransactionOutcomeEventReceiver cardEventReceiver = new
            LocalWalletTransactionOutcomeEventReceiver();
    private LocalWalletAuthEventReceiver pinAuthEventReceiver = new
            LocalWalletAuthEventReceiver();

    private final class PinTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() > 0) {
                //WLog.d(this,"pinIndex="+pinIndex);
                if (pinIndex < 3) {
                    pinIndex++;
                    mPinView[pinIndex].setEnabled(true);
                    mPinView[pinIndex].requestFocus();
                } else if (pinIndex == 3) {
                    if (validatePin()) {
                        byte[] pin = new byte[PIN_LENGTH];
                        for (int n = 0; n < PIN_LENGTH; n++) {
                            pin[n] = mPinView[n].getText().toString().getBytes()[0];
                        }
                        s = null;
                        if (pin.length == PIN_LENGTH) {
                            // the PIN must be between 4 and 8 bytes
                            CustomEncryptedData protectedPin =
                                    new CustomEncryptedData(Utils.encryptData(pin,
                                            SHARED_CRYPTO_KEY));
                            checkPin(WalletUtils.encryptWalletData(protectedPin));
                        }
                    }
                }
            }
        }

        private boolean validatePin() {
            int len = 0;
            for (int n = 0; n < PIN_LENGTH; n++) {
                len += mPinView[n].length();
            }
            return (len == PIN_LENGTH);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_pin);
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowHomeEnabled(false);

        // views
        ViewGroup pinMainLayout = (ViewGroup) findViewById(R.id.pin_main_layout);
        mPinChargeMessage1View = (TextView) findViewById(R.id.pin_charge_message_1);
        mPinChargeMessage2View = (TextView) findViewById(R.id.pin_charge_message_2);
        mPinLabelView = (TextView) findViewById(R.id.pin_label);
        mPinLayout = (LinearLayout) findViewById(R.id.pin_layout);
        mPinView = new EditText[PIN_LENGTH];
        mPinView[0] = (EditText) findViewById(R.id.pin_1);
        mPinView[1] = (EditText) findViewById(R.id.pin_2);
        mPinView[2] = (EditText) findViewById(R.id.pin_3);
        mPinView[3] = (EditText) findViewById(R.id.pin_4);

        for (int n = 0; n < PIN_LENGTH; n++) {
            mPinView[n].addTextChangedListener(new PinTextWatcher());
            final int index = n;
            mPinView[n].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        pinIndex = index;
                    }
                }
            });
            mPinView[n].setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN &&
                            keyCode == KeyEvent.KEYCODE_DEL &&
                            pinIndex > 0) {
                        pinIndex--;
                        mPinView[pinIndex].setText("");
                        mPinView[pinIndex].requestFocus();
                        return true;
                    }
                    return false;
                }
            });
        }
        mMessageView = (TextView) findViewById(R.id.pin_message);
        mAotCounterView = (TextView) findViewById(R.id.pin_aot_counter);

		/*
         User can tap the screen to restart the auth timer. This will
		 delay timer expiry allowing the user more time if required.
		 */
        pinMainLayout.setOnClickListener(new View.OnClickListener() {
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
        mStartingMode = i.getIntExtra(EXTRA_PIN_MODE, PIN_ENTER_NEW);
        mPaymentData = (PaymentData) i.getSerializableExtra(EXTRA_PAYMENT_DATA);
        mPaymentContext = (PaymentContext) i.getSerializableExtra(EXTRA_PAYMENT_CONTEXT);

        if (mPaymentContext == null) {
            mPaymentContext = PaymentContext.CONTACTLESS;
        }

        cardEventReceiver.register(this);
        pinAuthEventReceiver.register(this);

        isWaiting = false;
        mMode = mStartingMode;
        d(this, "Starting mode: " + mStartingMode);

        refreshView();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //stopCounter();
        cardEventReceiver.unregister(this);
        pinAuthEventReceiver.unregister(this);
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

    private void refreshView() {

        if (mPaymentData != null) {
            mPinChargeMessage1View.setText("You are being charged:");
            String paymentText = mPaymentData.getCurrency().getSymbol() + mPaymentData
                    .getAuthorizedAmount().toString();
            mPinChargeMessage2View.setText(paymentText);
            mPinChargeMessage2View.setVisibility(View.VISIBLE);
        }

        if (mMode == PIN_CONFIRM_EXISTING) {
            mPinChargeMessage1View.setText("Change PIN");
            mPinLabelView.setText("Enter current PIN");
        } else if (mMode == PIN_ENTER_NEW) {
            mPinChargeMessage1View.setText(mStartingMode == PIN_CONFIRM_EXISTING ? "Change PIN" :
                    "Set PIN");
            mPinLabelView.setText("Enter new PIN");
        } else if (mMode == PIN_CONFIRM_NEW) {
            mPinChargeMessage1View.setText(mStartingMode == PIN_CONFIRM_EXISTING ? "Change PIN" :
                    "Set PIN");
            mPinLabelView.setText("Confirm new PIN");
        } else if (mMode == PIN_ENTER_ONCE) {
            if (mPaymentData != null) {
                mPinChargeMessage1View.setText("You are being charged:");
            }
            mPinLabelView.setText("Enter PIN");
        }
        for (int n = 0; n < mPinView.length; n++) {
            mPinView[n].setText("");
        }
        mPinView[0].requestFocus();
    }

    private void checkPin(final WalletDekEncryptedData protectedPin) {

        if (mMode == PIN_CONFIRM_EXISTING) {
            mMode = PIN_ENTER_NEW;
            mExistingProtectedPin = protectedPin;
        } else if (mMode == PIN_ENTER_NEW) {
            mMode = PIN_CONFIRM_NEW;
            mNewProtectedPin = protectedPin;
        } else if (mMode == PIN_CONFIRM_NEW) {
            if (WalletUtils.isEqual(this.mNewProtectedPin, protectedPin)) {
                pinConfirmed();
                close(Constants.RESULT_PIN_ENTERED);
            } else {
                showMessage("PIN does not match", R.color.red);
                this.mNewProtectedPin = null;
                this.mExistingProtectedPin = null;
                mMode = mStartingMode;
            }
        } else if (mMode == PIN_ENTER_ONCE) {
            mNewProtectedPin = protectedPin;

            pinConfirmed();

            if (mPaymentContext == PaymentContext.QRC) {
                close(Constants.RESULT_QRC_AUTH_SUCCESS);
            } else if (mPaymentContext == PaymentContext.CONTACTLESS) {
                // CONTACTLESS: second tap is required
                showMessage("Please tap to\ncomplete transaction", R.color.green);
            } else {
                // DSRP
                close(Constants.RESULT_PIN_DSRP_AUTH_SUCCESS);
            }

        }
        refreshView();
    }

    private void pinConfirmed() {

        mPinLayout.setVisibility(View.INVISIBLE);
        mPinLabelView.setVisibility(View.INVISIBLE);
        Utils.hideKeyboard(this, mPinView[0]);

		/*
         * normally we would know our config authentication option, but in this
		 * sample activity we allow for both wallet and card pin variations
		 */
        final boolean walletPIN = (Wul.getWalletConfiguration().getUserAuthMode() == UserAuthMode.WALLET_PIN);
        final WulCard card = Wul.getCardManager().findCardById(mCardId);

        // The request id for a PIN set/change, in case we need to query the task
        String requestId = null;

        if (mStartingMode == PIN_ENTER_NEW) {
            // set pin
            if (walletPIN) {
                Wul.getCardManager().setWalletPin(mNewProtectedPin);
            } else {
                Wul.getCardManager().setCardPin(card, mNewProtectedPin);
            }
        } else if (mStartingMode == PIN_CONFIRM_EXISTING) {
            // change pin
            if (walletPIN) {
                Wul.getCardManager().changeWalletPin(mNewProtectedPin, mExistingProtectedPin);
            } else {
                Wul.getCardManager().changeCardPin(card, mNewProtectedPin, mExistingProtectedPin);
            }
        } else if (mStartingMode == PIN_ENTER_ONCE) {
            // this is a payment event - tell the WUL that we have a PIN
            Wul.getPaymentManager().setPinForPayment(card, mNewProtectedPin);
            isWaiting = true;
        }

        mNewProtectedPin = null;
        mExistingProtectedPin = null;
        mCardId = null;
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

    private void close(int resultCode) {
        // hide pin auth counter immediately
        pinAuthEventReceiver.unregister(this);
        mAotCounterView.setVisibility(View.INVISIBLE);
        setResult(resultCode);
        finish();
    }
}
