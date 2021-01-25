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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mastercard.mpsdk.componentinterface.PaymentContext;
import com.mastercard.mpsdk.walletusabilitylayer.api.Wul;
import com.mastercard.mpsdk.walletusabilitylayer.api.WulCardManager;
import com.mastercard.mpsdk.walletusabilitylayer.log.WLog;
import com.mastercard.mpsdk.walletusabilitylayer.model.WulCard;
import com.mastercard.mpsdk.walletusabilitylayer.receiver.WalletCardManagerEventReceiver;
import com.mastercard.mpsdksample.mpausingwul.NotificationDataQueue;
import com.mastercard.mpsdksample.mpausingwul.R;
import com.mastercard.mpsdksample.mpausingwul.util.Utils;


public class CardManagerEventReceiver extends WalletCardManagerEventReceiver {

	@Override
	public boolean onProvisionSucceeded(final WulCard card) {

		WulCardManager cardManager = Wul.getCardManager();
		cardManager.removeDigitizingCard();

		// if we have only one card, make sure it is the default
		if (cardManager.getCardCount() == 1) {
			cardManager.setCardAsDefault(card, PaymentContext.CONTACTLESS);
			if(card.isDsrpSupported()) {
				cardManager.setCardAsDefault(card, PaymentContext.DSRP);
			}
		}

		showCardArrivedNotification(card);
		sendRequestCompletionBroadcastToNotifyQueue();
		return true;
	}

	@Override
	public boolean onProvisionFailed(String errorCode, String errorMessage, Exception e) {
		WLog.d(this, "**** [CardManagerEventService] onCardProvisionFailure code=" +
				errorCode + " message=" + errorMessage);
		Wul.getCardManager().removeDigitizingCard();
		Utils.showNotification(getContext(), "Card provision failed",
				errorMessage + " [code=" + errorCode + "]");
		sendRequestCompletionBroadcastToNotifyQueue(errorCode);
		return true;
	}

	@Override
	public boolean onReProvisionSucceeded(final WulCard card) {
		showCardArrivedNotification(card);
		sendRequestCompletionBroadcastToNotifyQueue();
		return true;
	}

	@Override
	public boolean onReProvisionFailed(final String cardId,
											final String errorCode,
											final String errorMessage,
											final Exception exception) {

		WLog.d(this,
			   "**** [CardManagerEventService] onCardReProvisionFailure code=" +
					 errorCode + " message=" + errorMessage);
		Utils.showNotification(getContext(), "Card provision failed",
							   errorMessage + " [code=" + errorCode + "]");
		sendRequestCompletionBroadcastToNotifyQueue(errorCode);
		return true;
	}

	@Override
	public boolean onSetWalletPinSucceeded() {
		String title = "Wallet PIN set";
		String message = "The PIN for your wallet has been set.";
		Utils.showNotification(getContext(), title, message);
		sendRequestCompletionBroadcastToNotifyQueue();
		return true;
	}

	@Override
	public boolean onChangeWalletPinSucceeded() {
		String title = "Wallet PIN has been changed";
		String message = "The PIN for your wallet has been changed.";
		Utils.showNotification(getContext(), title, message);
		sendRequestCompletionBroadcastToNotifyQueue();
		return true;
	}

	@Override
	public boolean onReplenishSucceeded(final WulCard card,
										final int numberOfTransactionCredentials) {

		System.out.println("Replesnih Completed");
		sendRequestCompletionBroadcastToNotifyQueue();
		return true;
	}

	@Override
	public boolean onReplenishFailed(final WulCard card, final String errorCode,
									 final String errorMessage,
									 final Exception exception) {

		System.out.println("Replesnih Failed");
		sendRequestCompletionBroadcastToNotifyQueue(errorCode);
		return true;
	}

	@Override
	public boolean onDeleteCardSucceeded(final String cardId) {

		sendRequestCompletionBroadcastToNotifyQueue();
		return true;
	}

	@Override
	public boolean onDeleteCardFailed(final WulCard card, final String errorCode,
									  final String errorMessage,
									  final Exception exception) {

		sendRequestCompletionBroadcastToNotifyQueue(errorCode);
		return true;
	}

