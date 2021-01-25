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

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mastercard.mpsdk.walletusabilitylayer.api.Wul;
import com.mastercard.mpsdk.walletusabilitylayer.log.WLog;
import com.mastercard.mpsdk.walletusabilitylayer.receiver.WalletCardManagerEventReceiver;
import com.mastercard.mpsdkwulcesplugin.CesPaymentAppServer;
import com.mastercard.mpsdksample.mpausingwul.Constants;
import com.mastercard.mpsdksample.mpausingwul.R;
import com.mastercard.mpsdksample.mpausingwul.util.Utils;


public class AddCardActivity extends BaseActivity implements
                                                       View.OnClickListener,
                                                       CesPaymentAppServer.AddCardCallback {

    private ViewGroup mMainLayout;
    private EditText mCardPanView;
    private EditText mCardExpiryMonthView;
    private EditText mCardExpiryYearView;
    private EditText mCardHolderNameView;
    private EditText mCardCvcView;
    private Button mAddCardButton;
    private ProgressDialog mProgressDialog;
    private TextView mMessageView;


    private class LocalWalletCardManagerEventReceiver extends WalletCardManagerEventReceiver {

        @Override
        public boolean onSetWalletPinFailed(final int mobilePinTriesRemaining, final String
                errorCode, final String errorMessage, final Exception e) {
            WLog.d(this, "**** [AddCardActivity] onSetWalletPinFailed mobilePinTriesRemaining=" +
                    mobilePinTriesRemaining + " errorCode=" + errorCode + " errorMessage=" +
                    errorMessage);
            mProgressDialog.dismiss();
            Snackbar.make(mMainLayout, "Set Wallet PIN failed\n" + errorCode + " " +
                    errorMessage, Snackbar.LENGTH_LONG);
            return true;
        }
    }

    private LocalWalletCardManagerEventReceiver localWalletCardManagerEventReceiver = new
            LocalWalletCardManagerEventReceiver();

    @Override
    public void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_add_card);
        super.onCreate(savedInstanceState);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab=getSupportActionBar();
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowHomeEnabled(false);

        // layout
        mMainLayout = (ViewGroup) findViewById(R.id.main_layout);

        // fields
        mCardPanView = (EditText) findViewById(R.id.card_pan);
        mCardExpiryMonthView = (EditText) findViewById(R.id.card_expiry_month);
        mCardExpiryYearView = (EditText) findViewById(R.id.card_expiry_year);
        mCardHolderNameView = (EditText) findViewById(R.id.card_holder_name);
        mCardCvcView = (EditText) findViewById(R.id.card_cvc);

        // button
        mAddCardButton = (Button) findViewById(R.id.add_card_button);
        mAddCardButton.setOnClickListener(this);

        // progress dialog
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage("Please wait...");

        // message
        mMessageView = (TextView) findViewById(R.id.message);

        // populate a random PAN for this demo
        mCardPanView.setText(Utils.randomizePan());

        // card manager receiver
        localWalletCardManagerEventReceiver.register(this);
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
        localWalletCardManagerEventReceiver.unregister(this);
    }

    @Override
    public void onClick(View view) {
        if (view.equals(mAddCardButton)) {

            final String pan = mCardPanView.getText().toString();
            final String expiryMonth = mCardExpiryMonthView.getText().toString();
            final String expiryYear = mCardExpiryYearView.getText().toString();
            final String cardHolderName = mCardHolderNameView.getText().toString();
            final String cvc = mCardCvcView.getText().toString();

            if (pan.length() < 9 || pan.length() > 19 || ! Utils.isValidPan(pan)) {
                mMessageView.setText("Please enter a valid PAN");
                mMessageView.setVisibility(View.VISIBLE);
                return;
            }
            mMessageView.setText("");
            mAddCardButton.setEnabled(false);

            CesPaymentAppServer ces = new CesPaymentAppServer(this, Utils.getPasUrl(this));
            ces.addCard(pan,
                    expiryMonth,
                    expiryYear,
                    cvc,
                    cardHolderName,
                    Wul.getActivationManager().getPaymentAppInstanceId(),
                    true,
                    this);

            // add a temporary card while provisioning occurs (optional)
            Wul.getCardManager().addDigitizingCard();
        }
    }

    @Override
    public void cesAddCardComplete(String message) {
        WLog.d(this, "**** cesAddCardComplete message=" + message);
        mProgressDialog.dismiss();
        close(Constants.RESULT_CARD_ADDED);
    }

    @Override
    public void cesAddCardFailed(String s, Exception e) {
        mMessageView.setText("Add Card Failed. " + s + " " + e.getMessage());
        mMessageView.setVisibility(View.VISIBLE);
        mProgressDialog.dismiss();
        mAddCardButton.setEnabled(true);
    }

    private void openPinActivity() {
        Intent i = new Intent(this, PinActivity.class);
        startActivityForResult(i, 1);
    }

    private void close(int resultCode) {
        WLog.d(this, "**** finishWithResult");
        mProgressDialog.dismiss();
        setResult(resultCode);
        finish();
    }
}