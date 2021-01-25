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

public class Constants {
    public static final String PREFS_DB = "sample_mpa_wul";
    public static final String PREFS_PAS_URL = "pas_url";

    public static final int REQUEST_OPEN_CARD = 1;
    public static final int REQUEST_PRE_AUTHENTICATE = 2;
    public static final int REQUEST_PIN = 3;
    public static final int REQUEST_ADD_CARD = 4;
    public static final int REQUEST_LOCK_SCREEN = 5;

    public static final int RESULT_CARD_ADDED = 1;
    public static final int RESULT_PIN_ENTERED = 2;
    public static final int RESULT_FINGERPRINT_PERMISSION_REFUSED = 3;
    public static final int RESULT_PIN_DSRP_AUTH_SUCCESS = 4;
    public static final int RESULT_AOT_COUNTER_EXPIRED = 5;
    public static final int RESULT_CANCELLED = 6;
    public static final int RESULT_CLOSE_CARD = 7;
    public static final int RESULT_NO_SELECTED_CARD = 8;
    public static final int RESULT_ABORTED = 9;
    public static final int RESULT_DECLINED = 10;
    public static final int RESULT_UNLOCK_REQUIRED = 11;
    public static final int RESULT_QRC_AUTH_SUCCESS = 12;
    public static final int RESULT_CLOSE_REQUEST_SUCCESS = 13;


    public static final String EXTRA_CARD_ID = "card_id";
    public static final String EXTRA_PAYMENT_CONTEXT = "payment_context";
    public static final String EXTRA_PAYMENT_ACTION = "payment_action";
    public static final String EXTRA_PAYMENT_DATA = "payment_data";
    public static final String EXTRA_PIN_MODE = "pin_mode";
    public static final String EXTRA_QRC_AMOUNT = "qrc_amount";
    public static final String EXTRA_QRC_CURRENCY = "qrc_currency";



}
