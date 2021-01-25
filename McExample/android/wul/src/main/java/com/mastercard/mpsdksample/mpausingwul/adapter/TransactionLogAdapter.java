/*******************************************************************************
 * Copyright (c) 2019, MasterCard International Incorporated and/or its affiliates. All rights
 * reserved.
 *
 * The contents of this file may only be used subject to the MasterCard Mobile Payment SDK for
 * MCBP and/or MasterCard Mobile MPP UI SDK Materials License.
 *
 * Please refer to the file LICENSE.TXT for full details.
 *
 * TO THE EXTENT PERMITTED BY LAW, THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. TO THE EXTENT PERMITTED BY LAW, IN NO
 * EVENT SHALL MASTERCARD OR ITS AFFILIATES BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/

package com.mastercard.mpsdksample.mpausingwul.adapter;

import android.graphics.Color;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mastercard.mpsdk.componentinterface.database.TransactionLog;
import com.mastercard.mpsdk.implementation.TransactionLogImpl;
import com.mastercard.mpsdk.walletusabilitylayer.log.WLog;
import com.mastercard.mpsdk.walletusabilitylayer.model.transaction.PaymentData;
import com.mastercard.mpsdksample.mpausingwul.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class TransactionLogAdapter extends RecyclerView.Adapter {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy", Locale
            .ENGLISH);
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm a", Locale
            .ENGLISH);

    private ArrayList<TransactionLog> mTransactionLogs = new ArrayList<>();

    private static class TransactionHistoryViewHolder extends RecyclerView.ViewHolder {
        TextView logDateView;
        TextView logTimeView;
        TextView logAmountView;
        TextView logTransactionType;

        TransactionHistoryViewHolder(View v) {
            super(v);
            logDateView = (TextView) v.findViewById(R.id.transactionlog_date);
            logTimeView = (TextView) v.findViewById(R.id.transactionlog_time);
            logAmountView = (TextView) v.findViewById(R.id.transactionlog_amount);
            logTransactionType = (TextView) v.findViewById(R.id.transactionlog_type);
        }
    }

    public TransactionLogAdapter() {

    }

    public void setTransactionLogs(ArrayList<TransactionLog> transactionLogs) {
        this.mTransactionLogs = transactionLogs;
        refresh();
    }

    public TransactionLog getItem(int position) {
        if (mTransactionLogs != null && position >= 0 && position < mTransactionLogs.size()) {
            return mTransactionLogs.get(position);
        }
        return null;
    }

    public void refresh() {
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout
                                                                          .holder_transaction_log,
                                                                  parent, false);
        return new TransactionHistoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, int position) {
        final TransactionLog log = mTransactionLogs.get(position);
        final TransactionHistoryViewHolder holder = (TransactionHistoryViewHolder) h;

        try {
            // date/time
            Date d = new Date(log.getDate());
            holder.logDateView.setText(dateFormat.format(d));
            holder.logTimeView.setText(timeFormat.format(d));

            // amount
            final String amount = PaymentData.parseAmount(String.valueOf(log.getAmount()), String
                    .valueOf(log.getCurrencyCode()));
            if (amount != null && amount.length() > 0) {
                holder.logAmountView.setText(amount);
            } else {
                holder.logAmountView.setText("--");
            }

            setTransactionType(log, holder.logTransactionType);
        } catch (Exception e) {
            WLog.e(this, "Failed to render transaction history", e);
        }
    }

    private void setTransactionType(final TransactionLog log,
                                    final TextView logTransactionType) {
        switch (log.getCryptogramFormat()) {

            case TransactionLogImpl.FORMAT_MCHIP:
                logTransactionType.setText("Mchip");
                break;
            case TransactionLogImpl.FORMAT_MAGSTRIPE:
                logTransactionType.setText("Magstripe");
                break;
            case TransactionLogImpl.FORMAT_PPMC_DSRP_DE55:
                logTransactionType.setText("Remote");
                break;
            case TransactionLogImpl.FORMAT_PPMC_DSRP_UCAF:
                logTransactionType.setText("Remote");
                break;
            case TransactionLogImpl.FORMAT_FAILED:
                logTransactionType.setText("Failed");
                logTransactionType.setTextColor(Color.RED);
                break;
            case TransactionLogImpl.FORMAT_QRC:
                logTransactionType.setText("QRC");
                break;
            default:
                logTransactionType.setText("Canceled");
                logTransactionType.setTextColor(Color.RED);
        }
    }

    @Override
    public int getItemCount() {
        if (mTransactionLogs != null) {
            return mTransactionLogs.size();
        }
        return 0;
    }


}