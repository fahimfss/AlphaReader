package com.example.fahim.alphareader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fahim.alphareader.Adapters.BookFBsAdapter;
import com.example.fahim.alphareader.DataClasses.BookFB;
import com.example.fahim.alphareader.DataClasses.ChapterFB;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
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
import java.util.HashMap;
import java.util.HashSet;

public class BooksActivity extends AppCompatActivity {
    private ArrayList<BookFB> bookFBS;

    private boolean flagBookFBS;
    private boolean flagLastOpenedBook;

    private String lastOpenedBook;

    private ValueEventListener listenerBookList;
    private ValueEventListener listenerLastOpenedBook;

    private DatabaseReference ref;

    private void firebaseLogin() {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.signInWithEmailAndPassword("fahim.cross@gmail.com", "12121212").addOnCompleteListener((Activity) this, new OnCompleteListener<AuthResult>() {

            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.d("FirebaseAuth", "signInWithEmail:success");

                    ref = FirebaseDatabase.getInstance().getReference();
                    listenerBookList = getListenerBookList();
                    listenerLastOpenedBook = getListenerLastOpenedBook();

                    ref.child("Books").child("BookList").addValueEventListener(listenerBookList);
                    ref.child("Books").child("LastOpenedBook").addValueEventListener(listenerLastOpenedBook);
                }
            }
        });
    }

    private ValueEventListener getListenerBookList() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                flagBookFBS = false;
                bookFBS = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    bookFBS.add(snap.getValue(BookFB.class));
                }
                sortBookFbs();
                flagBookFBS = true;
                syncPopulateBooksRV();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
    }

    private ValueEventListener getListenerLastOpenedBook() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                flagLastOpenedBook = false;
                if (snapshot.getValue() != null)
                    lastOpenedBook = snapshot.getValue().toString();
                else
                    lastOpenedBook = "";

                flagLastOpenedBook = true;
                syncPopulateBooksRV();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
    }

    private void populateBooksRV() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rvBooks);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager((RecyclerView.LayoutManager) new LinearLayoutManager((Context) this));
        recyclerView.setAdapter(new BookFBsAdapter(bookFBS, this, lastOpenedBook));
    }

    private void syncPopulateBooksRV() {
        if (this.flagLastOpenedBook && this.flagBookFBS)
            populateBooksRV();
    }

    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        setContentView(R.layout.activity_books);

        FirebaseApp.initializeApp((Context) this);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_books, menu);

        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if(menuItem.getItemId() == R.id.addBook) {
            (new ChooserDialog()).with(BooksActivity.this).withFilter(true, false).withStartFile(null).withChosenListener(new ChooserDialog.Result() {
                public void onChoosePath(final String path, File param1File) {
                    (new Thread(new Runnable() {
                        public void run() {
                            addBook(path);
                        }
                    })).start();
                }
            }).build().show();
        }
        else{
            if(bookFBS != null && ref != null){
                ref.child("Chapters").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        runOnUiThread(() -> findViewById(R.id.uploadingLL).setVisibility(View.VISIBLE));
                        (new Thread(new Runnable() {
                            @Override
                            public void run() {
                                HashMap<String, BookFB> bookPathBook = new HashMap<>();
                                for(BookFB book: bookFBS){
                                    bookPathBook.put(book.getFilePath(), book);
                                }

                                HashSet<String> bookChapPath = new HashSet<>();
                                for(DataSnapshot snap: snapshot.getChildren()){
                                    for(DataSnapshot chapSnap: snap.child("ChapterList").getChildren()){
                                        String chapPath = chapSnap.child("filePath").getValue().toString();
                                        bookChapPath.add(chapPath);
                                    }
                                }

                                String safariPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +  "SafariBooksNew" + File.separator;
                                File file = new File(safariPath);
                                File [] bookFiles = file.listFiles();
                                if(bookFiles!=null){
                                    for(File bookFile: bookFiles){
                                        String bPath = bookFile.getAbsolutePath();
                                        if(bookPathBook.containsKey(bPath)){
                                            File [] chapterFiles = bookFile.listFiles();
                                            if(chapterFiles!=null){
                                                for(File chapterFile: chapterFiles){
                                                    String cPath = chapterFile.getAbsolutePath();
                                                    if(!bookChapPath.contains(cPath)) {
                                                        String cName = chapterFile.getName();
                                                        BookFB b = bookPathBook.get(bPath);
                                                        String bKey = b.getBookKey();
                                                        String chapterKey = ref.child("Chapters").child(bKey).child("ChapterList").push().getKey();
                                                        long currentTime = System.currentTimeMillis();
                                                        final ChapterFB chapterFB = new ChapterFB(chapterKey, currentTime, cName, 0.0, chapterFile.getAbsolutePath());
                                                        ref.child("Chapters").child(bKey).child("ChapterList").child(chapterKey).setValue(chapterFB);
                                                        FirebaseDatabase.getInstance().getReference().child("Books").child("BookList").child(bKey).child("numChapters").setValue(b.getNumChapters() + 1);
                                                        b.setNumChapters(b.getNumChapters() + 1);
                                                    }
                                                }
                                            }
                                        }
                                        else{
                                            (new Thread(new Runnable() {
                                                public void run() {
                                                    addBook(bookFile.getAbsolutePath());
                                                }
                                            })).start();
                                        }
                                    }
                                }
                            }
                        })).start();
                        runOnUiThread(() -> findViewById(R.id.uploadingLL).setVisibility(View.GONE));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void addBook(String path){
        if(bookFBS == null) return;
        for(BookFB book: bookFBS){
            if(book.getFilePath().equals(path)){
                runOnUiThread(() -> Toast.makeText(BooksActivity.this, "Book already exists!", Toast.LENGTH_LONG));
                return;
            }
        }

        File file = new File(path);
        if (file.exists()) {
            runOnUiThread(() -> findViewById(R.id.uploadingLL).setVisibility(View.VISIBLE));

            File[] arrayOfFile = file.listFiles();
            ArrayList<File> arrayList = new ArrayList<>();
            for (File file1 : arrayOfFile) {
                if (file1.isFile()) {
                    String str1 = file1.getName();
                    if (str1.length() > 4 && str1.endsWith("html"))
                        arrayList.add(file1);
                }
            }
            long currentTime = System.currentTimeMillis();
            String bookKey = ref.child("Books").child("BookList").push().getKey();
            BookFB bookFB = new BookFB(bookKey, currentTime, arrayList.size(), file.getName(), file.getAbsolutePath());
            BooksActivity.this.ref.child("Books").child("BookList").child(bookKey).setValue(bookFB);
            for (File file1 : arrayList) {
                String chapterKey = ref.child("Chapters").child(bookKey).child("ChapterList").push().getKey();
                final ChapterFB chapterFB = new ChapterFB(chapterKey, currentTime, file1.getName(), 0.0, file1.getAbsolutePath());
                ref.child("Chapters").child(bookKey).child("ChapterList").child(chapterKey).setValue(chapterFB);
                runOnUiThread(() -> {
                    String str2 = "Uploading: " + chapterFB.getChapterName();
                    ((TextView) BooksActivity.this.findViewById(R.id.uploadingTV)).setText(str2);
                });
            }
            runOnUiThread(() -> findViewById(R.id.uploadingLL).setVisibility(View.GONE));
        }
    }

    private void sortBookFbs(){
        Collections.sort(bookFBS, new Comparator<BookFB>() {
            @Override
            public int compare(BookFB o1, BookFB o2) {
                Long l1 = o1.getLastAccessed();
                Long l2 = o2.getLastAccessed();

                return l2.compareTo(l1);
            }
        });
    }

    protected void onPause() {
        super.onPause();
        if (this.listenerBookList != null) {
            this.ref.child("Books").child("BookList").removeEventListener(this.listenerBookList);
            this.listenerBookList = null;
            this.flagBookFBS = false;
        }
        if (this.listenerLastOpenedBook != null) {
            this.ref.child("Books").child("LastOpenedBook").removeEventListener(this.listenerLastOpenedBook);
            this.listenerLastOpenedBook = null;
            this.flagLastOpenedBook = false;
        }
    }

    protected void onResume() {
        super.onResume();
        firebaseLogin();
        if (this.listenerBookList == null && this.ref != null) {
            this.listenerBookList = getListenerBookList();
            this.ref.child("Books").child("BookList").addValueEventListener(this.listenerBookList);
        }
        if (this.listenerLastOpenedBook == null && this.ref != null) {
            this.listenerLastOpenedBook = getListenerLastOpenedBook();
            this.ref.child("Books").child("LastOpenedBook").addValueEventListener(this.listenerLastOpenedBook);
        }
    }
}
