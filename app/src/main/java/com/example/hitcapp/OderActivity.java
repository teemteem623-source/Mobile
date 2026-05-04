package com.example.hitcapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import com.example.hitcapp.adapters.SearchSuggestionAdapter;
import com.example.hitcapp.models.CartItem;
import com.example.hitcapp.models.Order;
import com.example.hitcapp.models.Product;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OderActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SearchPrefs";
    private static final String KEY_HISTORY = "search_history";

    private RecyclerView rvRelatedProducts, rvOrders;
    private OrderAdapter orderAdapter;
    private RelatedProductAdapter relatedAdapter;
    private List<Product> relatedList = new ArrayList<>();
    private List<Order> fullOrderList = new ArrayList<>();
    private List<Order> displayOrderList = new ArrayList<>();
    private LinearLayout layoutEmptyOrders;
    private SwipeRefreshLayout swipeRefresh;
    private ChipGroup chipGroupStatus;
    private TextView tvCartBadge, tvSearchTrigger;
    
    // Search Overlay Views
    private View searchCardTrigger, layoutSearchOverlay;
    private EditText edtSearchOverlay;
    private ImageView btnBackSearch, btnClearSearch;
    private TextView tvBtnSearch;
    private RecyclerView rvSuggestions;
    private SearchSuggestionAdapter suggestionAdapter;
    private List<String> allProductNames = new ArrayList<>();
    private List<String> searchHistory = new ArrayList<>();
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration ordersListener;
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_oder);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadSearchHistory();
        initViews();
        setupInsets();
        setupAdapters();
        setupFilters();
        setupSearchLogic();
        startListeningOrders();
        updateCartBadge();
        fetchProductNamesForSuggestions();
    }

    private void setupInsets() {
        View mainView = findViewById(R.id.main_order_list);
        View topBar = findViewById(R.id.topBar);
        View searchHeader = findViewById(R.id.searchHeader);

        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, 0);
                return insets;
            });
        }
        if (topBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
        if (searchHeader != null) {
            ViewCompat.setOnApplyWindowInsetsListener(searchHeader, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
        loadSearchHistory();
        if (ordersListener == null) startListeningOrders();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListener != null) ordersListener.remove();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
                resetSearch();
            } else {
                finish();
            }
        });
        rvRelatedProducts = findViewById(R.id.rvRelatedProducts);
        rvOrders = findViewById(R.id.rvOrders);
        layoutEmptyOrders = findViewById(R.id.layoutEmptyOrders);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        chipGroupStatus = findViewById(R.id.chipGroupStatus);
        tvCartBadge = findViewById(R.id.tvCartBadge);
        tvSearchTrigger = findViewById(R.id.tvSearchTrigger);

        // Search Views
        searchCardTrigger = findViewById(R.id.searchCardTrigger);
        layoutSearchOverlay = findViewById(R.id.layoutSearchOverlay);
        edtSearchOverlay = findViewById(R.id.edtSearchOverlay);
        btnBackSearch = findViewById(R.id.btnBackSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        tvBtnSearch = findViewById(R.id.tvBtnSearch);
        rvSuggestions = findViewById(R.id.rvSuggestions);

        findViewById(R.id.layoutCart).setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::startListeningOrders);
        }

        findViewById(R.id.btnShopNow).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void setupSearchLogic() {
        suggestionAdapter = new SearchSuggestionAdapter(this::performSearch);
        suggestionAdapter.setOnDeleteHistoryListener(this::deleteHistoryItem);
        rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        rvSuggestions.setAdapter(suggestionAdapter);

        if (searchCardTrigger != null) {
            searchCardTrigger.setOnClickListener(v -> {
                layoutSearchOverlay.setVisibility(View.VISIBLE);
                edtSearchOverlay.requestFocus();
                showKeyboard(edtSearchOverlay);
                showSearchHistory();
            });
        }

        if (btnBackSearch != null) {
            btnBackSearch.setOnClickListener(v -> {
                layoutSearchOverlay.setVisibility(View.GONE);
                hideKeyboard(edtSearchOverlay);
                resetSearch();
            });
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                edtSearchOverlay.setText("");
                showSearchHistory();
            });
        }

        if (tvBtnSearch != null) {
            tvBtnSearch.setOnClickListener(v -> performSearch(edtSearchOverlay.getText().toString()));
        }

        if (edtSearchOverlay != null) {
            edtSearchOverlay.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString();
                    btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                    if (query.isEmpty()) showSearchHistory();
                    else updateSuggestions(query);
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            edtSearchOverlay.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(edtSearchOverlay.getText().toString());
                    return true;
                }
                return false;
            });
        }
    }

    private void resetSearch() {
        currentSearchQuery = "";
        edtSearchOverlay.setText("");
        if (tvSearchTrigger != null) tvSearchTrigger.setText("Tìm kiếm đơn hàng của bạn...");
        filterList("Tất cả");
        if (chipGroupStatus != null) chipGroupStatus.check(R.id.chipAll);
    }

    private void showSearchHistory() {
        if (searchHistory.isEmpty()) suggestionAdapter.setData(new ArrayList<>(), "");
        else suggestionAdapter.setData(searchHistory, "");
    }

    private void deleteHistoryItem(String item) {
        searchHistory.remove(item);
        saveSearchHistory();
        showSearchHistory();
    }

    private void updateSuggestions(String query) {
        if (query.isEmpty()) {
            showSearchHistory();
            return;
        }
        String normalizedQuery = removeAccent(query.toLowerCase().trim());
        List<String> filtered = allProductNames.stream()
                .filter(name -> removeAccent(name.toLowerCase()).contains(normalizedQuery))
                .collect(Collectors.toList());
        suggestionAdapter.setData(filtered, query);
    }

    private void performSearch(String query) {
        if (query.trim().isEmpty()) return;
        String q = query.trim();
        saveToHistory(q);
        currentSearchQuery = q;
        
        if (tvSearchTrigger != null) tvSearchTrigger.setText(q);
        if (chipGroupStatus != null) chipGroupStatus.clearCheck();
        
        layoutSearchOverlay.setVisibility(View.GONE);
        hideKeyboard(edtSearchOverlay);
        searchOrder(q);
    }

    private void saveToHistory(String query) {
        searchHistory.remove(query);
        searchHistory.add(0, query);
        if (searchHistory.size() > 10) searchHistory.remove(searchHistory.size() - 1);
        saveSearchHistory();
    }

    private void loadSearchHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_HISTORY, null);
        if (json != null) {
            Gson gson = new Gson();
            searchHistory = gson.fromJson(json, new TypeToken<List<String>>(){}.getType());
        } else {
            searchHistory = new ArrayList<>();
        }
    }

    private void saveSearchHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(searchHistory);
        editor.putString(KEY_HISTORY, json);
        editor.apply();
    }

    private String removeAccent(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }

    private void fetchProductNamesForSuggestions() {
        db.collection("products").get().addOnSuccessListener(queryDocumentSnapshots -> {
            allProductNames.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String name = doc.getString("name");
                if (name != null) allProductNames.add(name);
            }
        });
    }

    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onBackPressed() {
        if (layoutSearchOverlay != null && layoutSearchOverlay.getVisibility() == View.VISIBLE) {
            layoutSearchOverlay.setVisibility(View.GONE);
            resetSearch();
        } else if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
            resetSearch();
        } else {
            super.onBackPressed();
        }
    }

    private void startListeningOrders() {
        if (auth.getCurrentUser() == null) return;
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        
        String userId = auth.getCurrentUser().getUid();
        if (ordersListener != null) ordersListener.remove();
        
        ordersListener = db.collection("orders")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    if (error != null) return;
                    
                    if (value != null) {
                        fullOrderList.clear();
                        for (QueryDocumentSnapshot doc : value) {
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
                        
                        if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
                            searchOrder(currentSearchQuery);
                        } else {
                            filterList("Tất cả"); 
                        }
                        
                        if (!fullOrderList.isEmpty() && fullOrderList.get(0).getProductId() != null) {
                            fetchRelatedProducts(fullOrderList.get(0));
                        } else {
                            fetchDefaultRelated();
                        }
                    }
                });
    }

    private void updateCartBadge() {
        if (tvCartBadge == null || auth.getCurrentUser() == null) return;
        db.collection("carts")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalQuantity = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long qty = doc.getLong("quantity");
                        if (qty != null) totalQuantity += qty.intValue();
                    }
                    if (totalQuantity > 0) {
                        tvCartBadge.setVisibility(View.VISIBLE);
                        tvCartBadge.setText(String.valueOf(totalQuantity));
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
            currentSearchQuery = "";
            if (tvSearchTrigger != null) tvSearchTrigger.setText("Tìm kiếm đơn hàng của bạn...");
            
            String filter = "Tất cả";
            if (checkedId == R.id.chipPending) filter = "Chờ xác nhận";
            else if (checkedId == R.id.chipConfirmed) filter = "Đã xác nhận";
            else if (checkedId == R.id.chipShipping) filter = "Vận chuyển";
            else if (checkedId == R.id.chipDelivering) filter = "Đang giao";
            else if (checkedId == R.id.chipDelivered) filter = "Giao thành công";
            else if (checkedId == R.id.chipCancelled) filter = "Đã hủy";
            
            filterList(filter);
        });
    }

    private void searchOrder(String query) {
        if (query.isEmpty()) {
            filterList("Tất cả");
            return;
        }
        displayOrderList.clear();
        String normalizedQuery = removeAccent(query.toLowerCase().trim());
        String[] queryWords = normalizedQuery.split("\\s+");

        for (Order o : fullOrderList) {
            if (o.getProductName() == null) continue;
            String normalizedName = removeAccent(o.getProductName().toLowerCase());
            String orderId = (o.getId() != null) ? o.getId().toLowerCase() : "";

            boolean match = true;
            for (String word : queryWords) {
                if (!normalizedName.contains(word) && !orderId.contains(word)) {
                    match = false;
                    break;
                }
            }
            if (match) displayOrderList.add(o);
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

    private void fetchRelatedProducts(Order lastOrder) {
        if (lastOrder.getProductId() == null) return;
        db.collection("products").document(lastOrder.getProductId()).get().addOnSuccessListener(ds -> {
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
            h.tvItemProductName.setText(o.getProductName());
            h.tvItemDetails.setText("Số lượng: " + o.getQuantity());
            Glide.with(h.itemView.getContext()).load(o.getImageUrl()).placeholder(R.drawable.phone_mockup).into(h.imgItemProduct);

            String status = (o.getStatus() != null) ? o.getStatus().toLowerCase() : "";
            int iconRes = R.drawable.logistic;
            String description = "Kiện hàng đang được xử lý";
            String colorHex = "#1E3A8A";

            if (status.contains("chờ")) { iconRes = R.drawable.booking; description = "Đang chờ shop xác nhận đơn hàng"; }
            else if (status.contains("xác nhận")) { iconRes = R.drawable.checklist; description = "Đơn hàng đã được shop xác nhận"; }
            else if (status.contains("vận chuyển")) { iconRes = R.drawable.tracking; description = "Đơn hàng đã bàn giao cho đơn vị vận chuyển"; }
            else if (status.contains("đang giao")) { iconRes = R.drawable.shop; description = "Kiện hàng đang trên đường giao đến bạn"; }
            else if (status.contains("giao thành công")) { iconRes = R.drawable.product; description = "Đơn hàng đã được giao thành công"; }
            else if (status.contains("hủy")) { iconRes = R.drawable.reject; description = "Đơn hàng đã bị hủy"; colorHex = "#EF4444"; }

            if (h.imgStatusIcon != null) { h.imgStatusIcon.setImageResource(iconRes); h.imgStatusIcon.setColorFilter(Color.parseColor(colorHex)); }
            if (h.tvStatusDescription != null) h.tvStatusDescription.setText(description);

            long originalPrice = o.getTotalPrice() + o.getShippingFee();
            long finalPrice = o.getFinalPrice();
            if (finalPrice < originalPrice) {
                h.tvOriginalPrice.setVisibility(View.VISIBLE);
                h.tvOriginalPrice.setText(String.format(Locale.getDefault(), "%,dđ", originalPrice));
                h.tvOriginalPrice.setPaintFlags(h.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                h.tvItemTotalPrice.setText(String.format(Locale.getDefault(), "%,dđ", Math.max(0, finalPrice)));
            } else {
                h.tvOriginalPrice.setVisibility(View.GONE);
                h.tvItemTotalPrice.setText(String.format(Locale.getDefault(), "%,dđ", originalPrice));
            }
            
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(OderActivity.this, OrderDetailActivity.class);
                intent.putExtra("ORDER_ID", o.getId());
                startActivity(intent);
            });
            h.btnItemAddCart.setOnClickListener(v -> addOrderToCart(o));
            h.btnItemAction.setOnClickListener(v -> buyOrderAgain(o));
        }
        private void addOrderToCart(Order order) {
            if (auth.getCurrentUser() == null) return;
            db.collection("carts").add(new com.example.hitcapp.models.CartItem(auth.getUid(), order.getProductId(), order.getProductName(), "", order.getTotalPrice()/(order.getQuantity()>0?order.getQuantity():1), order.getTotalPrice()/(order.getQuantity()>0?order.getQuantity():1), order.getQuantity(), order.getImageUrl()));
            Toast.makeText(OderActivity.this, "Đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
            updateCartBadge();
        }
        private void buyOrderAgain(Order order) {
            if (auth.getCurrentUser() == null) return;
            ArrayList<com.example.hitcapp.models.CartItem> items = new ArrayList<>();
            items.add(new com.example.hitcapp.models.CartItem(auth.getUid(), order.getProductId(), order.getProductName(), "", order.getTotalPrice()/(order.getQuantity()>0?order.getQuantity():1), order.getTotalPrice()/(order.getQuantity()>0?order.getQuantity():1), order.getQuantity(), order.getImageUrl()));
            Intent intent = new Intent(OderActivity.this, PaymentActivity.class);
            intent.putExtra("SELECTED_ITEMS_DATA", items);
            startActivity(intent);
        }
        @Override public int getItemCount() { return list.size(); }
        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView tvOrderDate, tvItemStatus, tvItemProductName, tvItemDetails, tvItemTotalPrice, tvOriginalPrice, tvStatusDescription;
            ImageView imgItemProduct, btnItemAddCart, imgStatusIcon;
            MaterialButton btnItemAction;
            OrderViewHolder(View v) {
                super(v);
                tvOrderDate = v.findViewById(R.id.tvOrderDate);
                tvItemStatus = v.findViewById(R.id.tvItemStatus);
                tvItemProductName = v.findViewById(R.id.tvItemProductName);
                tvItemDetails = v.findViewById(R.id.tvItemDetails);
                tvItemTotalPrice = v.findViewById(R.id.tvItemTotalPrice);
                tvOriginalPrice = v.findViewById(R.id.tvOriginalPrice);
                tvStatusDescription = v.findViewById(R.id.tvStatusDescription);
                imgItemProduct = v.findViewById(R.id.imgItemProduct);
                btnItemAddCart = v.findViewById(R.id.btnItemAddCart);
                btnItemAction = v.findViewById(R.id.btnItemAction);
                imgStatusIcon = v.findViewById(R.id.imgStatusIcon);
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
