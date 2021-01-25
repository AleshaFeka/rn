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

package com.mastercard.mpsdksample.mpausingwul.receiver;

import com.google.firebase.messaging.RemoteMessage;
import com.mastercard.mpsdk.walletusabilitylayer.api.Wul;
import com.mastercard.mpsdk.walletusabilitylayer.log.WLog;
import com.mastercard.mpsdk.walletusabilitylayer.receiver.WalletFcmEventReceiver;
import com.mastercard.mpsdksample.mpausingwul.BaseWulApplication;
import com.mastercard.mpsdksample.mpausingwul.util.Utils;

import java.util.Map;

public class WalletApplicationFcmReceiver extends WalletFcmEventReceiver {

    private static final String PAYLOAD_KEY = "payload";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Map<String, String> data = remoteMessage.getData();
        if (data != null && data.containsKey(PAYLOAD_KEY)) {
            // process CMS-D payload
            final String payload = data.get(PAYLOAD_KEY);
            if (payload != null && payload.length() > 0) {

                WLog.d(this, "#### onMessageReceived payload=" + payload);
                ((BaseWulApplication) Wul.getApplication())
                                                 .getNotificationDataQueue().add(payload);
            }
        }
    }


    @Override
    public void onFcmTokenReceived(final String token){

        // The FCM token must be updated to PAS whenever a fresh token is received
        WLog.d(this,"onFcmTokenReceived Token= " + token);

        // storing the token, to be sent to PAS during registration
        Utils.setFcmToken(token, Wul.getApplication());
    }
}
