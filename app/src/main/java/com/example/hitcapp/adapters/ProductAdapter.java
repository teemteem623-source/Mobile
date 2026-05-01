package com.example.hitcapp.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.hitcapp.R;
import com.example.hitcapp.models.Product;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private List<Product> productList = new ArrayList<>();
    private OnProductClickListener listener;
    private int limit = Integer.MAX_VALUE;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductAdapter(OnProductClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Product> list) {
        this.productList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setLimit(int limit) {
        this.limit = limit;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.tvName.setText(product.getName());
        
        DecimalFormat formatter = new DecimalFormat("#,###");
        holder.tvPrice.setText(formatter.format(product.getPrice()) + "đ");

        // Hiển thị mác giảm giá ở góc trái trên
        if (product.getDiscountPercent() > 0) {
            holder.tvSaleLabel.setVisibility(View.VISIBLE);
            holder.tvSaleLabel.setText("-" + product.getDiscountPercent() + "%");
            
            if (product.getOriginalPrice() > 0) {
                holder.tvOriginalPrice.setVisibility(View.VISIBLE);
                holder.tvOriginalPrice.setText(formatter.format(product.getOriginalPrice()) + "đ");
                holder.tvOriginalPrice.setPaintFlags(holder.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                holder.tvOriginalPrice.setVisibility(View.GONE);
            }
        } else {
            holder.tvSaleLabel.setVisibility(View.GONE);
            holder.tvOriginalPrice.setVisibility(View.GONE);
        }

        Glide.with(holder.itemView.getContext())
                .load(product.getImageUrl())
                .placeholder(R.drawable.phone_mockup)
                .error(R.drawable.phone_mockup)
                .centerCrop()
                .into(holder.imgProduct);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProductClick(product);
        });
    }

    @Override
    public int getItemCount() {
        if (productList == null) return 0;
        return Math.min(productList.size(), limit);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView tvName, tvPrice, tvSaleLabel, tvOriginalPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvSaleLabel = itemView.findViewById(R.id.tvSaleLabel);
            tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice);
        }
    }
}
