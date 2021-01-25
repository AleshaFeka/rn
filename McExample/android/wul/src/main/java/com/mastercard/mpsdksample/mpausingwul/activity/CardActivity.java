/*******************************************************************************
 * Copyright (c) 2019, MasterCard International Incorporated and/or its
 * affiliates. All rights reserved.
 *
 * The contents of this file may only be used subject to the MasterCard
 * Mobile Payment SDK for MCBP and/or MasterCard Mobile MPP UI SDK
 * Materials License.
 *
 * Please refer to the file LICENSE.TXT for full details.
 *
 * TO THE EXTENT PERMITTED BY LAW, THE SOFTWARE IS PROVIDED "AS IS", WITHOUT
 * WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON INFRINGEMENT. TO THE EXTENT PERMITTED BY LAW, IN NO EVENT SHALL
 * MASTERCARD OR ITS AFFILIATES BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 ******************************************************************************/

package com.mastercard.mpsdksample.mpausingwul.activity;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.mastercard.mpsdk.componentinterface.PaymentContext;
import com.mastercard.mpsdk.componentinterface.database.TransactionLog;
import com.mastercard.mpsdk.componentinterface.database.state.CardState;
import com.mastercard.mpsdk.walletusabilitylayer.api.Wul;
import com.mastercard.mpsdk.walletusabilitylayer.config.UserAuthMode;
import com.mastercard.mpsdk.walletusabilitylayer.log.WLog;
import com.mastercard.mpsdk.walletusabilitylayer.model.WulCard;
import com.mastercard.mpsdk.walletusabilitylayer.receiver.WalletCardManagerEventReceiver;
import com.mastercard.mpsdksample.mpausingwul.Constants;
import com.mastercard.mpsdksample.mpausingwul.R;
import com.mastercard.mpsdksample.mpausingwul.adapter.TransactionLogAdapter;
import com.mastercard.mpsdksample.mpausingwul.listener.OnCardActionListenerImpl;
import com.mastercard.mpsdksample.mpausingwul.util.Utils;
import com.mastercard.mpsdksample.mpausingwul.view.WalletCardView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import static com.mastercard.mpsdk.walletusabilitylayer.api.Wul.getCardManager;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_CARD_ID;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_PAYMENT_ACTION;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_PAYMENT_CONTEXT;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_PIN_MODE;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_QRC_AMOUNT;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_QRC_CURRENCY;
import static com.mastercard.mpsdksample.mpausingwul.Constants.REQUEST_PIN;
import static com.mastercard.mpsdksample.mpausingwul.Constants.REQUEST_PRE_AUTHENTICATE;
import static com.mastercard.mpsdksample.mpausingwul.Constants.RESULT_CLOSE_CARD;
import static com.mastercard.mpsdksample.mpausingwul.Constants.RESULT_CLOSE_REQUEST_SUCCESS;

