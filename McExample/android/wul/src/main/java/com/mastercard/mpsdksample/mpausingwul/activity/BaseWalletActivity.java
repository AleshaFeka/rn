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

package com.mastercard.mpsdksample.mpausingwul.activity;

import static com.mastercard.mpsdk.walletusabilitylayer.api.Wul.getCardManager;
import static com.mastercard.mpsdk.walletusabilitylayer.log.WLog.d;
import static com.mastercard.mpsdksample.mpausingwul.Constants.REQUEST_ADD_CARD;
import static com.mastercard.mpsdksample.mpausingwul.Constants.REQUEST_OPEN_CARD;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.CallSuper;
import androidx.annotation.RequiresApi;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.mastercard.mpsdk.componentinterface.PaymentContext;
import com.mastercard.mpsdk.walletusabilitylayer.api.Wul;
import com.mastercard.mpsdk.walletusabilitylayer.api.WulCardManager;
import com.mastercard.mpsdk.walletusabilitylayer.api.WulPaymentManager;
import com.mastercard.mpsdk.walletusabilitylayer.model.WulCard;
import com.mastercard.mpsdk.walletusabilitylayer.receiver.WalletCardManagerEventReceiver;
import com.mastercard.mpsdksample.mpausingwul.Constants;
import com.mastercard.mpsdksample.mpausingwul.R;
import com.mastercard.mpsdksample.mpausingwul.adapter.CardAdapter;
import com.mastercard.mpsdksample.mpausingwul.service.WalletHceService;
import com.mastercard.mpsdksample.mpausingwul.util.Utils;

import java.util.ArrayList;

/**
 * Base class for all WalletActivity classes
 */
public abstract class BaseWalletActivity extends BaseActivity
        implements View.OnClickListener, CardAdapter.OnCardClickListener {

    private TextView mMessageView;
    private RecyclerView mRecyclerView;
    private CardAdapter mCardAdapter;
    private FloatingActionButton mFloatingActionButton;
    private WalletCardManagerEventReceiver cardManagerEventReceiver = getCardManagerEventReceiver();

    protected abstract WalletCardManagerEventReceiver getCardManagerEventReceiver();

    @Override
    public void onCreate(Bundle savedInstanceState) {

        d(this, "**** onCreate");

        setContentView(R.layout.activity_wallet);
        super.onCreate(savedInstanceState);

        // transitions
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getSharedElementEnterTransition().setDuration(300);
            getWindow().getSharedElementReturnTransition().setDuration(300)
                       .setInterpolator(new DecelerateInterpolator());
        }

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowHomeEnabled(false);

        // misc views
        mMessageView = (TextView) findViewById(R.id.message_view);

        // recyclerview
        mRecyclerView = (RecyclerView) findViewById(R.id.card_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mCardAdapter = new CardAdapter();
        mCardAdapter.setListener(this);
        mRecyclerView.setAdapter(mCardAdapter);

        // fab
        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.wallet_add);
        mFloatingActionButton.setOnClickListener(this);

        // check that we are default for contactless payments
        WulPaymentManager paymentManager = Wul.getPaymentManager();
        boolean isDefault = paymentManager.isDefaultAppForContactlessPayment(this,
                                                                             WalletHceService.class);
        if (!isDefault) {
            paymentManager.showDefaultAppForPaymentDialog(this, WalletHceService.class);
        }

        // receivers
        cardManagerEventReceiver.register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getCardManager().clearSelectedCard();
        refreshCards();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cardManagerEventReceiver.unregister(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public void onClick(View view) {
        if (view.equals(mFloatingActionButton)) {
            if (Wul.getCardManager().getDigitizingCardCount() > 0) {
                Utils.showErrorMessage(this,
                                       getRootView(),
                                       getString(R.string.another_card_being_degitized));
                return;
            }
            Intent i = new Intent(this, AddCardActivity.class);
            startActivityForResult(i, REQUEST_ADD_CARD);
        }
    }

    @Override
    public void onCardClicked(View view, WulCard card) {

        if (card.isBeingDigitized()) {
            Utils.showErrorMessage(this,
                                   getRootView(),
                                   getString(R.string.digitization_in_progress));
            return;
        }

        if (Build.VERSION.SDK_INT < 21) {
            openCardActivity(card);
        } else {
            openCardActivity21(view, card);
        }
    }

    private void openCardActivity(final WulCard card) {
        Intent i = new Intent(this, CardActivity.class);
        i.putExtra(Constants.EXTRA_CARD_ID, card.getCardId());
        startActivityForResult(i, REQUEST_OPEN_CARD);
    }

    @RequiresApi(21)
    private void openCardActivity21(final View v, final WulCard card) {
        Intent i = new Intent(this, CardActivity.class);
        i.putExtra(Constants.EXTRA_CARD_ID, card.getCardId());
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, v, "card");
        startActivityForResult(i, REQUEST_OPEN_CARD, options.toBundle());
    }

    @CallSuper
    protected void refreshCards() {

        WulCardManager cardManager = Wul.getCardManager();
        ArrayList<WulCard> cards = getCardManager().getCards();

        // make sure a default is always selected
        if (cardManager.getCardCount() == 1) {
            WulCard card = cards.get(0);
            if (card.isContactlessSupported()) {
                cardManager.setCardAsDefault(card, PaymentContext.CONTACTLESS);
            }
            if (card.isDsrpSupported()) {
                cardManager.setCardAsDefault(card, PaymentContext.DSRP);
            }
        }

        mCardAdapter.setCards(cards);

        if (cards != null && cards.size() > 0) {
            mMessageView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        } else {
            mMessageView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.INVISIBLE);
        }
    }
}
