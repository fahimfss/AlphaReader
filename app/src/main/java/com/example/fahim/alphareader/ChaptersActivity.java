package com.example.fahim.alphareader;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fahim.alphareader.Adapters.ChapterFBsAdapter;
import com.example.fahim.alphareader.DataClasses.BookFB;
import com.example.fahim.alphareader.DataClasses.ChapterFB;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.obsez.android.lib.filechooser.ChooserDialog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ChaptersActivity extends AppCompatActivity {
    private BookFB bookFB;

    private ArrayList<ChapterFB> chapterFBS;

    private boolean flagChapterFBS;
    private boolean flagLastOpenedChapter;

    private String lastOpenedChapter;

    private ValueEventListener listenerChapterList;
    private ValueEventListener listenerLastOpenedChapter;

    private DatabaseReference ref;

    private ValueEventListener getListenerChapterList() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                flagChapterFBS =false;
                Log.d("ChaptersActivityLog", "get chapterFBS");
                chapterFBS = new ArrayList<>();
                for(DataSnapshot snap: snapshot.getChildren()){
                    chapterFBS.add(snap.getValue(ChapterFB.class));
                }
                sortChapterFBs();
                String size = "chapterFBS size: " + chapterFBS.size();
                Log.d("ChaptersActivityLog", size);
                flagChapterFBS =true;
                syncPopulateChaptersRV();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
    }

    private ValueEventListener getListenerLastOpenedChapter() {
        return new ValueEventListener() {
            public void onCancelled(@NonNull DatabaseError error) {}

            public void onDataChange(@NonNull DataSnapshot snapshot) {
                flagLastOpenedChapter = false;
                Log.d("ChaptersActivityLog", "get lastOpenedChapter");
                if (snapshot.getValue() != null) {
                    lastOpenedChapter = snapshot.getValue().toString();
                } else {
                    lastOpenedChapter = "";
                }
                String string = "LastOpenedChapter: " + ChaptersActivity.this.lastOpenedChapter;
                Log.d("ChaptersActivityLog", string);
                flagLastOpenedChapter = true;
                syncPopulateChaptersRV();
            }
        };
    }

    private void restoreListState() {
        final RecyclerView chaptersRecyclerView = (RecyclerView)findViewById(R.id.rvChapters);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences((Context)this);
        int pos = preferences.getInt(bookFB.getBookKey() +  "position", 0) ;
        int offset = preferences.getInt(bookFB.getBookKey() +  "offset", 0) ;

        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                ((LinearLayoutManager)chaptersRecyclerView.getLayoutManager()).scrollToPositionWithOffset(pos, offset);
            }
        }, 200);
    }

    private void setListState() {
        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.rvChapters);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences((Context)this);
        View view = recyclerView.getChildAt(0);
        if (view == null)
            return;
        int i = recyclerView.getChildAdapterPosition(view);
        int j = view.getTop();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(bookFB.getBookKey()+"position", i).apply();
        editor.putInt(bookFB.getBookKey()+"offset", j).apply();
    }

    private void sortChapterFBs() {
        Collections.sort(this.chapterFBS, new Comparator<ChapterFB>() {
            public int compare(ChapterFB chapterFB1, ChapterFB chapterFB2) {
                int dotPos1 = -1;
                int dotPos2 = -1;

                String name1 = chapterFB1.getChapterName();
                String name2 = chapterFB2.getChapterName();
                for(int i=0; i<name1.length(); i++) if(name1.charAt(i) == '.') {
                    dotPos1 = i;
                    break;
                }
                for(int i=0; i<name2.length(); i++) if(name2.charAt(i) == '.') {
                    dotPos2 = i;
                    break;
                }

                if(dotPos1 == -1 || dotPos2 == -1)
                    return name1.compareTo(name2);

                try {
                    Character c1 = name1.charAt(dotPos1-1);
                    Character c2 = name2.charAt(dotPos2-1);

                    int charCount = 0;

                    if((c1>='a' && c1<='z') || (c1>='A' && c1<='Z')) {
                        dotPos1--; charCount++;
                    }
                    if((c2>='a' && c2<='z') || (c2>='A' && c2<='Z')) {
                        dotPos2--; charCount++;
                    }

                    Integer num1 = Integer.parseInt(name1.substring(0, dotPos1));
                    Integer num2 = Integer.parseInt(name2.substring(0, dotPos2));

                    if(num1.equals(num2) && charCount == 2){
                        return c1.compareTo(c2);
                    }

                    return num1.compareTo(num2);
                }
                catch (Exception e){
                    return name1.compareTo(name2);
                }
            }
        });
    }

    private void syncPopulateChaptersRV() {
        if (this.flagChapterFBS && this.flagLastOpenedChapter)
            populateChaptersRV();
    }

    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        setContentView(R.layout.activity_chapters);
        paramBundle = getIntent().getExtras();
        if (paramBundle == null)
            finish();
        bookFB = (BookFB)paramBundle.getParcelable("bookFB");
        if (bookFB == null) {
            Toast.makeText((Context)this, "Book error!", Toast.LENGTH_LONG).show();
            finish();
        }

        String title = bookFB.getBookName() + "'s Chapters";

        ((TextView)findViewById(R.id.chapterName)).setText(title);

        this.ref = FirebaseDatabase.getInstance().getReference();
        this.listenerChapterList = getListenerChapterList();
        this.listenerLastOpenedChapter = getListenerLastOpenedChapter();
        ref.child("Chapters").child(this.bookFB.getBookKey()).child("ChapterList").addValueEventListener(listenerChapterList);
        ref.child("Chapters").child(this.bookFB.getBookKey()).child("LastOpenedChapter").addValueEventListener(listenerLastOpenedChapter);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chapters, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.addChapter) {
            (new ChooserDialog()).with((Context)this).withStartFile(null).withChosenListener(new ChooserDialog.Result() {
                public void onChoosePath(String param1String, File param1File) {
                    File file = new File(param1String);
                    if (file.exists()) {
                        String filePath = file.getAbsolutePath();
                        for(ChapterFB chapter: chapterFBS){
                            if(chapter.getFilePath().equals(filePath)){
                                runOnUiThread(() -> Toast.makeText(ChaptersActivity.this, "Chapter file already exists!", Toast.LENGTH_LONG));
                                return;
                            }
                        }

                        try {
                            String fileName = file.getName();
                            if (fileName.length() > 4 && fileName.substring(fileName.length() - 4).equals("html")) {
                                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Books").child("BookList").child(ChaptersActivity.this.bookFB.getBookKey()).child("numChapters");
                                ValueEventListener valueEventListener = new ValueEventListener() {
                                    public void onCancelled(@NonNull DatabaseError databaseError) {}

                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        int i = Integer.parseInt(dataSnapshot.getValue().toString());
                                        FirebaseDatabase.getInstance().getReference().child("Books").child("BookList").child(ChaptersActivity.this.bookFB.getBookKey()).child("numChapters").setValue(i + 1);
                                    }
                                };
                                databaseReference.addListenerForSingleValueEvent(valueEventListener);

                                String chapterKey = ChaptersActivity.this.ref.child("Chapters").child(ChaptersActivity.this.bookFB.getBookKey()).child("ChapterList").push().getKey();
                                long currentTime = System.currentTimeMillis();
                                final ChapterFB chapterFB = new ChapterFB(chapterKey, currentTime, fileName, 0.0, filePath);
                                ChaptersActivity.this.ref.child("Chapters").child(ChaptersActivity.this.bookFB.getBookKey()).child("ChapterList").child(chapterKey).setValue(chapterFB);
                            } else {
                                Toast.makeText((Context)ChaptersActivity.this, "Invalid chapter type. Must be HTML!", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception exception) {
                            exception.printStackTrace();
                            Toast.makeText((Context)ChaptersActivity.this, "Error adding chapter!", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText((Context)ChaptersActivity.this, "Error adding chapter!", Toast.LENGTH_LONG).show();
                    }
                }
            }).build().show();

        } else {
            onBackPressed();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    protected void onPause() {
        super.onPause();
        if (this.listenerChapterList != null) {
            this.ref.child("Chapters").child(this.bookFB.getBookKey()).child("ChapterList").removeEventListener(this.listenerChapterList);
            this.listenerChapterList = null;
            this.flagChapterFBS = false;
        }
        if (this.listenerLastOpenedChapter != null) {
            this.ref.child("Chapters").child(this.bookFB.getBookKey()).child("LastOpenedChapter").removeEventListener(this.listenerLastOpenedChapter);
            this.listenerLastOpenedChapter = null;
            this.flagLastOpenedChapter = false;
        }
        setListState();
    }

    protected void onResume() {
        super.onResume();
        if (this.listenerChapterList == null && this.ref != null) {
            this.listenerChapterList = getListenerChapterList();
            this.ref.child("Chapters").child(this.bookFB.getBookKey()).child("ChapterList").addValueEventListener(this.listenerChapterList);
        }
        if (this.listenerLastOpenedChapter == null && this.ref != null) {
            this.listenerLastOpenedChapter = getListenerLastOpenedChapter();
            this.ref.child("Chapters").child(this.bookFB.getBookKey()).child("LastOpenedChapter").addValueEventListener(this.listenerLastOpenedChapter);
        }
    }

    public void populateChaptersRV() {
        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.rvChapters);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager((RecyclerView.LayoutManager)new LinearLayoutManager((Context)this));
        recyclerView.setAdapter((RecyclerView.Adapter)new ChapterFBsAdapter(this.chapterFBS, (Context)this, this.lastOpenedChapter, this.bookFB));
        restoreListState();
    }
}
