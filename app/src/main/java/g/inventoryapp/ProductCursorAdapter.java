package g.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import g.inventoryapp.data.ProductContract;

public class ProductCursorAdapter extends CursorAdapter {

    int mQuantity;
    private ImageView mImageView;
    //private Button mEditBuyButton;
    private CatalogActivity activity;

    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
        activity = (CatalogActivity) context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final long id;

        id = cursor.getLong(cursor.getColumnIndex(ProductContract.ProductEntry._ID));
        mImageView = (ImageView) view.findViewById(R.id.image_view_buy);
        //mEditBuyButton = (Button) view.findViewById(R.id.button_view_buy);
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView priceTextView = (TextView) view.findViewById(R.id.text_view_price);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.text_view_quantity);
        ImageView imageView = (ImageView) view.findViewById(R.id.image_view_list);

        int nameColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRODUCT_NAME);
        int priceColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRODUCT_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRODUCT_QUANTITY);
        int imageColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRODUCT_IMAGE);

        String productName = cursor.getString(nameColumnIndex);
        String productPrice = cursor.getString(priceColumnIndex);
        String quantity = cursor.getString(quantityColumnIndex);
        String productImage = cursor.getString(imageColumnIndex);

        mQuantity = Integer.parseInt(quantity);
        final Uri currentProductUri = ContentUris.withAppendedId(ProductContract.ProductEntry.CONTENT_URI, id);

        Uri imageUri = Uri.parse(productImage);
        imageView.setImageURI(imageUri);
        imageView.invalidate();

        if (TextUtils.isEmpty(productPrice)) {
            productPrice = context.getString(R.string.unknown_price);
        }

        nameTextView.setText(productName);

        priceTextView.setText("PRICE " + productPrice + "$");
        quantityTextView.setText("QUANTITY " + quantity);

        nameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.onItemClick(id);
            }
        });

        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentValues values = new ContentValues();
                if (mQuantity > 0) {
                    int newQuanity = --mQuantity;
                    values.put(ProductContract.ProductEntry.COLUMN_PRODUCT_QUANTITY, newQuanity);
                    context.getContentResolver().update(currentProductUri, values, null, null);
                    quantityTextView.setText("QUANTITY " + newQuanity);
                } else {
                    Toast.makeText(context, "Quantity Unavailable", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}