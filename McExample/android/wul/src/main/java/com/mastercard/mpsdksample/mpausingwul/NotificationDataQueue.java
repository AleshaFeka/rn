/*
 *  Copyright (c) 2019, Mastercard International Incorporated and/or its
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

package com.mastercard.mpsdksample.mpausingwul;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mastercard.mpsdk.componentinterface.remotemanagement.RemoteManagementException;
import com.mastercard.mpsdk.remotemanagement.constants.ErrorCode;
import com.mastercard.mpsdk.walletusabilitylayer.log.WLog;
import com.mastercard.mpsdk.walletusabilitylayer.manager.WulInstanceManager;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Sample implementation for holding and processing the notification data.
 */
public class NotificationDataQueue extends LinkedBlockingQueue<String> {

    private final AtomicBoolean mAlive;
    /**
     * To wait and resume queue
     */
    private Semaphore mSemaphore;

    /**
     * BroadcastReceiver to receive request completion events form other parts of application.
     */
    private class RequestCompletedEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            WLog.d(NotificationDataQueue.this, " onEventCompleted broadcast received");
            String errorCode = intent.getStringExtra("ErrorCode");
            if (errorCode == null ||
                !errorCode.equalsIgnoreCase(ErrorCode.REMOTE_MANAGEMENT_BUSY)) {
                NotificationDataQueue.this.poll();
                WLog.d(this,
                       " Queue size after poll= " +
                       NotificationDataQueue.this.size());
                releasePermit();
            }
        }
    }

    NotificationDataQueue(Context context) {
        mAlive = new AtomicBoolean(false);
        String tag = NotificationDataQueue.class.getSimpleName();
        mSemaphore = new Semaphore(1);
        LocalBroadcastManager.getInstance(context)
                             .registerReceiver(new RequestCompletedEventReceiver(),
                                               new IntentFilter(tag));
    }

    @Override
    public boolean add(String notificationData) {

        boolean isAdded = super.add(notificationData);
        WLog.d(this,
               " Adding Notification data at end, size= " +
               NotificationDataQueue.this.size());
        if (!mAlive.get()) {
            startQueueProcessingThread();
        }
        return isAdded;
    }


    private void startQueueProcessingThread() {
        new QueueProcessingThread().start();
        WLog.d(this, " Started a new QueueProcessingThread ");
    }


    private class QueueProcessingThread extends Thread {
        @Override
        public void run() {
            synchronized (mAlive) {
                if (mAlive.get()) {
                    WLog.d(this, " A QueueProcessingThread is already running");
                    return;
                }
            }
            mAlive.set(true);

            while(NotificationDataQueue.this.size() != 0){
                try {

                    // acquire a permit to execute below code
                    acquirePermit();

                    String notificationData = NotificationDataQueue.this.peek();

                    WulInstanceManager.getInstance().getMcbp()
                                      .getRemoteCommunicationManager()
                                      .processNotificationData(notificationData);

                    WLog.d(this, " ProcessNotificationData is called");

                } catch (RemoteManagementException e) {
                    WLog.d(this,
                              " Notification data could not be processed " +
                              e.getErrorCode());
                    // remove this notification form queue and release the permit.
                    NotificationDataQueue.this.poll();
                    releasePermit();
                }
            }
            WLog.d(this, " No more items to process, thread is completed ");
            mAlive.set(false);
        }
    }

    private void acquirePermit(){
        try {
            WLog.d(this,
                   " Acquiring a permit, permits left= " +
                   mSemaphore.availablePermits());
            mSemaphore.acquire();
            WLog.d(this,
                   " Acquired a permit, permits left= " +
                   mSemaphore.availablePermits());
        } catch (InterruptedException e) {
            WLog.d(this, " InterruptedException at acquiring a permit");
        }
    }

    private void releasePermit() {
        if (mSemaphore.availablePermits() == 0) {
            mSemaphore.release();
            WLog.d(this, " Released permit, permits left= "+ mSemaphore.availablePermits());
        }
    }
}