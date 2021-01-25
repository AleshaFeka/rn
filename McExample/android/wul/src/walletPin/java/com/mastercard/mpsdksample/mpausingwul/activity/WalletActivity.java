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

import android.content.DialogInterface;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import android.view.MenuItem;

import com.mastercard.mpsdk.walletusabilitylayer.api.Wul;
import com.mastercard.mpsdk.walletusabilitylayer.log.WLog;
import com.mastercard.mpsdk.walletusabilitylayer.model.WulCard;
import com.mastercard.mpsdk.walletusabilitylayer.receiver.WalletCardManagerEventReceiver;
import com.mastercard.mpsdksample.mpausingwul.BuildConfig;
import com.mastercard.mpsdksample.mpausingwul.Constants;
import com.mastercard.mpsdksample.mpausingwul.R;
import com.mastercard.mpsdksample.mpausingwul.util.Utils;

import static com.mastercard.mpsdk.walletusabilitylayer.api.Wul.getCardManager;
import static com.mastercard.mpsdk.walletusabilitylayer.log.WLog.d;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_PIN_MODE;
import static com.mastercard.mpsdksample.mpausingwul.Constants.REQUEST_PIN;


public class WalletActivity extends BaseWalletActivity {

    protected WalletCardManagerEventReceiver getCardManagerEventReceiver() {

        return new WalletCardManagerEventReceiver() {

            @Override
            public boolean onProvisionSucceeded(final WulCard card) {
                d(this,
                  "**** [WalletActivity] onProvisionSucceeded " + card.getCardId());
                getCardManager().activateCard(card);
                refreshCards();
                if (!getCardManager().hasWalletPin()) {
                    openWalletPinDialog();
                }
                return true;
            }

            @Override
            public boolean onReProvisionSucceeded(final WulCard card) {
                d(this,
                  "**** [WalletActivity] onReProvisionSucceeded " + card.getCardId());
                refreshCards();
                return true;
            }

            @Override
            public boolean onChangeWalletPinSucceeded() {
                d(this, "**** onChangeWalletPinSucceeded");
                dismissProgressDialog();
                Utils.showSuccessMessage(WalletActivity.this,
                        getRootView(),
                        getString(R.string.pin_changed_success));
                refreshCards();
                return true;
            }

            @Override
            public boolean onSetWalletPinSucceeded() {
                d(this, "**** onSetWalletPinSucceeded");
                dismissProgressDialog();
                Utils.showSuccessMessage(WalletActivity.this,
                        getRootView(),
                        getString(R.string.pin_set_success));
                refreshCards();
                return true;
            }

            @Override
            public boolean onChangeWalletPinFailed(final int triesRemaining, final String errorCode,
                                                   final String errorMessage, final Exception e) {
                dismissProgressDialog();
                refreshCards();
                Utils.showErrorMessage(WalletActivity.this,
                        getRootView(),
                        "Change PIN failed, " + errorMessage);
                return true;
            }

            @Override
            public boolean onSetWalletPinFailed(final int triesRemaining, final String errorCode,
                                                final String errorMessage, final Exception e) {
                dismissProgressDialog();
                refreshCards();
                Utils.showErrorMessage(WalletActivity.this,
                        getRootView(),
                        "Set PIN failed, " + errorMessage);
                return true;
            }

            @Override
            public boolean onReplenishSucceeded(final WulCard card, final int
                    numberOfTransactionCredentials) {
                d(this, "**** [WalletActivity] onReplenishSucceeded " + card.getCardId() + " " +
                        numberOfTransactionCredentials);
                dismissProgressDialog();
                refreshCards();
                return true;
            }

            @Override
            public boolean onReplenishFailed(final WulCard card, final String errorCode, final String

                    errorMessage, Exception e) {
                d(this, "**** [WalletActivity] onReplenishFailed " + card.getCardId() + ": " +
                        errorMessage + " [" + errorCode + "]");
                dismissProgressDialog();
                refreshCards();
                Utils.showErrorMessage(WalletActivity.this,
                        getRootView(),
                        "Replenish failed: " + errorMessage);
                return true;
            }

            @Override
            public boolean onCardProfileConfigurationMismatch(final String cardId, String message) {

                getCardManager().removeDigitizingCard();
                WLog.e(this, "**** onCardProfileConfigurationMismatch cardId= " + cardId + " errorMessage= " +
                        message, new Exception(message));
                //take necessary action if your wallet configuration does not match with card profile configuration
                getCardManager().suspendCard(getCardManager().findCardById(cardId));
                if (!BuildConfig.DEBUG) {
                    Utils.showErrorMessage(WalletActivity.this, getRootView(), message);
                }
                refreshCards();
                return true;
            }

            @Override
            public boolean onReProvisionStarted(final String cardId) {
                // you need to block all operations for this card till the re-provision
                // is completed.

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.getModalDialog(WalletActivity.this,
                                             getString(R.string.re_provision_started,
                                                       cardId)).create().show();
                    }
                });
                return true;
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.main_menu_set_or_change_wallet_pin) {
            openPinActivity();
            return true;
        } else if (id == R.id.main_menu_request_pin_status) {
            requestPinStatus();
            return true;
        } else if (id == R.id.main_menu_reset) {
            Utils.resetAppAndRelaunch(this);
            return true;
        } else if (id == R.id.main_menu_refresh_cards) {
            refreshCards();
            return true;
        } else if (id == R.id.main_menu_about) {
            Utils.showAboutDialog(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent i) {
        d(this, "**** onActivityResult " + requestCode + " " + responseCode + " " + i);
        if (responseCode == Constants.RESULT_PIN_ENTERED) {
            // wait for a PIN completed event
            showProgressDialog();
        } else if (responseCode == Constants.RESULT_AOT_COUNTER_EXPIRED) {
            getCardManager().clearSelectedCard();
        }else if(responseCode == Constants.RESULT_CLOSE_REQUEST_SUCCESS){
            Utils.showSuccessMessage(this,getRootView(),i.getStringExtra("message"));
        }
        refreshCards();
    }

    private void openWalletPinDialog() {
        AlertDialog.Builder b = Utils.getModalDialog(this, "You need to set a PIN for this wallet before you can use the card.");
        b.setPositiveButton("Set PIN", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                openPinActivity();
            }
        });
        b.create().show();
    }

    private void openPinActivity() {

        int pinMode = (getCardManager().hasWalletPin() ? PinActivity.PIN_CONFIRM_EXISTING : PinActivity.PIN_ENTER_NEW);

        Intent i = new Intent(this, PinActivity.class);
        i.putExtra(EXTRA_PIN_MODE, pinMode);
        startActivityForResult(i, REQUEST_PIN);
    }

    private void requestPinStatus() {
        boolean requestStarted = Wul.getCardManager().requestWalletPinStatus();
        if (requestStarted) {
            dismissProgressDialog();
        } else {
            Utils.showErrorMessage(WalletActivity.this,
                    getRootView(),
                    getString(R.string.pin_never_changed));
        }
    }
}

