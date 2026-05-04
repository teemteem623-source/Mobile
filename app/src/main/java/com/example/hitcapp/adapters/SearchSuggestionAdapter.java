package com.example.hitcapp.adapters;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hitcapp.R;

import java.util.ArrayList;
import java.util.List;

public class SearchSuggestionAdapter extends RecyclerView.Adapter<SearchSuggestionAdapter.ViewHolder> {

    private List<String> suggestions = new ArrayList<>();
    private String query = "";
    private OnSuggestionClickListener listener;
    private OnDeleteHistoryListener deleteListener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(String suggestion);
    }

    public interface OnDeleteHistoryListener {
        void onDeleteClick(String suggestion);
    }

    public SearchSuggestionAdapter(OnSuggestionClickListener listener) {
        this.listener = listener;
    }

    public void setOnDeleteHistoryListener(OnDeleteHistoryListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setData(List<String> newData, String query) {
        this.suggestions = newData;
        this.query = query;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String suggestion = suggestions.get(position);
        
        // Neu query rong -> dang hien thi lich su
        boolean isHistory = (query == null || query.isEmpty());

        if (isHistory) {
            holder.imgIcon.setImageResource(android.R.drawable.ic_menu_recent_history);
            holder.btnDeleteHistory.setVisibility(View.VISIBLE);
            holder.tvSuggestion.setText(suggestion);
        } else {
            holder.imgIcon.setImageResource(R.drawable.find);
            holder.btnDeleteHistory.setVisibility(View.GONE);
            
            if (suggestion.toLowerCase().contains(query.toLowerCase())) {
                SpannableString spannable = new SpannableString(suggestion);
                int startPos = suggestion.toLowerCase().indexOf(query.toLowerCase());
                int endPos = startPos + query.length();
                spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#3B82F6")),
                        startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.tvSuggestion.setText(spannable);
            } else {
                holder.tvSuggestion.setText(suggestion);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSuggestionClick(suggestion);
        });

        holder.btnDeleteHistory.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDeleteClick(suggestion);
        });
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSuggestion;
        ImageView imgIcon, btnDeleteHistory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSuggestion = itemView.findViewById(R.id.tvSuggestion);
            imgIcon = itemView.findViewById(R.id.imgIcon);
            btnDeleteHistory = itemView.findViewById(R.id.btnDeleteHistory);
        }
    }
}
