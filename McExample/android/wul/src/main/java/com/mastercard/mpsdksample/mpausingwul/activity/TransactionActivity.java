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

import static com.mastercard.mpsdk.walletusabilitylayer.log.WLog.d;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_CARD_ID;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_PAYMENT_ACTION;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_PAYMENT_CONTEXT;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_PAYMENT_DATA;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_PIN_MODE;
import static com.mastercard.mpsdksample.mpausingwul.Constants.REQUEST_LOCK_SCREEN;
import static com.mastercard.mpsdksample.mpausingwul.Constants.REQUEST_PRE_AUTHENTICATE;
import static com.mastercard.mpsdksample.mpausingwul.Constants.RESULT_ABORTED;
import static com.mastercard.mpsdksample.mpausingwul.Constants.RESULT_DECLINED;
import static com.mastercard.mpsdksample.mpausingwul.Constants.RESULT_NO_SELECTED_CARD;
import static com.mastercard.mpsdksample.mpausingwul.Constants.RESULT_UNLOCK_REQUIRED;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mastercard.mchipengine.walletinterface.walletcommonenumeration.AbortReason;
import com.mastercard.mchipengine.walletinterface.walletcommonenumeration.TransactionOutcome;
import com.mastercard.mchipengine.walletinterface.walletdatatypes.ContactlessLog;
import com.mastercard.mchipengine.walletinterface.walletdatatypes.TransactionInformation;
import com.mastercard.mpsdk.componentinterface.DsrpOutputData;
import com.mastercard.mpsdk.componentinterface.DsrpTransactionContext;
import com.mastercard.mpsdk.componentinterface.DsrpTransactionOutcome;
import com.mastercard.mpsdk.componentinterface.PaymentContext;
import com.mastercard.mpsdk.componentinterface.RemoteCryptogramType;
import com.mastercard.mpsdk.utils.bytes.ByteArray;
import com.mastercard.mpsdk.walletusabilitylayer.api.Wul;
import com.mastercard.mpsdk.walletusabilitylayer.config.UserAuthMode;
import com.mastercard.mpsdk.walletusabilitylayer.exception.MissingIntentExtraException;
import com.mastercard.mpsdk.walletusabilitylayer.log.WLog;
import com.mastercard.mpsdk.walletusabilitylayer.model.WulCard;
import com.mastercard.mpsdk.walletusabilitylayer.model.transaction.DsrpRequestOutcome;
import com.mastercard.mpsdk.walletusabilitylayer.model.transaction.DsrpTransaction;
import com.mastercard.mpsdk.walletusabilitylayer.model.transaction.PaymentData;
import com.mastercard.mpsdk.walletusabilitylayer.receiver.WalletAuthEventReceiver;
import com.mastercard.mpsdk.walletusabilitylayer.receiver.WalletContactlessTransactionOutcomeEventReceiver;
import com.mastercard.mpsdk.walletusabilitylayer.util.Device;
import com.mastercard.mpsdksample.mpausingwul.BuildConfig;
import com.mastercard.mpsdksample.mpausingwul.Constants;
import com.mastercard.mpsdksample.mpausingwul.R;
import com.mastercard.mpsdksample.mpausingwul.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;


public class TransactionActivity extends BaseActivity {

	public static final int ACTION_WAIT = 0;
	public static final int ACTION_AUTH = 1;

	/**
	 * Key used for data denoting a payment amount.
	 */
	public final static String KEY_AMOUNT = "amount";

	/**
	 * Key used for data denoting a payment currency.
	 */
	public final static String KEY_CURRENCY = "currency";

	/**
	 * the country code
	 */
	public static final String KEY_COUNTRY_CODE = "country_code";

	/**
	 * the cryptogram type
	 */
	public static final String KEY_CRYPTOGRAM_TYPE = "cryptogram_type";

	/**
	 * Receiver for card or wallet PIN event.
	 */
	private class LocalWalletAuthEventReceiver extends WalletAuthEventReceiver {
		@Override
		public void onAuthRequiredForContactless(WulCard card, ContactlessLog contactlessLog,
												PaymentData paymentData) {
			d(this, "**** [TransactionActivity] onPinRequiredForContactless");
			authRequired(card,paymentData,PaymentContext.CONTACTLESS);
		}

		@Override
		public void onAuthRequiredForDsrp(WulCard card, PaymentData paymentData) {
			d(this, "**** [TransactionActivity] onPinRequiredForDsrp");
			authRequired(card,paymentData,PaymentContext.DSRP);
		}

