/**
 * ****************************************************************************
 * Copyright (c) 2019, MasterCard International Incorporated and/or its
 * affiliates. All rights reserved.
 * <p/>
 * The contents of this file may only be used subject to the MasterCard
 * Mobile Payment SDK for MCBP and/or MasterCard Mobile MPP UI SDK
 * Materials License.
 * <p/>
 * Please refer to the file LICENSE.TXT for full details.
 * <p/>
 * TO THE EXTENT PERMITTED BY LAW, THE SOFTWARE IS PROVIDED "AS IS", WITHOUT
 * WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON INFRINGEMENT. TO THE EXTENT PERMITTED BY LAW, IN NO EVENT SHALL
 * MASTERCARD OR ITS AFFILIATES BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 * *****************************************************************************
 */

package com.mastercard.mpsdksample.mpausingwul.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.mastercard.mpsdk.componentinterface.PaymentContext;
import com.mastercard.mpsdk.componentinterface.QrcTransactionContext;
import com.mastercard.mpsdk.walletusabilitylayer.api.Wul;
import com.mastercard.mpsdk.walletusabilitylayer.config.UserAuthMode;
import com.mastercard.mpsdk.walletusabilitylayer.model.transaction.QrcTransaction;
import com.mastercard.mpsdk.walletusabilitylayer.model.transaction.QrcTransactionOutcomeResult;
import com.mastercard.mpsdksample.mpausingwul.R;
import com.mastercard.mpsdksample.mpausingwul.util.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;
import static com.mastercard.mpsdk.walletusabilitylayer.log.WLog.d;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_CARD_ID;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_PAYMENT_CONTEXT;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_PIN_MODE;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_QRC_AMOUNT;
import static com.mastercard.mpsdksample.mpausingwul.Constants.EXTRA_QRC_CURRENCY;
import static com.mastercard.mpsdksample.mpausingwul.Constants.REQUEST_PRE_AUTHENTICATE;
import static com.mastercard.mpsdksample.mpausingwul.Constants.RESULT_QRC_AUTH_SUCCESS;

public class QrcActivity extends BaseActivity {


