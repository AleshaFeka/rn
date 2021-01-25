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

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.ActivityOptionsCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import com.mastercard.mpsdksample.mpausingwul.R;


public class BaseActivity extends AppCompatActivity {

    private ProgressDialog mProgressDialog;
    protected ActivityOptionsCompat mActivityOptions;
    private View mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getString(R.string.please_wait));
        mProgressDialog.setCancelable(false);
        mRootView = this.findViewById(R.id.coordinatorLayout);

        mActivityOptions =
                ActivityOptionsCompat.makeCustomAnimation(this,
                                                          R.anim.activity_translate_enter_from_right,
                                                          R.anim.activity_translate_exit_soft_to_left);

    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_translate_enter_soft_from_left,
                                  R.anim.activity_translate_exit_to_right);
    }

    @Override
    public void startActivity(Intent i, Bundle b) {
        if (b == null) {
            super.startActivity(i, mActivityOptions.toBundle());
        } else {
            super.startActivity(i, b);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void startActivityForResult(Intent i, int requestCode, Bundle b) {
        if (b == null) {
            super.startActivityForResult(i, requestCode, mActivityOptions.toBundle());
        } else {
            super.startActivityForResult(i, requestCode, b);
        }
    }

    public void showProgressDialog() {
        if (mProgressDialog != null && !mProgressDialog.isShowing() && !isFinishing()) {
            try {
                mProgressDialog.show();
            } catch (Exception e) {
            }
        }
    }

    public void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            try {
                mProgressDialog.dismiss();
            } catch (Exception e) {
            }
        }
    }

    public View getRootView() {
        return mRootView;
    }
}
