package com.example.xyzreader.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.app.ShareCompat;
import android.text.Html;
import android.text.PrecomputedText;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Picasso;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    //private static final float PARALLAX_FACTOR = 1.25f;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;

    private String template = "yyyy-MM-dd'T'HH:mm:ss.sss";
    //@see "https://stackoverflow.com/questions/14389349/android-get-current-locale-not-default"
    private Locale current = getCurrentLocale();
    private SimpleDateFormat dateFormat = new SimpleDateFormat(template, current);
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat(template, current);
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);
    //TypeFace to attach text views
    private Typeface rosario;

    //private CollapsingToolbarLayout mCollapsingToolbarLayout;
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Objects.requireNonNull(getArguments()).containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        //deprecated
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(Objects.requireNonNull(getActivity()))
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        //bindViews();
        return mRootView;
    }

    Locale getCurrentLocale(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            return Resources.getSystem().getConfiguration().getLocales().get(0);
        } else{
            //noinspection deprecation
            return Resources.getSystem().getConfiguration().locale;
        }
    }

    /*static float progress(float v, float min, float max) {
        return constrain((v - min) / (max - min), 0, 1);
    }

    static float constrain(float val, float min, float max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }*/

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        final TextView titleView = mRootView.findViewById(R.id.article_title);
        final TextView bylineView = mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = mRootView.findViewById(R.id.article_body);
        ImageView mPhotoView = mRootView.findViewById(R.id.photo);
        //bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));
        titleView.setTypeface(rosario);
        bylineView.setTypeface(rosario);
        bodyView.setTypeface(rosario);

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));

            }
            final TextView toolbarTitle = mRootView.findViewById(R.id.toolbar_title);
            //Setting title for toolbar
            toolbarTitle.setText(titleView.getText());
            if(Build.VERSION.SDK_INT == Build.VERSION_CODES.P){
                String longString = Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />")).toString();
                asyncSetText(bodyView, longString, Objects.requireNonNull(getActivity()).getMainExecutor());
            } else {
                bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />")));
            }
            AppBarLayout appBarLayout = mRootView.findViewById(R.id.app_bar_layout);

            appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                @Override
                public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
                    if(Math.abs(i) - appBarLayout.getTotalScrollRange() == 0){
                        toolbarTitle.setVisibility(View.VISIBLE);
                    } else {
                        toolbarTitle.setVisibility(View.GONE);
                    }
                }
            });
            String photoURL = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
            Picasso.get().load(photoURL).into(mPhotoView);
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
            bodyView.setText("N/A");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    static void asyncSetText(final TextView textView, final String longString, Executor bgExecutor){
        // construct precompute related parameters using the TextView that we will set the text on.
        final PrecomputedText.Params params = textView.getTextMetricsParams();
        final Reference textViewRef = new WeakReference<>(textView);
        bgExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Object text = textViewRef.get();
                if(text == null) return;
                final PrecomputedText precomputedText = PrecomputedText.create(longString, params);
                textView.setText(precomputedText);
            }
        });
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }
    //as recommended from android developer nanadogree Udacity course custom font adding on attach() method.
    //also course can be followed from below link
    //@see 'https://www.udacity.com/course/material-design-for-android-developers--ud862'
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(getActivity() != null)
            rosario = Typeface.createFromAsset(getActivity().getAssets(), "Rosario-Regular.ttf");
    }
}