public class CardActivity extends BaseActivity implements
                                               View.OnClickListener {
    private TextView mCardTokenView;
    private TextView mCardStateView;
    private TextView mTransactionLogMessageView;
    private Button mPayButton;
    private WulCard mCard;
    private RecyclerView mRecyclerView;
    private TransactionLogAdapter mTransactionLogAdapter;
    private Button mQrcButton;
    private Double mQrcAmount;
    private int mQrcCurrency;
    private LocalWalletCardManagerEventReceiver mCardManagerReceiver =
            new LocalWalletCardManagerEventReceiver();
    private WalletCardView mWalletCardView;


    private class LocalWalletCardManagerEventReceiver extends WalletCardManagerEventReceiver {

        @Override
        public boolean onDeleteCardSucceeded(final String cardId) {

            dismissProgressDialog();
            close(Constants.RESULT_CLOSE_REQUEST_SUCCESS, getString(R.string.card_deleted));
            return true;
        }

        @Override
        public boolean onDeleteCardFailed(final WulCard card,
                                          final String errorCode,
                                          final String errorMessage,
                                          final Exception e) {

            showErrorMessage("Delete failed [" + errorCode + "]" + errorMessage);
            return true;
        }

        @Override
        public boolean onReProvisionSucceeded(final WulCard card) {

            if (card.getCardId().equals(mCard.getCardId())) {
                refresh(card);
                Utils.showSuccessMessage(CardActivity.this,
                                         getRootView(),
                                         getString(R.string.notification_re_provision_card_profile_message));
            }
            return true;
        }

        @Override
        public boolean onReProvisionFailed(final String cardId,
                                                final String errorCode,
                                                final String errorMessage,
                                                final Exception exception) {

            showErrorMessage("Re-Provision failed [" + errorCode + "]" + errorMessage);
            return true;
        }

        @Override
        public boolean onReplenishSucceeded(final WulCard card,
                                            final int numberOfTransactionsRemaining) {

            dismissProgressDialog();
            close(RESULT_CLOSE_REQUEST_SUCCESS, getString(R.string.replenish_success));
            return true;
        }

        @Override
        public boolean onReplenishFailed(final WulCard card,
                                         final String errorCode,
                                         final String errorMessage,
                                         final Exception e) {

            showErrorMessage("Replenish failed [" + errorCode + "]" + errorMessage);
            return true;
        }

		/*
         * Card PIN wallets only below this point
		 */

        @Override
        public boolean onSetCardPinSucceeded(final WulCard card) {

            dismissProgressDialog();
            Utils.showSuccessMessage(CardActivity.this,
                                     getRootView(),
                                     "Set PIN complete");
            return true;
        }

        @Override
        public boolean onSetCardPinFailed(final WulCard card,
                                          final int triesRemaining,
                                          final String errorCode,
                                          final String errorMessage,
                                          final Exception e) {

            showErrorMessage("Set PIN failed [" + errorCode + "]" + errorMessage);
            return true;
        }

        @Override
        public boolean onChangeCardPinSucceeded(final WulCard card) {

            final int credentials = mCard.getNumberOfAvailableCredentials();
            mCardTokenView.setText(getString(R.string.token, credentials));
            Utils.showSuccessMessage(CardActivity.this,
                                     getRootView(),
                                     "Card PIN change complete");
            return true;
        }

        @Override
        public boolean onChangeCardPinFailed(final WulCard card,
                                             final int triesRemaining,
                                             final String errorCode,
                                             final String errorMessage,
                                             final Exception e) {

            showErrorMessage("Change PIN failed [" + errorCode + "]" + errorMessage);
            return true;
        }

        @Override
        public boolean onGetTaskStatusSucceeded(final String taskStatus) {

            dismissProgressDialog();
            Utils.showSuccessMessage(CardActivity.this,
                                     getRootView(),
                                     "Card Pin status: " + taskStatus);
            return true;
        }

        @Override
        public boolean onGetTaskStatusFailed(final String errorCode,
                                             final String errorMessage,
                                             final Exception e) {

            showErrorMessage("Card Pin task status failed" + " [" + errorCode + "]" + errorMessage);
            return true;
        }

        @Override
        public boolean onReProvisionStarted(final String cardId) {
            // you need to block all operations for this card till the re-provision
            // is completed.

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utils.getModalDialog(CardActivity.this,
                                         getString(R.string.re_provision_started,
                                                   cardId)).create().show();
                }
            });
            return true;
        }
    }

    private class LocalOnCardActionListener extends OnCardActionListenerImpl {

        @Override
        public void setCardAsDefault(WulCard card) {
            if (card.isContactlessSupported()) {
                getCardManager().setCardAsDefault(card, PaymentContext.CONTACTLESS);
            }
            if (card.isDsrpSupported()) {
                getCardManager().setCardAsDefault(card, PaymentContext.DSRP);
            }
            close(RESULT_CLOSE_CARD, null);
        }

        @Override
        public void activateOrSuspendCard(WulCard c) {
            CardState cardState = c.getCardState();
            if (cardState != CardState.ACTIVATED) {
                getCardManager().activateCard(c);
                close(RESULT_CLOSE_CARD, null);
            } else {
                getCardManager().suspendCard(c);
                close(RESULT_CLOSE_CARD, null);
            }
        }

        @Override
        public void replenishCredentials(WulCard c) {
            showProgressDialog();
            getCardManager().replenishCredentials(c);
        }

        @Override
        public void deleteCard(WulCard c) {
            showProgressDialog();
            getCardManager().deleteCard(c);
        }

        /*
         Card PIN only
         */
        @Override
        public void addOrChangeCardPin(WulCard c) {

            final int pinMode = (getCardManager().hasCardPin(c) ? PinActivity
                    .PIN_CONFIRM_EXISTING : PinActivity.PIN_ENTER_NEW);

            Intent i = new Intent(CardActivity.this, PinActivity.class);
            i.putExtra(EXTRA_CARD_ID, c.getCardId());
            i.putExtra(EXTRA_PIN_MODE, pinMode);
            startActivityForResult(i, REQUEST_PIN);
        }

        /*
         Card PIN only
         */
        @Override
        public void requestPinState(WulCard c) {
            if (getCardManager().requestCardPinStatus(c)) {
                showProgressDialog();
            } else {
                Utils.showErrorMessage(CardActivity.this,
                                       getRootView(),
                                       getString(R.string.card_pin_has_never_changed));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_card);
        super.onCreate(savedInstanceState);
        // intent
        Intent i = getIntent();
        final String cardId = i.getStringExtra(EXTRA_CARD_ID);
        mCard = getCardManager().findCardById(cardId);
        if (mCard == null) {
            close(Constants.RESULT_CLOSE_CARD, null);
            return;
        }

        // views
        mWalletCardView = (WalletCardView) findViewById(R.id.carddetail_card);
        mCardTokenView = (TextView) findViewById(R.id.carddetail_cardtokens);
        mCardStateView = (TextView) findViewById(R.id.carddetail_cardstate);
        mPayButton = (Button) findViewById(R.id.carddetail_pay_button);
        mPayButton.setOnClickListener(this);

        // transaction logs
        mRecyclerView = (RecyclerView) findViewById(R.id.transaction_log_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        DividerItemDecoration itemDecoration = new DividerItemDecoration(this,
                                                                         DividerItemDecoration
                                                                                 .VERTICAL);
        mRecyclerView.addItemDecoration(itemDecoration);
        mTransactionLogAdapter = new TransactionLogAdapter();
        mRecyclerView.setAdapter(mTransactionLogAdapter);
        mTransactionLogMessageView = (TextView) findViewById(R.id.transaction_log_message);
        mQrcButton = (Button) findViewById(R.id.carddetail_qrc_button);
        mQrcButton.setOnClickListener(this);

        // card manager listener
        mCardManagerReceiver.register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        WLog.d(this, "selecting card: " + mCard.getCardId());
        refresh(mCard);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mCardManagerReceiver.unregister(this);
        WLog.d(this, "deselecting card: " + mCard.getCardId());
        getCardManager().clearSelectedCard();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= 21) {
            if (!mCard.isDefaultFor(PaymentContext.CONTACTLESS) && !mCard.isDefaultFor(
                    PaymentContext
                            .DSRP)) {
                mCardStateView.setVisibility(View.GONE);
                mCardTokenView.setVisibility(View.GONE);
            } else {
                mCardTokenView.setVisibility(View.INVISIBLE);
            }
        }
        super.onBackPressed();
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent i) {
        WLog.d(this, "**** onActivityResult " + requestCode + " " + responseCode);
        refresh(mCard);
        if (responseCode == Constants.RESULT_PIN_ENTERED) {
            showProgressDialog();
        }
    }

    @Override
    public void onClick(View v) {

        if (mCard.getNumberOfAvailableCredentials() == 0) {
            Utils.showErrorMessage(this, getRootView(),
                                   getString(R.string.credentials_not_available));
            return;
        }

        if (v.equals(mPayButton)) {

            if (!mCard.isContactlessSupported()) {
                Utils.showErrorMessage(this, getRootView(),
                                       getString(R.string.contactless_not_supported));
                return;
            }

            launchActivity(TransactionActivity.class, false);
        } else if (v.equals(mQrcButton)) {

            if (!mCard.isQrcPaymentSupported()) {
                Utils.showErrorMessage(this,
                                       getRootView(),
                                       getString(R.string.qrc_not_supported));
                return;
            }
            showQrcInputDialog();
        }
    }

    private void refresh(final WulCard card) {

        mCard = card;
        getCardManager().setSelectedCard(mCard);
        // card state
        if (mCard.getCardState() == CardState.NOT_ACTIVATED) {
            mCardStateView.setText(getString(R.string.card_not_activate));
        } else if (mCard.getCardState() == CardState.SUSPENDED) {
            mCardStateView.setText(getString(R.string.card_suspended));
        } else if (mCard.isDefaultFor(PaymentContext.CONTACTLESS)) {
            mCardStateView.setText(getString(R.string.default_card));
        } else {
            mCardStateView.setText(getString(R.string.selected_card));
        }

        // card menu listener
        LocalOnCardActionListener cardActionListener = new LocalOnCardActionListener();
        mWalletCardView.setCard(mCard, cardActionListener);

        // card credentials
        final int credentials = mCard.getNumberOfAvailableCredentials();
        if (credentials == 1) {
            mCardTokenView.setText(getString(R.string.token, credentials));
        } else {
            mCardTokenView.setText(getString(R.string.tokens, credentials));
        }

        // transaction logs
        final ArrayList<TransactionLog> transactionLogs = getCardManager().getCardTransactions(
                mCard);
        mTransactionLogAdapter.setTransactionLogs(transactionLogs);
        if (transactionLogs != null && transactionLogs.size() > 0) {
            mRecyclerView.setVisibility(View.VISIBLE);
            mTransactionLogMessageView.setVisibility(View.GONE);
        } else {
            mRecyclerView.setVisibility(View.GONE);
            mTransactionLogMessageView.setVisibility(View.VISIBLE);
        }

        if (Wul.getWalletConfiguration().getUserAuthMode() == UserAuthMode.NONE) {
            mPayButton.setVisibility(View.GONE);
            mQrcButton.setVisibility(View.GONE);
        } else if (mCard.getCardState() == CardState.SUSPENDED ||
                   mCard.getCardState() == CardState.NOT_ACTIVATED) {
            mPayButton.setEnabled(false);
            mQrcButton.setEnabled(false);
        } else {
            mPayButton.setEnabled(true);
            mQrcButton.setEnabled(true);
        }
    }

    private void showErrorMessage(final String message) {
        dismissProgressDialog();
        Utils.showErrorMessage(CardActivity.this,
                               getRootView(),
                               message);
    }

    private void close(int resultCode, String message) {
        Intent intent = new Intent();
        intent.putExtra("message", message);
        setResult(resultCode, intent);
        finish();
    }

    private void launchActivity(Class activityToBeLaunched, boolean isQrc) {
        Intent i = new Intent(this, activityToBeLaunched);
        i.putExtra(EXTRA_CARD_ID, mCard.getCardId());
        i.putExtra(EXTRA_PAYMENT_ACTION, TransactionActivity.ACTION_AUTH);
        if (isQrc) {
            i.putExtra(EXTRA_QRC_AMOUNT, mQrcAmount);
            i.putExtra(EXTRA_QRC_CURRENCY, mQrcCurrency);
            i.putExtra(EXTRA_PAYMENT_CONTEXT, PaymentContext.QRC);
        } else {
            i.putExtra(EXTRA_PAYMENT_CONTEXT, PaymentContext.CONTACTLESS);
        }
        i.setFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(i, REQUEST_PRE_AUTHENTICATE);
    }


    private void showQrcInputDialog() {

        View v = LayoutInflater.from(this).inflate(R.layout.view_qrc_input, null);
        final EditText qrcAmountText = (EditText) v.findViewById(R.id.qrcAmount);
        Spinner currencySpinner = (Spinner) v.findViewById(R.id.spinner_currency);

        final Map<String, String> availableCurrencies = initSpinnerCurrencyAdapter(currencySpinner);
        final List<String> currencyCodes = new LinkedList<String>(availableCurrencies.values());

        currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {


            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mQrcCurrency = Integer.valueOf(currencyCodes.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        AlertDialog.Builder d = new AlertDialog.Builder(this);
        d.setCancelable(true).setTitle(null);
        d.setView(v);
        d.setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    mQrcAmount = Double.valueOf(qrcAmountText.getText().toString());
                } catch (NumberFormatException ex) {
                    mQrcAmount = 0D;
                }

                CardActivity.this.onDialogOkPressed();
                dialog.dismiss();
            }
        });
        d.create().show();
    }

    private void onDialogOkPressed() {
        launchActivity(QrcActivity.class, true);
    }

    private Map<String, String> initSpinnerCurrencyAdapter(Spinner currencySpinner) {

        // Get currency codes and apply to the spinner
        Map<String, String> availableCurrencies = new LinkedHashMap<String, String>();
        availableCurrencies.put("none", "0");
        availableCurrencies.put("\u00A5", "156");
        availableCurrencies.put("\u20ac", "978");
        availableCurrencies.put("\u00a3", "826");
        availableCurrencies.put("\u20B9", "356");
        availableCurrencies.put("\u0024", "840");

        final String[] spinnerArray = availableCurrencies.keySet().toArray(new String[0]);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(CardActivity.this, R.layout.spinner_item, spinnerArray);
        currencySpinner.setAdapter(adapter);
        currencySpinner.setSelection(0);
        return availableCurrencies;

    }
}
