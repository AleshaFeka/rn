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

package com.mastercard.mpsdksample.mpausingwul.adapter;

import android.content.Context;
import android.content.res.Resources;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mastercard.mpsdk.componentinterface.PaymentContext;
import com.mastercard.mpsdk.componentinterface.database.state.CardState;
import com.mastercard.mpsdk.walletusabilitylayer.log.WLog;
import com.mastercard.mpsdk.walletusabilitylayer.model.WulCard;
import com.mastercard.mpsdksample.mpausingwul.R;
import com.mastercard.mpsdksample.mpausingwul.view.WalletCardView;

import java.util.ArrayList;


public class CardAdapter extends RecyclerView.Adapter {

    private ArrayList<WulCard> mWulCards = new ArrayList<>();
    private OnCardClickListener mListener;

    public interface OnCardClickListener {
        void onCardClicked(View view, WulCard card);
    }

    private static class CardViewHolder extends RecyclerView.ViewHolder	{

        View parentView;
        WalletCardView cardView;
        TextView cardState;

        CardViewHolder(View v) {
            super(v);
            parentView=v;
            cardView=(WalletCardView)v.findViewById(R.id.cardholder_walletcardview);
            cardState=(TextView)v.findViewById(R.id.cardholder_cardstate);
        }
    }

    public CardAdapter() {

    }

    public void setCards(ArrayList<WulCard> cards) {
        mWulCards.clear();
        mWulCards.addAll(cards);
        refresh();
    }

    public void setListener(OnCardClickListener listener) {
        this.mListener=listener;
    }

    public WulCard getItem(int position) {
        if (mWulCards != null && position >= 0 && position < mWulCards.size()) {
            return mWulCards.get(position);
        }
        return null;
    }

    public void refresh() {
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_card, parent,
                false);
        return new CardViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, int position) {
        try{
            final WulCard card=mWulCards.get(position);
            final CardViewHolder holder=(CardViewHolder)h;
            final Context con=holder.parentView.getContext();
            final Resources res=con.getResources();
            final CardState cardState=card.getCardState();

            holder.cardView.setCard(card,null);

            if(cardState==CardState.SUSPENDED){
                holder.cardState.setText(R.string.card_suspended);
                holder.cardState.setVisibility(View.VISIBLE);
            }
            else if(card.isBeingDigitized()){
                holder.cardState.setText(R.string.digitization_in_progress);
                holder.cardState.setVisibility(View.VISIBLE);
            }
            else if(card.isDefaultFor(PaymentContext.CONTACTLESS)){
                holder.cardState.setText(R.string.default_card);
                holder.cardState.setVisibility(View.VISIBLE);
            }
            else{
                holder.cardState.setVisibility(View.GONE);
            }

            if(position==getItemCount()-1){
                holder.parentView.setPadding(
                        holder.parentView.getPaddingLeft(),
                        holder.parentView.getPaddingTop(),
                        holder.parentView.getPaddingRight(),
                        res.getDimensionPixelSize(R.dimen.default_card_padding_bottom)
                );
            }
            else{
                holder.parentView.setPadding(
                        holder.parentView.getPaddingLeft(),
                        holder.parentView.getPaddingTop(),
                        holder.parentView.getPaddingRight(),
                        0
                );
            }

            holder.parentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mListener!=null){
                        mListener.onCardClicked(v,card);
                    }
                }
            });
        }
        catch(Exception e){
            WLog.e(this,"Failed to render card adapter item",e);
        }
    }

    @Override
    public int getItemCount() {
        return mWulCards.size();
    }
}