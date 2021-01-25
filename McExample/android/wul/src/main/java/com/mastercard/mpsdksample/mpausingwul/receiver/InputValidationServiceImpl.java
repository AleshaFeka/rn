package com.mastercard.mpsdksample.mpausingwul.receiver;

import android.content.Context;
import android.widget.Toast;

import com.mastercard.mpsdk.componentinterface.McbpLogger;
import com.mastercard.mpsdk.utils.log.McbpLoggerInstance;
import com.mastercard.mpsdksample.androidcryptoengine.InputValidationService;

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
public class InputValidationServiceImpl implements InputValidationService {

    private McbpLogger mLogger;
    private Context mContext;

    public InputValidationServiceImpl(final Context context) {
        mContext = context;
        mLogger = McbpLoggerInstance.getInstance();
    }

    @Override
    public void onValidationFailure(final String api, final String parameter) {
        Toast.makeText(mContext,
                       "Invalid Input for "+ parameter + " parameter in "+ api + " API.",
                       Toast.LENGTH_SHORT).show();
        mLogger.d("Invalid Input for %s parameter in %s API.", api, parameter);
    }
}