	@Override
	public boolean onChangeCardPinSucceeded(final WulCard card) {

		sendRequestCompletionBroadcastToNotifyQueue();
		return true;
	}

	@Override
	public boolean onChangeCardPinFailed(final WulCard card, final int mobilePinTriesRemaining,
										 final String errorCode, final String errorMessage,
										 final Exception exception) {


		sendRequestCompletionBroadcastToNotifyQueue(errorCode);
		return true;
	}

	@Override
	public boolean onSetCardPinSucceeded(final WulCard card) {
		sendRequestCompletionBroadcastToNotifyQueue();
		return true;
	}

	@Override
	public boolean onSetCardPinFailed(final WulCard card, final int mobilePinTriesRemaining,
									  final String errorCode,
									  final String errorMessage, final Exception exception) {
		sendRequestCompletionBroadcastToNotifyQueue(errorCode);
		return true;
	}

	@Override
	public boolean onChangeWalletPinFailed(final int mobilePinTriesRemaining,
										   final String errorCode,
										   final String errorMessage, final Exception exception) {
		sendRequestCompletionBroadcastToNotifyQueue(errorCode);
		return true;
	}

	@Override
	public boolean onSetWalletPinFailed(final int mobilePinTriesRemaining, final String errorCode,
										final String errorMessage, final Exception exception) {


		sendRequestCompletionBroadcastToNotifyQueue(errorCode);
		return true;
	}

	@Override
	public boolean onGetSystemHealthSucceeded() {
		sendRequestCompletionBroadcastToNotifyQueue();
		return true;
	}

	@Override
	public boolean onGetSystemHealthFailed(final String errorCode,
										   final String errorMessage,
										   final Exception exception) {

		sendRequestCompletionBroadcastToNotifyQueue(errorCode);
		return true;
	}

	@Override
	public boolean onResetCardPinCompleted(final WulCard card) {
		Utils.showNotification(getContext(),
							   getContext().getString(R.string.pin_reset),
							   getContext().getString(R.string.card_pin_reset_description)
							   + card.getCardId());
		sendRequestCompletionBroadcastToNotifyQueue();
		return true;
	}

	@Override
	public boolean onResetWalletPinCompleted() {
		Utils.showNotification(getContext(),
							   getContext().getString(R.string.pin_reset),
							   getContext().getString(R.string.wallet_pin_reset_description));
		sendRequestCompletionBroadcastToNotifyQueue();
		return true;
	}

	@Override
	public boolean onGetTaskStatusSucceeded(final String taskStatus) {
		sendRequestCompletionBroadcastToNotifyQueue();
		return true;
	}

	@Override
	public boolean onGetTaskStatusFailed(final String errorCode, final String errorMessage,
									  final Exception exception) {
		sendRequestCompletionBroadcastToNotifyQueue(errorCode);
		return true;
	}

	@Override
	public boolean onRequestSessionSucceeded() {

		sendRequestCompletionBroadcastToNotifyQueue();
		return true;
	}

	@Override
	public boolean onRequestSessionFailed(final String errorCode, final String errorMessage,
										  final Exception exception) {
		sendRequestCompletionBroadcastToNotifyQueue(errorCode);
		return true;
	}

	@Override
	public boolean onCardProfileConfigurationMismatch(final String cardId, final String message) {

		sendRequestCompletionBroadcastToNotifyQueue();
		return true;
	}

	private void sendRequestCompletionBroadcastToNotifyQueue(){
		sendRequestCompletionBroadcastToNotifyQueue(null);
	}
	private void sendRequestCompletionBroadcastToNotifyQueue(String errorCode){
		Intent i = new Intent(NotificationDataQueue.class.getSimpleName());
		i.putExtra("ErrorCode", errorCode);
		LocalBroadcastManager.getInstance(Wul.getApplication().getApplicationContext())
							 .sendBroadcast(i);
	}

	private void showCardArrivedNotification(final WulCard card) {
		String title = "New card received";
		String message = "Your card ***-" + card.getDisplayablePanDigits() + " is now ready " +
						 "to use.";
		Utils.showNotification(getContext(), title, message);
	}
}
