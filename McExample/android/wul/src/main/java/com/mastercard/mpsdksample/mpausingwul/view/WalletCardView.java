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

package com.mastercard.mpsdksample.mpausingwul.view;

import android.content.Context;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.mastercard.mpsdk.componentinterface.CardProductType;
import com.mastercard.mpsdk.componentinterface.PaymentContext;
import com.mastercard.mpsdk.componentinterface.database.state.CardState;
import com.mastercard.mpsdk.walletusabilitylayer.api.Wul;
import com.mastercard.mpsdk.walletusabilitylayer.model.WulCard;
import com.mastercard.mpsdksample.mpausingwul.R;
import com.mastercard.mpsdksample.mpausingwul.listener.OnCardActionListener;


public class WalletCardView extends FrameLayout implements View.OnClickListener {

	private static final double CARD_HEIGHT_RATIO = 0.6321839d;

	private ImageView mBackgroundView;
	private TextView mPanView;
	private ImageView mActionMenuView;
	private View mMaskView;

	private int mPadding;
	private WulCard mCard;
	private OnCardActionListener mListener;
	private boolean mShowActions;
	private boolean mIsSuspended;
	private boolean mIsDefault;

	private int mBackgroundId = -1;
	private boolean mIsActivated;

	private class CardMenuListener implements PopupMenu.OnMenuItemClickListener {
		@Override
		public boolean onMenuItemClick(MenuItem item) {
			if (mListener != null) {
				int itemId = item.getItemId();
				if (itemId == R.id.card_menu_set_as_default) {
					mListener.setCardAsDefault(mCard);
					return true;
				} else if (itemId == R.id.card_menu_activate_or_suspend_or_unsuspend) {
					mListener.activateOrSuspendCard(mCard);
					return true;
				} else if (itemId == R.id.card_menu_add_or_change_card_pin) {
					mListener.addOrChangeCardPin(mCard);
					return true;
				} else if (itemId == R.id.card_menu_request_pin_status) {
					mListener.requestPinState(mCard);
					return true;
				} else if (itemId == R.id.card_menu_replenish) {
					mListener.replenishCredentials(mCard);
					return true;
				} else if (itemId == R.id.card_menu_delete) {
					mListener.deleteCard(mCard);
					return true;
				}
			}
			return false;
		}
	}

	public WalletCardView(@NonNull Context context) {
		this(context, null);
	}

	public WalletCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public WalletCardView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int
			defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(@NonNull final Context context) {

		mPadding = getResources().getDimensionPixelSize(R.dimen.default_card_padding);
		setPadding(mPadding, mPadding, mPadding, mPadding);

		LayoutInflater i = LayoutInflater.from(getContext());
		i.inflate(R.layout.view_card, this, true);

		mBackgroundView = (ImageView) findViewById(R.id.cardholder_background);
		mPanView = (TextView) findViewById(R.id.cardholder_pan);
		mActionMenuView = (ImageView) findViewById(R.id.cardholder_overflow);
		mMaskView = findViewById(R.id.cardholder_mask);
	}

	public void setCard(@NonNull final WulCard card, @Nullable final OnCardActionListener
			listener) {
		mCard = card;
		mListener = listener;
		mShowActions = (mListener != null);
		refresh();
	}

	private void refresh() {

		String pan = mCard.getDisplayablePanDigits();
		mBackgroundId = generateBackgroundId(pan);

		// background
		mBackgroundView.setImageResource(mBackgroundId);

		// pan
		if (pan.equals("****")) {
			mPanView.setText(getContext().getString(R.string.please_wait));
		} else {
			mPanView.setText(getContext().getString(R.string.pan,pan));
		}

		// state
		CardState cardState = mCard.getCardState();
		if (cardState == CardState.SUSPENDED) {
			mIsSuspended = true;
		} else {
			mIsSuspended = false;
		}

		if(cardState == CardState.NOT_ACTIVATED){
			mIsActivated=false;
		}else{
			mIsActivated=true;
		}

		// mask
		if (cardState == CardState.SUSPENDED ||
				mCard.isBeingDigitized() ||
				cardState == CardState.NOT_ACTIVATED) {
			mMaskView.setVisibility(View.VISIBLE);
		} else {
			mMaskView.setVisibility(View.INVISIBLE);
		}

		// menu
		mIsDefault = mCard.isDefaultFor(PaymentContext.CONTACTLESS);

		if (mShowActions) {
			mActionMenuView.setVisibility(View.VISIBLE);
			mActionMenuView.setOnClickListener(this);
		} else {
			mActionMenuView.setVisibility(View.GONE);
			mActionMenuView.setOnClickListener(null);
		}

	}

