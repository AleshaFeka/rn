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

package com.mastercard.mpsdksample.mpausingwul.service;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;

import com.mastercard.mpsdk.walletusabilitylayer.api.Wul;
import com.mastercard.mpsdk.walletusabilitylayer.log.WLog;

public final class WalletHceService extends HostApduService {
    public WalletHceService() {
        // default empty constructor
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        WLog.d(this, "**** processCommandApdu");

        // add any other APDU handling here if required...

        // process Mastercard APDU
        return Wul.getPaymentManager().processCommandApdu(commandApdu, extras);
    }

    @Override
    public void onDeactivated(int reason) {
        WLog.d(this, "**** onDeactivated");

        // notify Mastercard of the deactivation
        Wul.getPaymentManager().onDeactivated(reason);
    }
}
