// ProductAdapter.java
package com.example.eyalproject.Models;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.eyalproject.Models.Product;
import com.example.eyalproject.R;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> productList;

    public ProductAdapter(List<Product> productList) {
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void updateProducts(List<Product> products) {
        this.productList = products;
        notifyDataSetChanged();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private TextView productName;
        private TextView productPrice;
        private TextView productCategory;
        private ImageView productImage;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            productCategory = itemView.findViewById(R.id.productCategory);
            productImage = itemView.findViewById(R.id.productImage);
        }

        public void bind(Product product) {
            productName.setText(product.getName());
            productPrice.setText(product.getFormattedPrice());
            productCategory.setText(product.getCategory());

            // Set category-based images (you'll need to add these drawables)
            switch (product.getCategory().toLowerCase()) {
                case "electronics":
                    productImage.setImageResource(R.drawable.iconapp);
                    break;
                case "clothing":
                    productImage.setImageResource(R.drawable.iconapp);
                    break;
                case "home":
                    productImage.setImageResource(R.drawable.iconapp);
                    break;
                case "sports":
                    productImage.setImageResource(R.drawable.iconapp);
                    break;
                default:
                    productImage.setImageResource(R.drawable.iconapp);
            }
        }
    }
}