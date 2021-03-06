/*
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui.adapter;


import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.ui.util.Highlighter;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;

public class KeyAdapter extends CursorAdapter {

    protected String mQuery;
    protected LayoutInflater mInflater;

    // These are the rows that we will retrieve.
    public static final String[] PROJECTION = new String[]{
            KeyRings._ID,
            KeyRings.MASTER_KEY_ID,
            KeyRings.USER_ID,
            KeyRings.IS_REVOKED,
            KeyRings.IS_EXPIRED,
            KeyRings.VERIFIED,
            KeyRings.HAS_ANY_SECRET,
            KeyRings.HAS_DUPLICATE_USER_ID,
            KeyRings.HAS_ENCRYPT,
            KeyRings.FINGERPRINT,
            KeyRings.CREATION,
    };

    public static final int INDEX_MASTER_KEY_ID = 1;
    public static final int INDEX_USER_ID = 2;
    public static final int INDEX_IS_REVOKED = 3;
    public static final int INDEX_IS_EXPIRED = 4;
    public static final int INDEX_VERIFIED = 5;
    public static final int INDEX_HAS_ANY_SECRET = 6;
    public static final int INDEX_HAS_DUPLICATE_USER_ID = 7;
    public static final int INDEX_HAS_ENCRYPT = 8;
    public static final int INDEX_FINGERPRINT = 9;
    public static final int INDEX_CREATION = 10;

    public KeyAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);

        mInflater = LayoutInflater.from(context);
    }

    public void setSearchQuery(String query) {
        mQuery = query;
    }

    public static class KeyItemViewHolder {
        public Long mMasterKeyId;
        public TextView mMainUserId;
        public TextView mMainUserIdRest;
        public TextView mCreationDate;
        public ImageView mStatus;
        public View mSlinger;
        public ImageButton mSlingerButton;

        public KeyItemViewHolder(View view) {
            mMainUserId = (TextView) view.findViewById(R.id.key_list_item_name);
            mMainUserIdRest = (TextView) view.findViewById(R.id.key_list_item_email);
            mStatus = (ImageView) view.findViewById(R.id.key_list_item_status_icon);
            mSlinger = view.findViewById(R.id.key_list_item_slinger_view);
            mSlingerButton = (ImageButton) view.findViewById(R.id.key_list_item_slinger_button);
            mCreationDate = (TextView) view.findViewById(R.id.key_list_item_creation);
        }

        public void setData(Context context, Cursor cursor, Highlighter highlighter) {

            { // set name and stuff, common to both key types
                String userId = cursor.getString(INDEX_USER_ID);
                KeyRing.UserId userIdSplit = KeyRing.splitUserId(userId);
                if (userIdSplit.name != null) {
                    mMainUserId.setText(highlighter.highlight(userIdSplit.name));
                } else {
                    mMainUserId.setText(R.string.user_id_no_name);
                }
                if (userIdSplit.email != null) {
                    mMainUserIdRest.setText(highlighter.highlight(userIdSplit.email));
                    mMainUserIdRest.setVisibility(View.VISIBLE);
                } else {
                    mMainUserIdRest.setVisibility(View.GONE);
                }
            }

            { // set edit button and status, specific by key type

                long masterKeyId = cursor.getLong(INDEX_MASTER_KEY_ID);
                boolean isSecret = cursor.getInt(INDEX_HAS_ANY_SECRET) != 0;
                boolean isRevoked = cursor.getInt(INDEX_IS_REVOKED) > 0;
                boolean isExpired = cursor.getInt(INDEX_IS_EXPIRED) != 0;
                boolean isVerified = cursor.getInt(INDEX_VERIFIED) > 0;
                boolean hasDuplicate = cursor.getInt(INDEX_HAS_DUPLICATE_USER_ID) != 0;

                mMasterKeyId = masterKeyId;

                // Note: order is important!
                if (isRevoked) {
                    KeyFormattingUtils
                            .setStatusImage(context, mStatus, null, State.REVOKED, R.color.bg_gray);
                    mStatus.setVisibility(View.VISIBLE);
                    mSlinger.setVisibility(View.GONE);
                    mMainUserId.setTextColor(context.getResources().getColor(R.color.bg_gray));
                    mMainUserIdRest.setTextColor(context.getResources().getColor(R.color.bg_gray));
                } else if (isExpired) {
                    KeyFormattingUtils.setStatusImage(context, mStatus, null, State.EXPIRED, R.color.bg_gray);
                    mStatus.setVisibility(View.VISIBLE);
                    mSlinger.setVisibility(View.GONE);
                    mMainUserId.setTextColor(context.getResources().getColor(R.color.bg_gray));
                    mMainUserIdRest.setTextColor(context.getResources().getColor(R.color.bg_gray));
                } else if (isSecret) {
                    mStatus.setVisibility(View.GONE);
                    if (mSlingerButton.hasOnClickListeners()) {
                        mSlinger.setVisibility(View.VISIBLE);
                    } else {
                        mSlinger.setVisibility(View.GONE);
                    }
                    mMainUserId.setTextColor(context.getResources().getColor(R.color.black));
                    mMainUserIdRest.setTextColor(context.getResources().getColor(R.color.black));
                } else {
                    // this is a public key - show if it's verified
                    if (isVerified) {
                        KeyFormattingUtils.setStatusImage(context, mStatus, State.VERIFIED);
                        mStatus.setVisibility(View.VISIBLE);
                    } else {
                        KeyFormattingUtils.setStatusImage(context, mStatus, State.UNVERIFIED);
                        mStatus.setVisibility(View.VISIBLE);
                    }
                    mSlinger.setVisibility(View.GONE);
                    mMainUserId.setTextColor(context.getResources().getColor(R.color.black));
                    mMainUserIdRest.setTextColor(context.getResources().getColor(R.color.black));
                }

                if (hasDuplicate) {
                    String dateTime = DateUtils.formatDateTime(context,
                            cursor.getLong(INDEX_CREATION) * 1000,
                            DateUtils.FORMAT_SHOW_DATE
                                    | DateUtils.FORMAT_SHOW_YEAR
                                    | DateUtils.FORMAT_ABBREV_MONTH);

                    mCreationDate.setText(context.getString(R.string.label_key_created,
                            dateTime));
                    mCreationDate.setVisibility(View.VISIBLE);
                } else {
                    mCreationDate.setVisibility(View.GONE);
                }

            }

        }

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.key_list_item, parent, false);
        KeyItemViewHolder holder = new KeyItemViewHolder(view);
        view.setTag(holder);
        holder.mSlingerButton.setColorFilter(context.getResources().getColor(R.color.tertiary_text_light),
                PorterDuff.Mode.SRC_IN);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Highlighter highlighter = new Highlighter(context, mQuery);
        KeyItemViewHolder h = (KeyItemViewHolder) view.getTag();
        h.setData(context, cursor, highlighter);
    }

    public boolean isSecretAvailable(int id) {
        if (!mCursor.moveToPosition(id)) {
            throw new IllegalStateException("couldn't move cursor to position " + id);
        }

        return mCursor.getInt(INDEX_HAS_ANY_SECRET) != 0;
    }

    public long getMasterKeyId(int id) {
        if (!mCursor.moveToPosition(id)) {
            throw new IllegalStateException("couldn't move cursor to position " + id);
        }

        return mCursor.getLong(INDEX_MASTER_KEY_ID);
    }

    @Override
    public KeyItem getItem(int position) {
        Cursor c = getCursor();
        if (c.isClosed() || !c.moveToPosition(position)) {
            return null;
        }
        return new KeyItem(c);
    }

    @Override
    public long getItemId(int position) {
        // prevent a crash on rapid cursor changes
        if (getCursor().isClosed()) {
            return 0L;
        }
        return super.getItemId(position);
    }

    // must be serializable for TokenCompleTextView state
    public static class KeyItem implements Serializable {

        public final String mUserIdFull;
        public final KeyRing.UserId mUserId;
        public final long mKeyId;
        public final boolean mHasDuplicate;
        public final Date mCreation;
        public final String mFingerprint;

        private KeyItem(Cursor cursor) {
            String userId = cursor.getString(INDEX_USER_ID);
            mUserId = KeyRing.splitUserId(userId);
            mUserIdFull = userId;
            mKeyId = cursor.getLong(INDEX_MASTER_KEY_ID);
            mHasDuplicate = cursor.getLong(INDEX_HAS_DUPLICATE_USER_ID) > 0;
            mCreation = new Date(cursor.getLong(INDEX_CREATION) * 1000);
            mFingerprint = KeyFormattingUtils.convertFingerprintToHex(
                    cursor.getBlob(INDEX_FINGERPRINT));
        }

        public KeyItem(CanonicalizedPublicKeyRing ring) {
            CanonicalizedPublicKey key = ring.getPublicKey();
            String userId = key.getPrimaryUserIdWithFallback();
            mUserId = KeyRing.splitUserId(userId);
            mUserIdFull = userId;
            mKeyId = ring.getMasterKeyId();
            mHasDuplicate = false;
            mCreation = key.getCreationTime();
            mFingerprint = KeyFormattingUtils.convertFingerprintToHex(
                    ring.getFingerprint());
        }

        public String getReadableName() {
            if (mUserId.name != null) {
                return mUserId.name;
            } else {
                return mUserId.email;
            }
        }

    }

}
