package com.example.fahim.alphareader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.fahim.alphareader.DataClasses.BookFB;
import com.example.fahim.alphareader.DataClasses.ChapterFB;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

public class ViewerActivity extends AppCompatActivity {
    public static final int THEME_CLOUDY = 2;
    public static final int THEME_DAY = 0;
    public static final int THEME_NIGHT = 1;

    private BookFB bookFB;
    private ChapterFB chapterFB;

    private long lastCompletedSaved;

    private TextView chapterTitleTv;
    private WebView chapterViewer;
    private TextView percentDone;
    private ProgressBar progressBar;

//    private double prevCompleted;
    private int themeCode;
    private int fontSize;

    private int scrollState;

    public static final int SCROLL_INITIAL = 0;
    public static final int SCROLL_FONT_CHANGE = 1;
    public static final int SCROLL_COMPLETE = 2;
    public static final int SCROLL_DONE = 3;

    private boolean checkDataInStorage() {
        if (checkPermission()) {
            String path = chapterFB.getFilePath();
            File file = new File(path);

            if (file.exists()) {
                chapterViewer.loadUrl("file:///" + file);
                return true;
            }
        }
        requestPermission();
        return false;
    }

    private boolean checkPermission() {
        boolean bool;
        bool = ContextCompat.checkSelfPermission((Context) this, "android.permission.WRITE_EXTERNAL_STORAGE") == 0;
        return bool;
    }

    private void getChapterData() {
        if (!checkDataInStorage()) {
            chapterViewer.loadUrl("file:///android_asset/not_found.html");
        }
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale((Activity)this, "android.permission.WRITE_EXTERNAL_STORAGE")) {
            Toast.makeText((Context)this, "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions((Activity)this, new String[] { "android.permission.WRITE_EXTERNAL_STORAGE" }, 1);
        }
    }

    private void setChapterTitleTvOnClick() {
        this.chapterTitleTv.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            public void onClick(View param1View) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ViewerActivity.this);

                final View customLayout = getLayoutInflater().inflate(R.layout.custom_alert_dialog, null);
                builder.setView(customLayout);

                final ImageButton whiteTheme = customLayout.findViewById(R.id.theme_white);
                final ImageButton cloudyTheme = customLayout.findViewById(R.id.theme_cloudy);
                final ImageButton darkTheme = customLayout.findViewById(R.id.theme_black);

                final Button incFontSize = customLayout.findViewById(R.id.font_inc);
                final TextView fontTV = customLayout.findViewById(R.id.font_tv);
                fontTV.setText("" + fontSize);
                final Button decFontSize = customLayout.findViewById(R.id.font_dec);


