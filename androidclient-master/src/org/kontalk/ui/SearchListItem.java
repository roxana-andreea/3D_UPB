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

import org.kontalk.data.Contact;
import org.kontalk.data.SearchItem;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class SearchListItem extends RelativeLayout {
    //private static final String TAG = SearchListItem.class.getSimpleName();

    private SearchItem mFound;
    private TextView mText1;
    private TextView mText2;

    public SearchListItem(Context context) {
        super(context);
    }

    public SearchListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mText1 = (TextView) findViewById(android.R.id.text1);
        mText2 = (TextView) findViewById(android.R.id.text2);

        if (isInEditMode()) {
            mText1.setText("Test contact");
            mText2.setText("...hello buddy! How...");
        }
    }

    public final void bind(Context context, final SearchItem found) {
        mFound = found;

        final Contact contact = found.getContact();
        String name;
        if (contact != null)
            name = contact.getName() + " <" + contact.getNumber() + ">";
        else
            name = found.getUserId();

        mText1.setText(name);
        mText2.setText(found.getText());
    }

    public final void unbind() {
        // TODO unbind (?)
    }

    public SearchItem getSearchItem() {
        return mFound;
    }

}
