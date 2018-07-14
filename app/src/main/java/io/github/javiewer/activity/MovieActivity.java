package io.github.javiewer.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.robertlevonyan.views.chip.Chip;
import com.wefika.flowlayout.FlowLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.jzvd.JZVideoPlayer;
import cn.jzvd.JZVideoPlayerStandard;
import io.github.javiewer.JAViewer;
import io.github.javiewer.R;
import io.github.javiewer.adapter.ActressPaletteAdapter;
import io.github.javiewer.adapter.MovieHeaderAdapter;
import io.github.javiewer.adapter.ScreenshotAdapter;
import io.github.javiewer.adapter.item.Genre;
import io.github.javiewer.adapter.item.Movie;
import io.github.javiewer.adapter.item.MovieDetail;
import io.github.javiewer.network.Avgle;
import io.github.javiewer.network.item.AvgleSearchResult;
import io.github.javiewer.network.provider.AVMOProvider;
import io.github.javiewer.util.SimpleVideoPlayer;
import io.github.javiewer.view.ViewUtil;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieActivity extends AppCompatActivity {

    public Movie movie;
    public AvgleSearchResult.Response.Video video = null;

    @BindView(R.id.toolbar_layout)
    CollapsingToolbarLayout mToolbarLayout;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.toolbar_layout_background)
    ImageView mToolbarLayoutBackground;

    @BindView(R.id.movie_content)
    NestedScrollView mContent;

    @BindView(R.id.movie_progress_bar)
    ProgressBar mProgressBar;

    @BindView(R.id.fab)
    FloatingActionButton mFab;

    @BindView(R.id.genre_flow_layout)
    FlowLayout mFlowLayout;

    @BindView(R.id.text_preview)
    TextView mTextPreview;

    MenuItem mStarButton;

    @Override
    @SuppressWarnings("ConstantConditions")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie);

        ButterKnife.bind(this);

        Bundle bundle = this.getIntent().getExtras();
        movie = (Movie) bundle.getSerializable("movie");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(movie.title);

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MovieActivity.this, DownloadActivity.class);
                Bundle arguments = new Bundle();
                arguments.putString("keyword", movie.getCode());
                intent.putExtras(arguments);
                startActivity(intent);
            }
        });
        mFab.bringToFront();

        Call<ResponseBody> call = JAViewer.SERVICE.get(this.movie.getLink());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (!response.isSuccessful()) {
                    return;
                }

                MovieDetail detail;
                try {
                    detail = AVMOProvider.parseMoviesDetail(response.body().string());
                    detail.headers.add(0, MovieDetail.Header.create("影片名", movie.getTitle(), null));
                    displayInfo(detail);

                    Glide.with(mToolbarLayoutBackground.getContext().getApplicationContext())
                            .load(detail.coverUrl)
                            .into(mToolbarLayoutBackground);
                } catch (IOException e) {
                    onFailure(call, e);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void displayInfo(MovieDetail detail) {
        //Info
        {
            RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.headers_recycler_view);
            ImageView mIcon = (ImageView) findViewById(R.id.movie_icon_header);

            if (detail.headers.isEmpty()) {
                TextView mText = (TextView) findViewById(R.id.header_empty_text);
                mRecyclerView.setVisibility(View.GONE);
                mText.setVisibility(View.VISIBLE);
                ViewUtil.alignIconToView(mIcon, mText);
            } else {
                mRecyclerView.setAdapter(new MovieHeaderAdapter(detail.headers, this, mIcon));
                mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                mRecyclerView.setNestedScrollingEnabled(false);
            }
        }

        //Screenshots
        {
            RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.screenshots_recycler_view);
            ImageView mIcon = (ImageView) findViewById(R.id.movie_icon_screenshots);

            if (detail.screenshots.isEmpty()) {
                TextView mText = (TextView) findViewById(R.id.screenshots_empty_text);
                mRecyclerView.setVisibility(View.GONE);
                mText.setVisibility(View.VISIBLE);
                ViewUtil.alignIconToView(mIcon, mText);
            } else {
                mRecyclerView.setAdapter(new ScreenshotAdapter(detail.screenshots, this, mIcon, movie));
                mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL));
                mRecyclerView.setNestedScrollingEnabled(false);
            }
        }

        //Actress
        {
            RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.actresses_recycler_view);
            ImageView mIcon = (ImageView) findViewById(R.id.movie_icon_actresses);

            if (detail.actresses.isEmpty()) {
                TextView mText = (TextView) findViewById(R.id.actresses_empty_text);
                mRecyclerView.setVisibility(View.GONE);
                mText.setVisibility(View.VISIBLE);
                ViewUtil.alignIconToView(mIcon, mText);
            } else {
                mRecyclerView.setAdapter(new ActressPaletteAdapter(detail.actresses, this, mIcon));
                mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                mRecyclerView.setNestedScrollingEnabled(false);
            }
        }

        //Genre
        {
            ImageView mIcon = (ImageView) findViewById(R.id.movie_icon_genre);

            if (detail.genres.isEmpty()) {
                mFlowLayout.setVisibility(View.GONE);
                TextView mText = (TextView) findViewById(R.id.genre_empty_text);
                mText.setVisibility(View.VISIBLE);
                ViewUtil.alignIconToView(mIcon, mText);
            } else {
                for (int i = 0; i < detail.genres.size(); i++) {
                    final Genre genre = detail.genres.get(i);
                    View view = getLayoutInflater().inflate(R.layout.chip_genre, mFlowLayout, false);
                    Chip chip = (Chip) view.findViewById(R.id.chip_genre);
                    chip.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (genre.getLink() != null) {
                                startActivity(MovieListActivity.newIntent(MovieActivity.this, genre.getName(), genre.getLink()));
                            }
                        }
                    });
                    chip.setChipText(genre.getName());
                    mFlowLayout.addView(view);

                    if (i == 0) {
                        ViewUtil.alignIconToView(mIcon, view);
                    }
                }
            }
        }

        //Preview
        {
            /*mTextPreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    JZVideoPlayerStandard.startFullscreen(MovieActivity.this,
                            JZVideoPlayerStandard.class,
                            "https://static-clst.avgle.com/videos/tmb5/185025/preview.mp4",
                            movie.title);
                }
            });*/
        }

        //Changing visibility
        mProgressBar.animate().setDuration(200).alpha(0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mProgressBar.setVisibility(View.GONE);
            }
        }).start();

        //Slide Up Animation
        mContent.setVisibility(View.VISIBLE);
        mContent.setY(mContent.getY() + 120);
        mContent.setAlpha(0);
        mContent.animate().translationY(0).alpha(1).setDuration(500).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.movie, menu);

        mStarButton = menu.findItem(R.id.action_star);
        {
            if (JAViewer.CONFIGURATIONS.getStarredMovies().contains(movie)) {
                mStarButton.setIcon(R.drawable.ic_menu_star);
                mStarButton.setTitle("取消收藏");
            }
        }
        mStarButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (JAViewer.CONFIGURATIONS.getStarredMovies().contains(movie)) {
                    JAViewer.CONFIGURATIONS.getStarredMovies().remove(movie);
                    mStarButton.setIcon(R.drawable.ic_menu_star_border);
                    Snackbar.make(mContent, "已取消收藏", Snackbar.LENGTH_LONG).show();
                    mStarButton.setTitle("收藏");
                } else {
                    List<Movie> movies = JAViewer.CONFIGURATIONS.getStarredMovies();
                    Collections.reverse(movies);
                    movies.add(movie);
                    Collections.reverse(movies);
                    mStarButton.setIcon(R.drawable.ic_menu_star);
                    Snackbar.make(mContent, "已收藏", Snackbar.LENGTH_LONG).show();
                    mStarButton.setTitle("取消收藏");
                }
                JAViewer.CONFIGURATIONS.save();
                FavouriteActivity.update();
                return true;
            }
        });

        final MenuItem mShareButton = menu.findItem(R.id.action_share);
        mShareButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                try {

                    File cache = new File(getExternalFilesDir("cache"), "screenshot");

                    //Generate screenshot
                    FileOutputStream os = new FileOutputStream(cache);
                    Bitmap screenshot = getScreenBitmap();
                    screenshot.compress(Bitmap.CompressFormat.JPEG, 100, os);
                    os.flush();
                    os.close();

                    Uri uri = FileProvider.getUriForFile(getApplicationContext(), "io.github.javiewer.fileprovider", cache);
                    // Uri uri = Uri.fromFile(cache);
                    Intent intent = new Intent(Intent.ACTION_SEND)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            .setType("image/jpeg")
                            .putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(intent, "分享此影片"));

                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MovieActivity.this, "无法分享：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    public Bitmap getScreenBitmap() {
        int imageHeight = mToolbarLayoutBackground.getHeight();
        int scrollViewHeight = 0;
        for (int i = 0; i < mContent.getChildCount(); i++) {
            scrollViewHeight += mContent.getChildAt(i).getHeight();
        }
        Bitmap result = Bitmap.createBitmap(mContent.getWidth(), imageHeight + scrollViewHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawColor(Color.parseColor("#FAFAFA"));

        //Image
        {
            Bitmap bitmap = Bitmap.createBitmap(mToolbarLayoutBackground.getWidth(), imageHeight, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bitmap);
            mToolbarLayoutBackground.draw(c);
            canvas.drawBitmap(bitmap, 0, 0, null);
        }

        //ScrollView
        {
            Bitmap bitmap = Bitmap.createBitmap(mContent.getWidth(), scrollViewHeight, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bitmap);
            mContent.draw(c);
            canvas.drawBitmap(bitmap, 0, imageHeight, null);
        }

        return result;
    }

    @OnClick(R.id.text_preview)
    public void onClickPreview() {
        //TODO: Deprecated
        final ProgressDialog dialog = ProgressDialog.show(this, "请稍后", "正在搜索该影片的预览视频", true, false);

        if (video != null) {
            JZVideoPlayerStandard.startFullscreen(MovieActivity.this, SimpleVideoPlayer.class, video.preview_video_url, movie.title);
            dialog.dismiss();
            return;
        }

        Call<AvgleSearchResult> call = Avgle.INSTANCE.search(movie.code);
        call.enqueue(new Callback<AvgleSearchResult>() {
            @Override
            public void onResponse(Call<AvgleSearchResult> call, Response<AvgleSearchResult> response) {
                if (response.isSuccessful()) {
                    AvgleSearchResult result = response.body();
                    if (result.success && result.response.videos.size() > 0) {
                        video = result.response.videos.get(0);
                        JZVideoPlayerStandard.startFullscreen(MovieActivity.this, SimpleVideoPlayer.class, video.preview_video_url, movie.title);
                        dialog.dismiss();
                        return;
                    }
                }

                Toast.makeText(MovieActivity.this, "该影片暂无预览", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }

            @Override
            public void onFailure(Call<AvgleSearchResult> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(MovieActivity.this, "获取预览失败", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });
    }

    @OnClick(R.id.button_play)
    public void onPlay() {
        //TODO: Deprecated
        final ProgressDialog dialog = ProgressDialog.show(this, "请稍后", "正在搜索该影片的在线视频源", true, false);

        if (video != null) {
            startActivityForResult(WebViewActivity.newIntent(MovieActivity.this, video.embedded_url), 0x0000eeff);
            dialog.dismiss();
            return;
        }

        Call<AvgleSearchResult> call = Avgle.INSTANCE.search(movie.code);
        call.enqueue(new Callback<AvgleSearchResult>() {
            @Override
            public void onResponse(Call<AvgleSearchResult> call, Response<AvgleSearchResult> response) {
                if (response.isSuccessful()) {
                    AvgleSearchResult result = response.body();
                    if (result.success && result.response.videos.size() > 0) {
                        video = result.response.videos.get(0);
                        startActivityForResult(WebViewActivity.newIntent(MovieActivity.this, video.embedded_url), 0x0000eeff);
                        dialog.dismiss();
                        return;
                    }
                }

                Toast.makeText(MovieActivity.this, "该影片暂无在线视频源", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }

            @Override
            public void onFailure(Call<AvgleSearchResult> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(MovieActivity.this, "获取视频源失败", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0x0000eeff && resultCode == RESULT_OK) {
            JZVideoPlayerStandard.startFullscreen(MovieActivity.this, SimpleVideoPlayer.class, data.getStringExtra("m3u8"), movie.title);
        }
    }


    @Override
    public void onBackPressed() {
        if (JZVideoPlayer.backPress()) {
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        JZVideoPlayer.releaseAllVideos();
    }
}
