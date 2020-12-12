package com.example.fahim.alphareader.Adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fahim.alphareader.DataClasses.BookFB;
import com.example.fahim.alphareader.DataClasses.ChapterFB;
import com.example.fahim.alphareader.R;
import com.example.fahim.alphareader.ViewerActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class ChapterFBsAdapter extends RecyclerView.Adapter<ChapterFBsAdapter.ChaptersViewHolder> {
    private final BookFB bookFB;
    private final ArrayList<ChapterFB> chapterFBs;
    private final Context context;
    private final String openedChapterId;

    public ChapterFBsAdapter(ArrayList<ChapterFB> chapterFBS, Context context, String openedChapterId, BookFB bookFB) {
        this.chapterFBs = chapterFBS;
        this.context = context;
        this.openedChapterId = openedChapterId;
        this.bookFB = bookFB;
    }

    public int getItemCount() {
        return this.chapterFBs.size();
    }

    public void onBindViewHolder(ChaptersViewHolder paramChaptersViewHolder, final int position) {
        final ChapterFB chapterFB = this.chapterFBs.get(position);
        paramChaptersViewHolder.chapterName.setText(chapterFB.getChapterName());
        DecimalFormat decimalFormat = new DecimalFormat("##.##");
        String str = decimalFormat.format(chapterFB.getCompleted()) +  "% done";
        paramChaptersViewHolder.chapterData.setText(str);
        paramChaptersViewHolder.cv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View param1View) {
                String str = ((ChapterFB)ChapterFBsAdapter.this.chapterFBs.get(position)).getChapterKey();
                FirebaseDatabase.getInstance().getReference().child("Chapters").child(ChapterFBsAdapter.this.bookFB.getBookKey()).child("LastOpenedChapter").setValue(str);
                Intent intent = new Intent(ChapterFBsAdapter.this.context, ViewerActivity.class);
                intent.putExtra("chapterFB", ChapterFBsAdapter.this.chapterFBs.get(position));
                intent.putExtra("bookFB", (Parcelable)ChapterFBsAdapter.this.bookFB);
                ChapterFBsAdapter.this.context.startActivity(intent);
            }
        });
        if (Build.VERSION.SDK_INT >= 23) {
            if (chapterFB.getChapterKey().equals(this.openedChapterId)) {
                paramChaptersViewHolder.cv.setBackgroundTintList(this.context.getResources().getColorStateList(R.color.colorAccent, this.context.getTheme()));
            } else {
                paramChaptersViewHolder.cv.setBackgroundTintList(this.context.getResources().getColorStateList(R.color.colorPrimary, this.context.getTheme()));
            }
        } else if (chapterFB.getChapterKey().equals(this.openedChapterId)) {
            paramChaptersViewHolder.cv.setBackground((Drawable)new ColorDrawable(this.context.getResources().getColor(R.color.colorAccent)));
        } else {
            paramChaptersViewHolder.cv.setBackground((Drawable)new ColorDrawable(this.context.getResources().getColor(R.color.colorPrimary)));
        }
        paramChaptersViewHolder.moreBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View param1View) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ChapterFBsAdapter.this.context);
                builder.setTitle("Options");
                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface param2DialogInterface, int param2Int) {
                        if (param2Int == 0) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(ChapterFBsAdapter.this.context);
                            builder.setTitle(" ");
                            final EditText input = new EditText(ChapterFBsAdapter.this.context);
                            input.setText(chapterFB.getChapterName());
                            input.setInputType(1);
                            builder.setView((View)input);
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface param3DialogInterface, int param3Int) {
                                    String str = input.getText().toString();
                                    if (str.length() < 1) {
                                        Toast.makeText(ChapterFBsAdapter.this.context, "Invalid name", Toast.LENGTH_SHORT).show();
                                    } else {
                                        try {
                                            FirebaseDatabase.getInstance().getReference().child("Chapters").child(ChapterFBsAdapter.this.bookFB.getBookKey()).child("ChapterList").child(chapterFB.getChapterKey()).child("chapterName").setValue(str);
                                        } catch (Exception exception) {
                                            exception.printStackTrace();
                                            Toast.makeText(ChapterFBsAdapter.this.context, "An error occurred...", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface param3DialogInterface, int param3Int) {}
                            });
                            builder.show();
                        } else {
                            (new AlertDialog.Builder(ChapterFBsAdapter.this.context)).setTitle("Confirm Remove").setMessage("Are you sure you want to remove this chapter?").setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface param3DialogInterface, int param3Int) {
                                    String chapterName = chapterFB.getChapterName();
                                    try {
                                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Books").child("BookList").child(ChapterFBsAdapter.this.bookFB.getBookKey()).child("numChapters");
                                        ValueEventListener valueEventListener = new ValueEventListener() {
                                            public void onCancelled(@NonNull DatabaseError param4DatabaseError) {}

                                            public void onDataChange(DataSnapshot param4DataSnapshot) {
                                                int i = Integer.parseInt(param4DataSnapshot.getValue().toString());
                                                FirebaseDatabase.getInstance().getReference().child("Books").child("BookList").child(ChapterFBsAdapter.this.bookFB.getBookKey()).child("numChapters").setValue(i - 1);
                                            }
                                        };

                                        databaseReference.addListenerForSingleValueEvent(valueEventListener);
                                        FirebaseDatabase.getInstance().getReference().child("Chapters").child(ChapterFBsAdapter.this.bookFB.getBookKey()).child("ChapterList").child(chapterFB.getChapterKey()).removeValue();
                                        FirebaseDatabase.getInstance().getReference().child("ChapterData").child(ChapterFBsAdapter.this.bookFB.getBookKey()).child(chapterFB.getChapterKey()).removeValue();
                                        Context context = ChapterFBsAdapter.this.context;

                                        String stringBuilder = chapterName + " removed";
                                        Toast.makeText(context, stringBuilder, Toast.LENGTH_LONG).show();
                                    } catch (Exception exception) {
                                        exception.printStackTrace();
                                        Toast.makeText(ChapterFBsAdapter.this.context, "An error occurred...", Toast.LENGTH_LONG).show();
                                    }
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface param3DialogInterface, int param3Int) {}
                            }).show();
                        }
                    }
                };
                builder.setItems(new CharSequence[] { "Rename", "Remove" }, onClickListener);
                builder.create().show();
            }
        });
    }

    @NonNull
    public ChaptersViewHolder onCreateViewHolder(ViewGroup paramViewGroup, int paramInt) {
        return new ChaptersViewHolder(LayoutInflater.from(paramViewGroup.getContext()).inflate(R.layout.rv_item_chapter, paramViewGroup, false));
    }

    static class ChaptersViewHolder extends RecyclerView.ViewHolder {
        TextView chapterData;

        TextView chapterName;

        CardView cv;

        ImageView moreBtn;

        ChaptersViewHolder(View param1View) {
            super(param1View);
            cv = itemView.findViewById(R.id.chapterItemCard);
            chapterName = itemView.findViewById(R.id.chapterName);
            chapterData = itemView.findViewById(R.id.chapterData);
            moreBtn = itemView.findViewById(R.id.moreBtn);
        }
    }
}
