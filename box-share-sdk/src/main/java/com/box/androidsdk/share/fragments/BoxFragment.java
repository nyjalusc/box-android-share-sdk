package com.box.androidsdk.share.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.view.View;
import android.widget.Toast;

import com.box.androidsdk.content.BoxApiCollaboration;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.auth.BoxAuthentication;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.utils.SdkUtils;
import com.box.androidsdk.share.CollaborationUtils;
import com.box.androidsdk.share.R;
import com.box.androidsdk.share.activities.BoxActivity;
import com.box.androidsdk.share.api.BoxShareController;
import com.box.androidsdk.share.api.ShareController;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base class for Fragments in Share SDK
 * This fragment contains common code for all fragments
 */
public abstract class BoxFragment extends Fragment {

    protected BoxItem mShareItem;

    private static final int  DEFAULT_SPINNER_DELAY = 500;

    private ProgressDialog mDialog;
    private LastRunnableHandler mDialogHandler;

    protected ShareController mController;

    private Lock mSpinnerLock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mDialogHandler = new LastRunnableHandler();
        mSpinnerLock = new ReentrantLock();

        if (savedInstanceState != null && savedInstanceState.getSerializable(CollaborationUtils.EXTRA_ITEM) != null){
            mShareItem = (BoxItem)savedInstanceState.getSerializable(CollaborationUtils.EXTRA_ITEM);
        } else if (getArguments() != null) {
            Bundle args = getArguments();
            mShareItem = (BoxItem)args.getSerializable(CollaborationUtils.EXTRA_ITEM);
        }

        if (mShareItem == null){
            Toast.makeText(getActivity(), R.string.box_sharesdk_no_item_selected, Toast.LENGTH_LONG).show();
            getActivity().finish();
            return;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(CollaborationUtils.EXTRA_ITEM, mShareItem);
        super.onSaveInstanceState(outState);
    }

    public void AddResult(Intent data) {
        data.putExtra(CollaborationUtils.EXTRA_ITEM, mShareItem);
    }

    public void SetController(ShareController controller) {
        mController = controller;
    }

    /**
     * Dismisses the spinner if it is currently showing
     */
    protected void dismissSpinner(){
        mSpinnerLock.lock();
        try {
            mDialogHandler.cancelLastRunnable();
            if (mDialog != null && mDialog.isShowing()){
                mDialog.dismiss();
            }
        }
        finally {
            mSpinnerLock.unlock();
        }
    }

    /**
     * Shows the spinner with the default wait messaging
     */
    protected void showSpinner(){
        showSpinner(R.string.boxsdk_Please_wait, R.string.boxsdk_Please_wait);
    }

    /**
     * Shows the spinner with a custom title and description
     *
     * @param stringTitleRes string resource for the spinner title
     * @param stringRes string resource for the spinner description
     */
    protected void showSpinner(final int stringTitleRes, final int stringRes) {
        mDialogHandler.queue(new Runnable() {
            @Override
            public void run() {
                if (mSpinnerLock.tryLock()) {
                    try {
                        if (mDialog != null && mDialog.isShowing()) {
                            return;
                        }

                        mDialog = ProgressDialog.show(getActivity(), getText(stringTitleRes), getText(stringRes));
                        mDialog.show();
                    } catch (Exception e) {
                        // WindowManager$BadTokenException will be caught and the app would not display
                        // the 'Force Close' message
                        mDialog = null;
                        return;
                    } finally {
                        mSpinnerLock.unlock();
                    }
                }
            }
        }, DEFAULT_SPINNER_DELAY);

    }

    /**
     * Helper method to hide a view ie. set the visibility to View.GONE
     *
     * @param view the view that should be hidden
     */
    protected void hideView(View view){
        view.setVisibility(View.GONE);
    }

    /**
     * Helper method to show a view ie. set the visibility to View.VISIBLE
     *
     * @param view the view that should be shown
     */
    protected void showView(View view){
        view.setVisibility(View.VISIBLE);
    }

    public static Bundle getBundle(BoxItem boxItem) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(CollaborationUtils.EXTRA_ITEM, boxItem);
        return bundle;
    }

    /**
     * Helper class used to keep track of last runnable queued.
     */
    private class LastRunnableHandler extends Handler {
        private Runnable mLastRunable;

        public void queue(final Runnable runnable, final int delay){
            cancelLastRunnable();
            postDelayed(runnable, delay);
            mLastRunable = runnable;
        }

        public void cancelLastRunnable(){
            if (mLastRunable != null){
                removeCallbacks(mLastRunable);
            }
        }

    }

    /**
     * Helper method that returns formatted text that is meant to be shown in a Button
     *
     * @param title the title text that should be emphasized
     * @param description the description text that should be de-emphasized
     * @return Spannable that is the formatted text
     */
    protected Spannable createTitledSpannable(final String title, final String description){
        String combined = title +"\n"+ description;
        Spannable accessSpannable = new SpannableString(combined);

        accessSpannable.setSpan(new TextAppearanceSpan(getActivity(), R.style.Base_TextAppearance_AppCompat_Body1), title.length(),combined.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        accessSpannable.setSpan(new ForegroundColorSpan(R.color.box_sharesdk_hint), title.length(),combined.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return accessSpannable;
    }
}
