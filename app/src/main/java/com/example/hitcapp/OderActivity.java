package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.hitcapp.models.CartItem;
import com.example.hitcapp.models.Order;
import com.example.hitcapp.models.OrderItem;
import com.example.hitcapp.models.Product;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class OderActivity extends AppCompatActivity {

    private RecyclerView rvRelatedProducts, rvOrders;
    private OrderAdapter orderAdapter;
    private RelatedProductAdapter relatedAdapter;
    private List<Product> relatedList = new ArrayList<>();
    private List<Order> fullOrderList = new ArrayList<>();
    private List<Order> displayOrderList = new ArrayList<>();
    private LinearLayout layoutEmptyOrders;
    private SwipeRefreshLayout swipeRefresh;
    private ChipGroup chipGroupStatus;
    private EditText edtSearchOrder;
    private TextView tvCartBadge;
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_oder);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupAdapters();
        setupFilters();
        setupSearch();
        fetchAllOrders();
        updateCartBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        rvRelatedProducts = findViewById(R.id.rvRelatedProducts);
        rvOrders = findViewById(R.id.rvOrders);
        layoutEmptyOrders = findViewById(R.id.layoutEmptyOrders);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        chipGroupStatus = findViewById(R.id.chipGroupStatus);
        edtSearchOrder = findViewById(R.id.edtSearchOrder);
        tvCartBadge = findViewById(R.id.tvCartBadge);

        findViewById(R.id.layoutCart).setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::fetchAllOrders);
        }

        findViewById(R.id.btnShopNow).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        View rootView = findViewById(R.id.main_order_list);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, 0);
                return insets;
            });
        }
    }

    private void updateCartBadge() {
        if (tvCartBadge == null || auth.getCurrentUser() == null) return;
        db.collection("carts")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    if (count > 0) {
                        tvCartBadge.setVisibility(View.VISIBLE);
                        tvCartBadge.setText(String.valueOf(count));
                    } else {
                        tvCartBadge.setVisibility(View.GONE);
                    }
                });
    }

    private void setupAdapters() {
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderAdapter(displayOrderList);
        rvOrders.setAdapter(orderAdapter);

        rvRelatedProducts.setLayoutManager(new GridLayoutManager(this, 2));
        relatedAdapter = new RelatedProductAdapter(relatedList);
        rvRelatedProducts.setAdapter(relatedAdapter);
    }

    private void setupFilters() {
        if (chipGroupStatus == null) return;
        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            String filter = "Tất cả";
            if (checkedId == R.id.chipPending) filter = "Chờ xác nhận";
            else if (checkedId == R.id.chipConfirmed) filter = "Đã xác nhận";
            else if (checkedId == R.id.chipShipping) filter = "Vận chuyển";
            else if (checkedId == R.id.chipDelivering) filter = "Đang giao";
            else if (checkedId == R.id.chipCancelled) filter = "Đã hủy";
            
            filterList(filter);
        });
    }

    private void setupSearch() {
        if (edtSearchOrder == null) return;
        edtSearchOrder.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchOrder(edtSearchOrder.getText().toString().trim());
                return true;
            }
            return false;
        });

        edtSearchOrder.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) filterList("Tất cả");
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void searchOrder(String query) {
        if (query.isEmpty()) {
            filterList("Tất cả");
            return;
        }
        displayOrderList.clear();
        for (Order o : fullOrderList) {
            boolean matches = false;
            if (o.getId() != null && o.getId().toLowerCase().contains(query.toLowerCase())) matches = true;
            if (o.getItems() != null) {
                for (OrderItem item : o.getItems()) {
                    if (item.getName() != null && item.getName().toLowerCase().contains(query.toLowerCase())) {
                        matches = true;
                        break;
                    }
                }
            }
            if (matches) displayOrderList.add(o);
        }
        orderAdapter.notifyDataSetChanged();
        layoutEmptyOrders.setVisibility(displayOrderList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void filterList(String status) {
        displayOrderList.clear();
        if (status.equals("Tất cả")) {
            displayOrderList.addAll(fullOrderList);
        } else {
            for (Order o : fullOrderList) {
                if (status.equalsIgnoreCase(o.getStatus())) displayOrderList.add(o);
            }
        }
        orderAdapter.notifyDataSetChanged();
        layoutEmptyOrders.setVisibility(displayOrderList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void fetchAllOrders() {
        if (auth.getCurrentUser() == null) return;
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        
        String userId = auth.getCurrentUser().getUid();
        db.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    fullOrderList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Order order = doc.toObject(Order.class);
                            order.setId(doc.getId());
                            fullOrderList.add(order);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    Collections.sort(fullOrderList, (o1, o2) -> {
                        if (o1.getTimestamp() == null || o2.getTimestamp() == null) return 0;
                        return o2.getTimestamp().compareTo(o1.getTimestamp());
                    });
                    
                    filterList("Tất cả"); 
                    if (!fullOrderList.isEmpty()) fetchRelatedProducts(fullOrderList.get(0));
                    else fetchDefaultRelated();
                    
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                });
    }

    private void fetchRelatedProducts(Order lastOrder) {
        if (lastOrder.getItems() == null || lastOrder.getItems().isEmpty()) return;
        db.collection("products").document(lastOrder.getItems().get(0).getProductId()).get().addOnSuccessListener(ds -> {
            Product p = ds.toObject(Product.class);
            if (p != null) {
                db.collection("products").whereEqualTo("category", p.getCategory()).limit(6).get().addOnSuccessListener(qs -> {
                    relatedList.clear();
                    for (QueryDocumentSnapshot doc : qs) {
                        Product prod = doc.toObject(Product.class);
                        prod.setId(doc.getId());
                        relatedList.add(prod);
                    }
                    relatedAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    private void fetchDefaultRelated() {
        db.collection("products").limit(6).get().addOnSuccessListener(qs -> {
            relatedList.clear();
            for (QueryDocumentSnapshot doc : qs) {
                Product prod = doc.toObject(Product.class);
                prod.setId(doc.getId());
                relatedList.add(prod);
            }
            relatedAdapter.notifyDataSetChanged();
        });
    }

    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
        private List<Order> list;
        private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
        OrderAdapter(List<Order> list) { this.list = list; }
        @NonNull @Override public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new OrderViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_order, p, false));
        }
        @Override public void onBindViewHolder(@NonNull OrderViewHolder h, int pos) {
            Order o = list.get(pos);
            if (o.getTimestamp() != null) h.tvOrderDate.setText(sdf.format(o.getTimestamp().toDate()));
            h.tvItemStatus.setText(o.getStatus());
            if (o.getItems() != null && !o.getItems().isEmpty()) {
                OrderItem first = o.getItems().get(0);
                h.tvItemProductName.setText(first.getName());
                h.tvItemDetails.setText("Số lượng: " + first.getQuantity() + (o.getItems().size() > 1 ? " và " + (o.getItems().size()-1) + " sp khác" : ""));
                Glide.with(h.itemView.getContext()).load(first.getImageUrl()).placeholder(R.drawable.phone_mockup).into(h.imgItemProduct);
            }
            h.tvItemTotalPrice.setText(String.format(Locale.getDefault(), "%,dđ", o.getTotalAmount()));
            
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(OderActivity.this, OrderDetailActivity.class);
                intent.putExtra("ORDER_ID", o.getId());
                startActivity(intent);
            });

            h.btnItemAddCart.setOnClickListener(v -> addOrderToCart(o));
            h.btnItemAction.setOnClickListener(v -> buyOrderAgain(o));
        }

        private void addOrderToCart(Order order) {
            if (order.getItems() == null || auth.getCurrentUser() == null) return;
            String userId = auth.getUid();
            for (OrderItem item : order.getItems()) {
                CartItem cartItem = new CartItem(
                        userId, item.getProductId(), item.getName(), "", 
                        item.getPrice(), item.getPrice(), item.getQuantity(), item.getImageUrl()
                );
                db.collection("carts").add(cartItem);
            }
            Toast.makeText(OderActivity.this, "TT Shop đã thêm sản phẩm vào giỏ hàng cho bạn!", Toast.LENGTH_SHORT).show();
            updateCartBadge();
        }

        private void buyOrderAgain(Order order) {
            if (order.getItems() == null || auth.getCurrentUser() == null) return;
            ArrayList<CartItem> selectedItems = new ArrayList<>();
            for (OrderItem item : order.getItems()) {
                selectedItems.add(new CartItem(
                        auth.getUid(), item.getProductId(), item.getName(), "",
                        item.getPrice(), item.getPrice(), item.getQuantity(), item.getImageUrl()
                ));
            }
            Intent intent = new Intent(OderActivity.this, PaymentActivity.class);
            intent.putExtra("SELECTED_ITEMS_DATA", selectedItems);
            startActivity(intent);
        }

        @Override public int getItemCount() { return list.size(); }
        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView tvOrderDate, tvItemStatus, tvItemProductName, tvItemDetails, tvItemTotalPrice;
            ImageView imgItemProduct, btnItemAddCart;
            MaterialButton btnItemAction;
            OrderViewHolder(View v) {
                super(v);
                tvOrderDate = v.findViewById(R.id.tvOrderDate);
                tvItemStatus = v.findViewById(R.id.tvItemStatus);
                tvItemProductName = v.findViewById(R.id.tvItemProductName);
                tvItemDetails = v.findViewById(R.id.tvItemDetails);
                tvItemTotalPrice = v.findViewById(R.id.tvItemTotalPrice);
                imgItemProduct = v.findViewById(R.id.imgItemProduct);
                btnItemAddCart = v.findViewById(R.id.btnItemAddCart);
                btnItemAction = v.findViewById(R.id.btnItemAction);
            }
        }
    }

    private class RelatedProductAdapter extends RecyclerView.Adapter<RelatedProductAdapter.ViewHolder> {
        private List<Product> list;
        RelatedProductAdapter(List<Product> list) { this.list = list; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_product_card, p, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            Product p = list.get(pos);
            h.tvName.setText(p.getName());
            h.tvPrice.setText(String.format(Locale.getDefault(), "%,dđ", p.getPrice()));
            Glide.with(h.itemView.getContext()).load(p.getImageUrl()).into(h.imgProduct);
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(OderActivity.this, DetailActivity.class);
                intent.putExtra("PRODUCT_ID", p.getId());
                startActivity(intent);
            });
        }
        @Override public int getItemCount() { return list.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvPrice; ImageView imgProduct;
            ViewHolder(View v) { super(v); tvName = v.findViewById(R.id.tvProductName); tvPrice = v.findViewById(R.id.tvProductPrice); imgProduct = v.findViewById(R.id.imgProduct); }
        }
    }
}
