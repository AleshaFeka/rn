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

package com.mastercard.mpsdksample.mpausingwul;

import com.mastercard.mchipengine.walletinterface.walletcommonenumeration.CvmModel;
import com.mastercard.mpsdk.walletusabilitylayer.config.TransactionConsentType;
import com.mastercard.mpsdksample.androidcryptoengine.McbpCryptoEngineFactory;
import com.mastercard.mpsdk.walletusabilitylayer.api.Wul;
import com.mastercard.mpsdk.walletusabilitylayer.config.TransactionPolicy;
import com.mastercard.mpsdk.walletusabilitylayer.config.UserAuthMode;
import com.mastercard.mpsdk.walletusabilitylayer.config.WalletConfiguration;
import com.mastercard.mpsdk.walletusabilitylayer.log.WLog;
import com.mastercard.mpsdksample.mpausingwul.receiver.InputValidationServiceImpl;
import com.mastercard.mpsdksample.mpausingwul.service.WalletHceService;


public class MpaApplication extends BaseWulApplication {
    @Override
    public void onCreate() {
        WLog.d(this, "**** onCreate");
        super.onCreate();
        init();
    }

    public void init() {

        // create policies

        TransactionPolicy lvtPolicy = new TransactionPolicy(TransactionPolicy.Type.LVT)
                .allowTransactions(true)
                .numberOfTransactionsAllowedBeforeUnlock(0)
                .numberOfTransactionsAllowedBeforeAuth(2)
                .secondsAllowedSinceLastAuth(TransactionPolicy.INFINITE);

        TransactionPolicy hvtPolicy = new TransactionPolicy(TransactionPolicy.Type.HVT)
                .allowTransactions(true)
                .numberOfTransactionsAllowedBeforeUnlock(0)
                .numberOfTransactionsAllowedBeforeAuth(0)
                .secondsAllowedSinceLastAuth(TransactionPolicy.INFINITE);

        TransactionPolicy transitPolicy = new TransactionPolicy(TransactionPolicy.Type.TRANSIT)
                .allowTransactions(false);

        TransactionPolicy unknownPolicy = new TransactionPolicy(TransactionPolicy.Type.UNKNOWN)
                .copyOf(hvtPolicy);

        // create configuration

        WalletConfiguration.Builder builder = new WalletConfiguration.Builder(this)

                .withCryptoEngine(
                        new McbpCryptoEngineFactory().
                                getCryptoEngine(this, new InputValidationServiceImpl(this)))
                .withUserAuthMode(UserAuthMode.CUSTOM)
                .withCvmModel(CvmModel.FLEXIBLE_CDCVM)

                .withPolicy(lvtPolicy)
                .withPolicy(hvtPolicy)
                .withPolicy(transitPolicy)
                .withPolicy(unknownPolicy)
                .usingOptionalTransactionConsentType(TransactionConsentType.DEVICE_SCREEN_ON);


        WalletConfiguration conf = builder.build();
        Wul.init(this, conf);
    }
}