		@Override
		public void onUnlockRequired(final WulCard card, final ContactlessLog contactlessLog, final
		PaymentData paymentData) {
			d(this, "**** [TransactionActivity] onUnlockRequired");
			Wul.getPaymentManager().stopContactlessTransaction(card);
			vibrate(1000);
			closeWithMessage("Unlock required", true, RESULT_UNLOCK_REQUIRED);
		}

		private void authRequired(final WulCard card, final PaymentData paymentData, final
								  PaymentContext paymentContext) {
            if (Device.isScreenLocked(TransactionActivity.this)) {
                // vibrate warning that we must unlock first
                vibrate(1000);
            }
            Intent i = new Intent(TransactionActivity.this, PinActivity.class);
            final UserAuthMode userAuthMode =  Wul.getWalletConfiguration().getUserAuthMode();
            if (userAuthMode == UserAuthMode.CUSTOM) {
                i = new Intent(TransactionActivity.this, FingerprintActivity.class);
            } else if (userAuthMode == UserAuthMode.WALLET_PIN || userAuthMode == UserAuthMode.CARD_PIN) {
                i.putExtra(EXTRA_PIN_MODE, PinActivity.PIN_ENTER_ONCE);
            }
            i.putExtra(EXTRA_CARD_ID, card.getCardId());
            i.putExtra(EXTRA_PAYMENT_DATA, paymentData);
            i.putExtra(EXTRA_PAYMENT_CONTEXT, paymentContext);
            startActivityForResult(i, 0);
        }

    }


	private class LocalWalletTransactionOutcomeEventReceiver extends WalletContactlessTransactionOutcomeEventReceiver {
		@Override
		public void onContactlessTransactionCompleted(WulCard card, ContactlessLog contactlessLog) {
			d(this, "**** [TransactionActivity] onContactlessPaymentCompleted card=" + card);

			TransactionOutcome transactionOutcome = contactlessLog.getTransactionOutcome();
			TransactionInformation transactionInformation = contactlessLog
					.getTransactionInformation();
			PaymentData paymentData = new PaymentData(contactlessLog.getTransactionInformation(),
					contactlessLog.getTerminalInformation());
			d(this, "Outcome: " + transactionOutcome.name());

			if (transactionOutcome == TransactionOutcome.AUTHORIZE_ONLINE) {
				showPaymentNotification(paymentData);
				vibrate(200);
				closeWithMessage("Transaction complete", false, RESULT_OK);
			} else if (transactionOutcome == TransactionOutcome.WALLET_ACTION_REQUIRED) {
				// an auth event is about to occur (other Receivers will handle this)
				finish();
			} else if (transactionOutcome == TransactionOutcome.DECLINE_BY_CARD ||
					transactionOutcome == TransactionOutcome.DECLINE_BY_TERMINAL) {
				closeWithMessage("Transaction declined", true, RESULT_DECLINED);
			}
		}

		@Override
		public void onContactlessTransactionAborted(WulCard card,
												AbortReason abortReason,
												Exception e) {
			d(this, "**** [TransactionActivity] onContactlessPaymentAborted card=" + card + " " +
					"abortReason=" + abortReason.name());
			WLog.e(this, "Transaction aborted", e);
			closeWithMessage(getString(R.string.contactless_transaction_abort_message) +
									 abortReason.name()+" message: " +e.getMessage(),
							 true, RESULT_ABORTED);
		}

		@Override
		public void onContactlessTransactionIncident(WulCard card, Exception e) {
			d(this, "**** [TransactionActivity] onContactlessPaymentIncident card=" + card);
			// TODO exception thrown by MChipEngine

		}
	}

	private ImageView mSuccessIcon;
	private TextView mMessage;
	private ProgressBar mProgressBar;

	// globals
	private String mCardId;
	private WulCard mCard;
	private PaymentContext mPaymentContext;
	private int mAction;

	// DSRP only
	private DsrpTransactionContext dsrpTransactionContext;

	private LocalWalletAuthEventReceiver authEventReceiver = new
            LocalWalletAuthEventReceiver();
	private LocalWalletTransactionOutcomeEventReceiver transactionEventReceiver = new
			LocalWalletTransactionOutcomeEventReceiver();

	@Override
	public void onCreate(Bundle savedInstanceState) {

		d(this, "**** onCreate");

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager
				.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_transaction);

		mSuccessIcon = (ImageView) findViewById(R.id.transaction_success_icon);
		mProgressBar = (ProgressBar) findViewById(R.id.transaction_progress_bar);
		mMessage = (TextView) findViewById(R.id.transaction_message);