                incFontSize.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int newFontSize = fontSize + 1;
                        if(newFontSize > 25 ){
                            Toast.makeText(ViewerActivity.this, "Font Size can not be greater than 25", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        setFontSizeOfWebView(newFontSize);
                        fontTV.setText("" + newFontSize);
                    }
                });

                decFontSize.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int newFontSize = fontSize - 1;
                        if(newFontSize < 5 ){
                            Toast.makeText(ViewerActivity.this, "Font Size can not be less than 5", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        setFontSizeOfWebView(newFontSize);
                        fontTV.setText("" + newFontSize);
                    }
                });



                whiteTheme.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View param2View) {
                        ViewerActivity.this.setThemeOfWebView(THEME_DAY);
                    }
                });
                cloudyTheme.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View param2View) {
                        ViewerActivity.this.setThemeOfWebView(THEME_CLOUDY);
                    }
                });
                darkTheme.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View param2View) {
                        ViewerActivity.this.setThemeOfWebView(THEME_NIGHT);
                    }
                });
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface param2DialogInterface, int param2Int) {
                        param2DialogInterface.cancel();
                    }
                });
                builder.show();
            }
        });
    }

    private void setPercentDoneOnClick() {
        this.percentDone.setOnClickListener(new View.OnClickListener() {
            public void onClick(View param1View) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ViewerActivity.this);
                builder.setTitle("Scroll");
                final EditText input = new EditText((Context)ViewerActivity.this);
                DecimalFormat decimalFormat = new DecimalFormat("##.##");
                String text = "" + decimalFormat.format(chapterFB.getCompleted());
                input.setText(text);
                builder.setView((View)input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface param2DialogInterface, int param2Int) {
                        try {
                            double scrollPercentInput = Double.parseDouble(input.getText().toString());
                            if (scrollPercentInput >= 0.0 && scrollPercentInput <= 100.0) {
                                chapterFB.setCompleted(scrollPercentInput);
                                chapterViewer.scrollTo(0, calculateScrollPixel(scrollPercentInput));
                                ViewerActivity.this.setPercentDoneText(chapterViewer.getScrollY());
                            } else {
                                throw new  Exception();
                            }
                        } catch (Exception exception) {
                            Toast.makeText((Context)ViewerActivity.this, "Invalid input", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface param2DialogInterface, int param2Int) {
                        param2DialogInterface.cancel();
                    }
                });
                builder.show();
            }
        });
    }

    private void setPercentDoneText(int ypos) {
        double d1 = (chapterViewer.getContentHeight() * (getResources().getDisplayMetrics()).density);
        double d2 = (ypos + chapterViewer.getHeight());
        double d3 = Math.min(100.0, d2 * 100.0 / d1);

        chapterFB.setCompleted(d3);
        DecimalFormat decimalFormat = new DecimalFormat("##.##");
        String str = decimalFormat.format(this.chapterFB.getCompleted()) + "%";
        this.percentDone.setText(str);
    }

    private void setThemeOfWebView(int themeCode) {
        String str = "";
        if(themeCode == THEME_CLOUDY) str = "reading sidenav  scalefonts day-mode subscribe-panel library nav-collapsed cloudy-mode";
        else if(themeCode == THEME_NIGHT) str = "reading sidenav  scalefonts day-mode subscribe-panel library nav-collapsed night-mode";
        else if(themeCode == THEME_DAY) str = "reading sidenav  scalefonts day-mode subscribe-panel library nav-collapsed";

        this.themeCode = themeCode;
        WebView webView = this.chapterViewer;
        String stringBuilder = "javascript:(function(){document.body.className = '" + str + "'})()";
        webView.loadUrl(stringBuilder);
    }

    private void setFontSizeOfWebView(int newFontSize){
        if(chapterViewer != null){
            double size = newFontSize / 10.0;
            String js = "document.getElementById(\"sbo-rt-content\").style=\"transform: none; padding: 0px; width: 98% !important; max-width: 98% !important; font-size: " + size + "em !important;\"";
            chapterViewer.loadUrl("javascript:(function() {" + js+ "})()");
        }
        fontSize = newFontSize;
    }

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_viewer);

        Bundle savedInstanceState = getIntent().getExtras();
        if (savedInstanceState == null)
            finish();

        scrollState = SCROLL_INITIAL;

        chapterFB = savedInstanceState.getParcelable("chapterFB");
        bookFB = savedInstanceState.getParcelable("bookFB");

        if (this.chapterFB == null) {
            Toast.makeText(this, "Chapter error!", Toast.LENGTH_LONG).show();
            finish();
        }

        chapterViewer = findViewById(R.id.webview);
        percentDone = findViewById(R.id.percentDoneTv);
        progressBar = findViewById(R.id.chapterLoading);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences((Context)this);

        ViewerActivity.this.findViewById(R.id.loadingLL).setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        themeCode = sharedPreferences.getInt("themeCode", 0);
        fontSize = sharedPreferences.getInt("fontSize", 10);
        chapterTitleTv = findViewById(R.id.chapterTitleTv);

        chapterTitleTv.setText(this.chapterFB.getChapterName());
        setChapterTitleTvOnClick();
        (findViewById(R.id.backIv)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View param1View) {
                ViewerActivity.this.onBackPressed();
            }
        });

        percentDone.setText("0%");
        setPercentDoneOnClick();
        WebSettings webSettings = chapterViewer.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        if (Build.VERSION.SDK_INT >= 23) {
            chapterViewer.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                public void onScrollChange(View param1View, int param1Int1, int param1Int2, int param1Int3, int param1Int4) {
                    setPercentDoneText(chapterViewer.getScrollY());
                    if(scrollState == SCROLL_COMPLETE){
                        ViewerActivity.this.findViewById(R.id.loadingLL).setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                        scrollState = SCROLL_DONE;
                    }
                }
            });
        } else {
            chapterViewer.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                public void onScrollChanged() {
                    setPercentDoneText(chapterViewer.getScrollY());
                    if(scrollState == SCROLL_COMPLETE){
                        scrollState = SCROLL_DONE;
                        ViewerActivity.this.findViewById(R.id.loadingLL).setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                    }
                }
            });
        }

        chapterViewer.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if(newProgress == 100){
                    if(scrollState == SCROLL_INITIAL) {
                        scrollState = SCROLL_FONT_CHANGE;
                        setThemeOfWebView(themeCode);
                        setFontSizeOfWebView(fontSize);
                    }
                    else if(scrollState == SCROLL_FONT_CHANGE) {
                        scrollState = SCROLL_COMPLETE;
                        (new Handler()).postDelayed(new Runnable() {
                            public void run() {
                                int ypos = calculateScrollPixel(chapterFB.getCompleted());
                                if(ypos == 0) ypos = 1;
                                chapterViewer.scrollTo(0, ypos);
                            }
                        },  200);
                    }
                }
            }
        });

        FirebaseDatabase.getInstance().getReference().child("Chapters").child(bookFB.getBookKey()).child("ChapterList").child(chapterFB.getChapterKey()).child("completed").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chapterFB.setCompleted(Double.parseDouble(snapshot.getValue().toString()));
                getChapterData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private int calculateScrollPixel(double percent){
         double d1 = chapterViewer.getContentHeight() * (ViewerActivity.this.getResources().getDisplayMetrics()).density;
         double d2 = (percent * d1) / 100;
         int v1 = (int)d2 - chapterViewer.getHeight();
         return Math.max(v1, 0);
    }

    protected void onPause() {
        super.onPause();
        FirebaseDatabase.getInstance().getReference().child("Chapters").child(bookFB.getBookKey()).child("ChapterList").child(chapterFB.getChapterKey()).child("completed").setValue(chapterFB.getCompleted());
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt("themeCode", this.themeCode);
        editor.putInt("fontSize", this.fontSize);
        editor.apply();
    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1)
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("ViewerActivityLog", "Permission Granted, Now you can use local drive .");
            } else {
                Log.d("ViewerActivityLog", "Permission Denied, You cannot use local drive .");
            }
    }
}
