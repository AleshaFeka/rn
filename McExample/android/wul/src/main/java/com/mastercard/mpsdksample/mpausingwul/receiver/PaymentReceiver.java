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

package com.mastercard.mpsdksample.mpausingwul.receiver;

import android.content.Intent;
import android.widget.Toast;

import com.mastercard.mchipengine.walletinterface.walletcommonenumeration.AbortReason;
import com.mastercard.mpsdk.componentinterface.PaymentContext;
import com.mastercard.mpsdk.walletusabilitylayer.exception.UnsupportedPaymentContextException;
import com.mastercard.mpsdk.walletusabilitylayer.exception.WulCardException;
import com.mastercard.mpsdk.walletusabilitylayer.log.WLog;
import com.mastercard.mpsdk.walletusabilitylayer.model.WulCard;
import com.mastercard.mpsdk.walletusabilitylayer.receiver.WalletContactlessTransactionEventReceiver;
import com.mastercard.mpsdk.walletusabilitylayer.receiver.WalletContactlessTransactionOutcomeEventReceiver;
import com.mastercard.mpsdksample.mpausingwul.R;
import com.mastercard.mpsdksample.mpausingwul.activity.TransactionActivity;
import com.mastercard.mpsdksample.mpausingwul.util.Utils;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.mastercard.mpsdk.walletusabilitylayer.log.WLog.d;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_CARD_ID;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_PAYMENT_ACTION;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_PAYMENT_CONTEXT;
import static com.mastercard.mpsdksample.mpausingwul.Constants.RESULT_ABORTED;


public class PaymentReceiver extends WalletContactlessTransactionEventReceiver {

    @Override
    public void onContactlessTransactionStarted(final WulCard card) {

        d(this, "**** [PaymentService] contactlessTransactionStarted");

        // if we have a card, launch the activity that will be handling this transaction
        Intent i = new Intent(getContext(), TransactionActivity.class);
        i.putExtra(EXTRA_CARD_ID, card.getCardId());
        i.putExtra(EXTRA_PAYMENT_CONTEXT, PaymentContext.CONTACTLESS);
        i.putExtra(EXTRA_PAYMENT_ACTION, TransactionActivity.ACTION_WAIT);
        i.setFlags(FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
    }

    public TransactionOutcomeEventReceiver getOutcomeEventReceiver() {
        return new TransactionOutcomeEventReceiver();
    }

    public class TransactionOutcomeEventReceiver extends WalletContactlessTransactionOutcomeEventReceiver {

        @Override
        public void onContactlessTransactionAborted(final WulCard card,
                                                final AbortReason abortReason,
                                                final Exception e) {

            WLog.e(this, "Transaction aborted", e);
            Utils.popToast(getContext(),
                    getContext().getString(
                            R.string.contactless_transaction_abort_message)+
                            abortReason.name() + " message: " + e.getMessage());
        }
    }
}