    private ImageView mImgQrCode;
    private ImageView mPbLoading;
    private ImageView mIvBrandingLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.actvity_qrc_screen);
        super.onCreate(savedInstanceState);
        mImgQrCode = (ImageView) findViewById(R.id.iv_qr);
        mPbLoading = (ImageView) findViewById(R.id.pb_loading);
        mIvBrandingLogo = (ImageView) findViewById(R.id.iv_branding_logo);
        mPbLoading.setVisibility(View.GONE);
        launchAuth();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_QRC_AUTH_SUCCESS) {
            d(this, "**** processQrcRequest");
            new QrGeneratorTask(this).execute();
        }else{
            onBackPressed();
        }
    }


    private static class QrGeneratorTask extends AsyncTask<Void, Void, List<Bitmap>>{

       private WeakReference<QrcActivity> mActivityReference;

        QrGeneratorTask(QrcActivity activity){
            mActivityReference= new WeakReference<>(activity);
       }

        @Override
        protected void onPreExecute() {
            mActivityReference.get().startProgressBar();
        }

        @Override
        protected List<Bitmap> doInBackground(Void... voids) {

            final QrcTransactionContext qrcTransactionContext =
                    mActivityReference.get().getQrcTransactionContext();
            final QrcTransactionOutcomeResult qrcTransactionOutcomeResult =
                    Wul.getPaymentManager().processQrcTransaction(mActivityReference.get(),
                                                                  qrcTransactionContext);

            List<Bitmap> result = null;
            if (qrcTransactionOutcomeResult.getStatus() == QrcTransactionOutcomeResult.Status.OK) {
                result = new ArrayList<>();
                try {
                    String qrcBuffer =
                            new String(qrcTransactionOutcomeResult.getQrcOutput().getQrcBuffer());
                    result.add(mActivityReference.get().generateQrCode(qrcBuffer));
                } catch (WriterException e) {
                    e.printStackTrace();
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<Bitmap> result) {

            final QrcActivity qrcActivity = mActivityReference.get();
            if (result != null) {
                // The QRC will always be first in the list if we are showing it
                qrcActivity.setQrBitmap(result);
                qrcActivity.changeViewsVisibility();
                qrcActivity.scaleQrBrandingImage();
                // Barcode will be second if QRC is displayed, otherwise it will be first
            } else {
                // Something went wrong
                qrcActivity.mPbLoading.setVisibility(View.GONE);
                Utils.showErrorMessage(qrcActivity,
                                       qrcActivity.getRootView(),
                                       qrcActivity.getString(R.string.qrc_generate_error),
                                       new View.OnClickListener() {
                                           @Override
                                           public void onClick(final View v) {
                                               qrcActivity.finish();
                                           }
                                       });

            }

        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        finish();
    }

    @NonNull
    private QrcTransactionContext getQrcTransactionContext() {

        double qrcAmount =
                getIntent().getDoubleExtra(EXTRA_QRC_AMOUNT, 0);
        qrcAmount*=100;

        QrcTransaction.Builder builder=new QrcTransaction.Builder()
                .usingOptionalCurrencyCode(getIntent().getIntExtra((EXTRA_QRC_CURRENCY),0))
                .usingOptionalTransactionAmount((long)qrcAmount)
                .usingOptionalEpochTimeInSeconds(System.currentTimeMillis() / 1000)
                .usingOptionalCountryCode(315)
                .usingOptionalTransactionDate(new Date());

        return builder.build();
    }

    private void setQrBitmap(List<Bitmap> result) {
        mImgQrCode.setImageBitmap(result.get(0));
    }

    private void startProgressBar() {
        mPbLoading.setVisibility(View.VISIBLE);
        ((AnimationDrawable) (mPbLoading).getDrawable()).start();
    }

    private void changeViewsVisibility() {
        mIvBrandingLogo.setVisibility(View.VISIBLE);
        mPbLoading.setVisibility(View.GONE);
    }


    private void launchAuth() {

        final UserAuthMode authMode = Wul.getWalletConfiguration().getUserAuthMode();
        final String cardId = Wul.getCardManager().getDefaultCard(PaymentContext.CONTACTLESS).getCardId();
        if (authMode == UserAuthMode.WALLET_PIN || authMode == UserAuthMode.CARD_PIN) {

            // if using WALLET_PIN
            Intent i = new Intent(this, PinActivity.class);
            i.putExtra(EXTRA_CARD_ID, cardId);
            i.putExtra(EXTRA_PIN_MODE, PinActivity.PIN_ENTER_ONCE);
            i.putExtra(EXTRA_PAYMENT_CONTEXT, PaymentContext.QRC);

            startActivityForResult(i, REQUEST_PRE_AUTHENTICATE);

        } else if (authMode == UserAuthMode.CUSTOM) {

            // if using CUSTOM
            Intent i = new Intent(this, FingerprintActivity.class);
            i.putExtra(EXTRA_CARD_ID, cardId);
            i.putExtra(EXTRA_PAYMENT_CONTEXT, PaymentContext.CONTACTLESS);
            i.putExtra(EXTRA_PAYMENT_CONTEXT, PaymentContext.QRC);

            startActivityForResult(i, REQUEST_PRE_AUTHENTICATE);

        } else {
            // No auth to launch!
            finish();
        }
    }

    private Bitmap generateQrCode(String data) throws WriterException {
        BitMatrix result;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int QR_CODE_WIDTH = (int) ((float) (displayMetrics.widthPixels) / 100f * 80);
        try {
            result = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, QR_CODE_WIDTH,
                                                    QR_CODE_WIDTH, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }

    private void scaleQrBrandingImage() {
        Rect bounds = mImgQrCode.getDrawable().getBounds();
        int scaledWidth = (bounds.width() * 35) / 100;
        int scaledHeight = (bounds.height() * 35) / 100;

        mIvBrandingLogo.setMinimumWidth(scaledWidth);
        mIvBrandingLogo.setMinimumHeight(scaledHeight);

        mIvBrandingLogo.setMaxHeight(bounds.height() * 35/100);
        mIvBrandingLogo.setMaxWidth(bounds.width() * 35/100);
    }
}