		Intent i=getIntent();
		try {
			Uri uri=i.getData();
			if(uri!=null && uri.getScheme().equals("anywallet")){
				// DSRP
				mPaymentContext = PaymentContext.DSRP;
				mAction = ACTION_WAIT;
				mCard = Wul.getCardManager().getSelectedCard();
				if(mCard==null || !mCard.isDsrpSupported()){
					mCard = Wul.getCardManager().getDefaultCard(PaymentContext.DSRP);
				}
				if(mCard!=null) {
					mCardId = mCard.getCardId();
				} else {
					WLog.d(this, "Transaction failed: no available card for DSRP");
					closeWithMessage("Transaction cancelled:\nno card available",true,
							RESULT_NO_SELECTED_CARD);
					return;
				}
				buildDsrpRequest(i);
			} else {
				// CONTACTLESS
				mCardId = i.getStringExtra(EXTRA_CARD_ID);
				mPaymentContext = (PaymentContext) i.getSerializableExtra(EXTRA_PAYMENT_CONTEXT);
				mAction = i.getIntExtra(EXTRA_PAYMENT_ACTION, ACTION_WAIT);
				mCard = Wul.getCardManager().findCardById(mCardId);
				if(mCard==null) {
					WLog.e(this, "Failed to launch TransactionActivity. Cannot find card", new
							MissingIntentExtraException("Card ID unknown"));
					finish();
					return;
				}
			}
		} catch(Exception e) {
			WLog.e(this, "Failed to launch TransactionActivity", e);
			finish();
			return;
		}

		// if we are pre-authenticating, make sure the correct card is selected
		Wul.getCardManager().setSelectedCard(mCard);

		authEventReceiver.register(this);
		transactionEventReceiver.register(this);

		WLog.d(this,"build="+ BuildConfig.BUILD_TYPE);
		WLog.d(this,"cardId="+mCardId);
		WLog.d(this,"action="+mAction);
		WLog.d(this,"paymentContext="+mPaymentContext);

		if(mAction==ACTION_AUTH || mPaymentContext==PaymentContext.DSRP){
			launchAuth();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Wul.getCardManager().clearSelectedCard();
		Wul.getPaymentManager().clearAuthForPayment();
		authEventReceiver.unregister(this);
		transactionEventReceiver.unregister(this);
	}

	@Override
	public void onActivityResult(int requestCode, int responseCode, Intent i) {
		WLog.d(this, "**** onActivityResult " + requestCode + " " + responseCode);
		if (responseCode == Constants.RESULT_CANCELLED) {
			setResult(responseCode);
			closeWithMessage("Transaction cancelled", true, responseCode);
		} else if (responseCode == Constants.RESULT_AOT_COUNTER_EXPIRED) {
			setResult(responseCode);
			closeWithMessage("Transaction expired", true, responseCode);
		} else if(responseCode == Constants.RESULT_PIN_DSRP_AUTH_SUCCESS) {
			// auth entered - attempt DSRP again
			processDsrpRequest();
		}
	}

	private void launchAuth() {

		final UserAuthMode authMode = Wul.getWalletConfiguration().getUserAuthMode();

		if(authMode == UserAuthMode.WALLET_PIN || authMode == UserAuthMode.CARD_PIN) {

			// if using WALLET_PIN
			Intent i = new Intent(this, PinActivity.class);
			i.putExtra(EXTRA_CARD_ID, mCardId);
			i.putExtra(EXTRA_PIN_MODE, PinActivity.PIN_ENTER_ONCE);
			i.putExtra(EXTRA_PAYMENT_CONTEXT, mPaymentContext);
			startActivityForResult(i, REQUEST_PRE_AUTHENTICATE);

		} else if(authMode == UserAuthMode.CUSTOM) {

			// if using CUSTOM
			Intent i = new Intent(this, FingerprintActivity.class);
			i.putExtra(EXTRA_CARD_ID, mCardId);
			i.putExtra(EXTRA_PAYMENT_CONTEXT, mPaymentContext);
			startActivityForResult(i, REQUEST_PRE_AUTHENTICATE);

		} else {

			// No auth to launch!
			finish();
		}
	}

