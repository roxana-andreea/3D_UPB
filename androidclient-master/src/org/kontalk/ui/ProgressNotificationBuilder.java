/*
 * Kontalk Android client
 * Copyright (C) 2014 Kontalk Devteam <devteam@kontalk.org>

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kontalk.ui;

import org.kontalk.R;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;


/**
 * Builder for progress notifications.
 * @author Daniele Ricci
 */
public class ProgressNotificationBuilder extends NotificationCompat.Builder {

    private Context mContext;
    private RemoteViews mContentView;
    private int mLayout;

    public ProgressNotificationBuilder(Context context,
            int layout,
            CharSequence tickerText,
            int smallIcon,
            PendingIntent intent) {
        super(context);

        mContext = context;
        mLayout = layout;

        setTicker(tickerText);
        // HACK this is needed otherwise notification won't be showed
        setSmallIcon(smallIcon);
        setContentIntent(intent);
        setOngoing(true);
    }

    /**
     * Updates the notification progress bar.
     * @param progress if less than 0, progress bar will be indeterminate
     */
    public ProgressNotificationBuilder progress(int progress, int contentTitle, int contentText) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            this.setSmallIcon(R.drawable.stat_notify)
                .setProgress(100, progress, false)
                .setContentTitle(mContext.getString(contentTitle))
                .setContentText(mContext.getString(contentText));

            if (progress < 0)
                setProgress(0, 0, true);
            else
                setProgress(100, progress, false);

            // not using custom content view
            mContentView = null;
        }
        else {
            mContentView = new RemoteViews(mContext.getPackageName(), mLayout);
            // this should not be needed -- contentView.setOnClickPendingIntent(R.id.progress_notification, null);
            mContentView.setTextViewText(R.id.title, mContext.getString(contentText));
            mContentView.setTextViewText(R.id.progress_text, (progress < 0) ? "" : String.format("%d%%", progress));

            if (progress < 0)
                mContentView.setProgressBar(R.id.progress_bar, 0, 0, true);
            else
                mContentView.setProgressBar(R.id.progress_bar, 100, progress, false);

            setContent(mContentView);
        }

        return this;
    }

    @Override
    public Notification build() {
        Notification no = super.build();

        if (mContentView != null) {
            /**
             * HACK working around bug #30495
             * @see https://code.google.com/p/android/issues/detail?id=30495
             */
            no.contentView = mContentView;
        }

        return no;
    }

}
