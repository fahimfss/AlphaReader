package com.example.fahim.alphareader.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fahim.alphareader.ChaptersActivity;
import com.example.fahim.alphareader.DataClasses.BookFB;
import com.example.fahim.alphareader.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.Calendar;

public class BookFBsAdapter extends RecyclerView.Adapter<BookFBsAdapter.bookFBsViewHolder> {
    private final ArrayList<BookFB> bookFBs;
    private final Context context;
    private final String openedBookId;

    public BookFBsAdapter(ArrayList<BookFB> paramArrayList, Context paramContext, String paramString) {
        this.bookFBs = paramArrayList;
        this.context = paramContext;
        this.openedBookId = paramString;
    }

    public int getItemCount() {
        return this.bookFBs.size();
    }

    public void onBindViewHolder(bookFBsViewHolder bookFBsViewHolder, final int position) {
        final BookFB bookFB = this.bookFBs.get(position);
        bookFBsViewHolder.bookName.setText(bookFB.getBookName());

        if(bookFB.getLastAccessed() > 0) {
            String str1 = bookFB.getNumChapters() + " chapters,  Last accessed: ";
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(bookFB.getLastAccessed());

            int mYear = calendar.get(Calendar.YEAR);
            String year = (mYear < 10)? "0" + mYear : "" + mYear;

            int mMonth = calendar.get(Calendar.MONTH) + 1;
            String month = (mMonth < 10)? "0" + mMonth : "" + mMonth;

            int mDay = calendar.get(Calendar.DAY_OF_MONTH);
            String day = (mDay < 10)? "0" + mDay : "" + mDay;

            int hour = calendar.get(Calendar.HOUR);
            if(hour == 0) hour = 12;
            String sHour = (hour < 10)? "0" + hour : "" + hour;

            int minute = calendar.get(Calendar.MINUTE);
            String sMinute = (minute < 10)? "0" + minute : "" + minute;

            String ampm = (calendar.get(Calendar.AM_PM) == Calendar.AM) ? "AM" : "PM";
            String time = "" + day + "/" + month + "/" + year + " at " + sHour + ":" + sMinute + " " + ampm;
            String str2 = str1 + time;

            bookFBsViewHolder.bookChapters.setText(str2);
        }
        else {
            String str1 = bookFB.getNumChapters() + " chapters,  Never accessed";
            bookFBsViewHolder.bookChapters.setText(str1);
        }

        bookFBsViewHolder.cv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View param1View) {
                String str = ((BookFB)BookFBsAdapter.this.bookFBs.get(position)).getBookKey();
                FirebaseDatabase.getInstance().getReference().child("Books").child("LastOpenedBook").setValue(str);
                Intent intent = new Intent(BookFBsAdapter.this.context, ChaptersActivity.class);
                long time = System.currentTimeMillis();
                FirebaseDatabase.getInstance().getReference().child("Books").child("BookList").child(bookFBs.get(position).getBookKey()).child("lastAccessed").setValue(time);
                intent.putExtra("bookFB", BookFBsAdapter.this.bookFBs.get(position));
                BookFBsAdapter.this.context.startActivity(intent);
            }
        });
        if (Build.VERSION.SDK_INT >= 23) {
            bookFBsViewHolder.cv.setBackgroundTintList(this.context.getResources().getColorStateList(R.color.colorPrimary, this.context.getTheme()));
        } else {
            bookFBsViewHolder.cv.setBackground((Drawable)new ColorDrawable(this.context.getResources().getColor(R.color.colorPrimary)));
        }

        bookFBsViewHolder.moreBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View param1View) {
                AlertDialog.Builder builder = new AlertDialog.Builder(BookFBsAdapter.this.context);
                builder.setTitle("Options");
                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface param2DialogInterface, int param2Int) {
                        if (param2Int == 0) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(BookFBsAdapter.this.context);
                            builder.setTitle(" ");
                            final EditText input = new EditText(BookFBsAdapter.this.context);
                            input.setText(bookFB.getBookName());
                            input.setInputType(1);
                            builder.setView((View)input);
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface param3DialogInterface, int param3Int) {
                                    String str = input.getText().toString();
                                    if (str.length() < 1) {
                                        Toast.makeText(BookFBsAdapter.this.context, "Invalid name", Toast.LENGTH_SHORT).show();
                                    } else {
                                        try {
                                            FirebaseDatabase.getInstance().getReference().child("Books").child("BookList").child(bookFB.getBookKey()).child("bookName").setValue(str);
                                        } catch (Exception exception) {
                                            exception.printStackTrace();
                                            Toast.makeText(BookFBsAdapter.this.context, "An error occurred...", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface param3DialogInterface, int param3Int) {}
                            });
                            builder.show();
                        } else {
                            (new AlertDialog.Builder(BookFBsAdapter.this.context)).setTitle("Confirm Remove").setMessage("Are you sure you want to remove this book?").setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface param3DialogInterface, int param3Int) {
                                    String str = bookFB.getBookKey();
                                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                                    databaseReference.child("Books").child("BookList").child(str).removeValue();
                                    databaseReference.child("ChapterData").child(str).removeValue();
                                    databaseReference.child("Chapters").child(str).removeValue();
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

    public bookFBsViewHolder onCreateViewHolder(ViewGroup paramViewGroup, int paramInt) {
        return new bookFBsViewHolder(LayoutInflater.from(paramViewGroup.getContext()).inflate(R.layout.rv_item_book, paramViewGroup, false));
    }

    static class bookFBsViewHolder extends RecyclerView.ViewHolder {
        TextView bookChapters;
        TextView bookName;
        CardView cv;
        ImageView moreBtn;

        bookFBsViewHolder(View param1View) {
            super(param1View);
            this.cv = (CardView)param1View.findViewById(R.id.bookItemCard);
            this.bookName = (TextView)param1View.findViewById(R.id.bookName);
            this.bookChapters = (TextView)param1View.findViewById(R.id.bookChapters);
            this.moreBtn = (ImageView)param1View.findViewById(R.id.moreBtn);
        }
    }
}