	private void buildDsrpRequest(Intent i) {

		final long amount = i.getLongExtra(KEY_AMOUNT, 0);
		final int currencyCode = i.getIntExtra(KEY_CURRENCY, 0);
		final int countryCode = i.getIntExtra(KEY_COUNTRY_CODE, 0);
		final String cryptogramType = i.getStringExtra(KEY_CRYPTOGRAM_TYPE);

		DsrpTransaction.Builder dsrpBuilder = new DsrpTransaction.Builder()
				.withTransactionAmount(amount)
				.withCurrencyCode(currencyCode)
				.usingOptionalCountryCode(countryCode)
				.usingOptionalTransactionType((byte) 1)
				.withCryptogramType(RemoteCryptogramType.valueOf(cryptogramType))
				.withUnpredictableNumber(29496729)
				.usingOptionalTransactionDate(new Date(System.currentTimeMillis()));
		dsrpTransactionContext = dsrpBuilder.build();
	}

	private void processDsrpRequest() {
		d(this, "**** processDsrpRequest");
		DsrpRequestOutcome requestOutcome = Wul.getPaymentManager().processDsrpTransaction(this,
			dsrpTransactionContext);
		processDsrpResult(requestOutcome);
	}

	private void processDsrpResult(DsrpRequestOutcome requestOutcome) {

		d(this, "**** processDsrpResult");

		DsrpRequestOutcome.Result requestResult = requestOutcome.getResult();
		DsrpTransactionOutcome transactionOutcome = requestOutcome.getDsrpTransactionOutcome();
		d(this, "DsrpRequestOutcome.Result=" + requestResult.name());

		if (requestResult == DsrpRequestOutcome.Result.REQUEST_SUCCESSFUL &&
				transactionOutcome != null &&
				transactionOutcome.getDsrpOutputData() != null) {

			try {
				DsrpOutputData d=transactionOutcome.getDsrpOutputData();
				JSONObject j = new JSONObject();
				j.put("result", transactionOutcome.getResult());
				j.put("cryptogramType",d.getCryptogramType());
				j.put("expirationDate",d.getExpirationDate());
				j.put("pan",d.getPan());
				j.put("panSequenceNumber",d.getPanSequenceNumber());
				j.put("transactionId",ByteArray.of(d.getTransactionId()).toHexString());
				j.put("transactionCryptogramData", ByteArray.of(d.getTransactionCryptogramData
						()).toHexString());
				j.put("par",ByteArray.of(d.getPar()).toHexString());
				j.put("track2Data",d.getTrack2Data());
				Intent responseIntent=new Intent();
				responseIntent.putExtra("dsrp",j.toString());
				setResult(RESULT_OK,responseIntent);
				finish();
			} catch (JSONException e){
				WLog.e(this,"Failed to create response JSON for issuer",e);
			}

		} else if (requestResult == DsrpRequestOutcome.Result.AUTHENTICATION_REQUIRED) {
			// No-op: PinActivity will launch from receiver
		} else {
			// SDK DSRP request failed
			closeWithMessage("DSRP failed: "+requestOutcome.getResult().name(),true,0);
		}
	}

	private void closeWithMessage(final String message,
								  final boolean isError,
								  final int resultCode) {
		if (Utils.isScreenLocked(this) && isError) {
			showErrorNotification(message);
			finish();
			return;
		}
		mMessage.setText(message);
		mSuccessIcon.setImageResource(isError? R.drawable.ic_error:R.drawable.ic_check_circle);
		mProgressBar.setVisibility(View.GONE);
		mSuccessIcon.setVisibility(View.VISIBLE);
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				setResult(resultCode);
				finish();
			}
		}, 2500);
	}

	private void showPaymentNotification(PaymentData paymentData) {

		final String amount = paymentData.getCurrency().getSymbol() + paymentData
				.getAuthorizedAmount().toString();

		Utils.showNotification(this,
				"Thank you!",
				"A payment of " + amount + " was successfully processed"
		);
	}

	private void showErrorNotification(String message) {
		Utils.showNotification(this,
				message,
				"There was a problem with this transaction. Please try again."
		);
	}

	private void vibrate(final int duration) {
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(duration);
	}

	/**
	 * On Android 5.0+ we can lock the screen programmatically. Not called in this sample, but
	 * this might be useful in an implementation of a locally-verified wallet.
	 * @param act
	 */
	@TargetApi (21)
	public void lockScreen(Activity act) {
		WLog.d(this,"**** lockScreen");
		KeyguardManager keyguardManager=(KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
		Intent i=keyguardManager.createConfirmDeviceCredentialIntent("title","desc");
		act.startActivityForResult(i, REQUEST_LOCK_SCREEN);
	}
}