	@Override
	public void onClick(View v) {
		if (v.equals(mActionMenuView) && mShowActions) {
			PopupMenu m = new PopupMenu(v.getContext(), v);
			m.inflate(R.menu.card_menu);
			m.getMenu().findItem(R.id.card_menu_set_as_default).setVisible(!mIsDefault);

			String title=getResources().getString(R.string.suspend);
			if(!mIsActivated){
				title=getResources().getString(R.string.acticate);
			}else if(mIsSuspended){
				title=getResources().getString(R.string.unsuspend);
			}

			m.getMenu().findItem(R.id.card_menu_activate_or_suspend_or_unsuspend).setTitle(title);
			MenuItem pinItem = m.getMenu().findItem(R.id.card_menu_add_or_change_card_pin);
			if (pinItem.isVisible() && Wul.getCardManager().hasCardPin(mCard)) {
				pinItem.setTitle(R.string.menu_change_pin);
			} else {
				pinItem.setTitle(R.string.menu_set_pin);
			}
			if(mIsSuspended){
				m.getMenu().findItem(R.id.card_menu_set_as_default).setEnabled(false);
				m.getMenu().findItem(R.id.card_menu_replenish).setEnabled(false);
				m.getMenu().findItem(R.id.card_menu_request_pin_status).setEnabled(false);
			} else {
				m.getMenu().findItem(R.id.card_menu_set_as_default).setEnabled(true);
				m.getMenu().findItem(R.id.card_menu_replenish).setEnabled(true);
				m.getMenu().findItem(R.id.card_menu_delete).setEnabled(true);
				m.getMenu().findItem(R.id.card_menu_request_pin_status).setEnabled(true);
				m.getMenu().findItem(R.id.card_menu_add_or_change_card_pin).setEnabled(true);
			}
			m.setOnMenuItemClickListener(new CardMenuListener());
			m.show();
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

		int width = getWidth();
		int height = getHeight();

		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			child.measure(
					MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
					MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
			);
			child.layout(mPadding, mPadding, width - (mPadding << 1), height - (mPadding << 1));
		}
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int width = getMeasuredWidth();
		int height = getMeasuredHeight();
		if (width > 0 && height > 0) {
			int adjustedHeight = (int) ((double) width * CARD_HEIGHT_RATIO);
			//WLog.d(this,"adjustedHeight="+adjustedHeight);
			setMeasuredDimension(width, adjustedHeight);
		}
	}

	private int generateBackgroundId(final String pan) {
		int backgroundImageId = 1;
		try {
			backgroundImageId = (int) (6f * ((float) Integer.parseInt(pan) / 9999f)) + 1;
		} catch (Exception e) {
			backgroundImageId = 1;
		}
		// Preparing card background image name
		CardProductType cardProductType = CardProductType.UNKNOWN;
		if (mCard.getMcbpCard() != null){
			cardProductType = mCard.getMcbpCard().getProductType();
		}
		String cardBackgroundImage =  "cardfront_" + backgroundImageId;
		if (mCard.isContactlessSupported()){
			cardBackgroundImage = cardBackgroundImage + "_contactless";
		}
		if(cardProductType.equals(CardProductType.DEBIT) || cardProductType.equals(
				CardProductType.PREPAID)) {
			cardBackgroundImage = cardBackgroundImage + "_debit";
		}
		return getResources().getIdentifier(cardBackgroundImage, "drawable",
											getContext().getPackageName());
	}
}
