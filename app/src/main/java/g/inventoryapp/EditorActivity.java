package g.inventoryapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import g.inventoryapp.data.ProductContract;
import g.inventoryapp.data.ProductContract.ProductEntry;

public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_PRODUCT_LOADER = 0;
    private Uri mCurrentProductUri;
    private EditText mNameEditText;
    private ImageView mImageView;
    private EditText mEditPrice;
    private TextView mTextViewQuantity;
    private int mQuantity;
    private Button mButtonPlus;
    private Button mButtonMinus;
    private Button mOrderButton;
    private EditText mCustomerName;
    private EditText mEmailField;
    private Uri imageUri;
    private boolean mProductHasChanged = false;
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        if (mCurrentProductUri == null) {
            setTitle(getString(R.string.editor_activity_title_new_product));
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.editor_activity_title_edit_product));

            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        // Views
        mNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mImageView = (ImageView) findViewById(R.id.image_view);
        mEditPrice = (EditText) findViewById(R.id.edit_price);
        mOrderButton = (Button) findViewById(R.id.order_button);
        mTextViewQuantity = (TextView) findViewById(R.id.edit_text_quantity);
        mButtonPlus = (Button) findViewById(R.id.plus_button);
        mButtonMinus = (Button) findViewById(R.id.minus_button);
        mCustomerName = (EditText) findViewById(R.id.edit_text_customer_name);
        mEmailField = (EditText) findViewById(R.id.edit_text_customer_email);

        // Listeners
        mNameEditText.setOnTouchListener(mTouchListener);
        mEditPrice.setOnTouchListener(mTouchListener);
        mTextViewQuantity.setOnTouchListener(mTouchListener);
        mButtonPlus.setOnTouchListener(mTouchListener);
        mButtonMinus.setOnTouchListener(mTouchListener);
        mCustomerName.setOnTouchListener(mTouchListener);
        mEmailField.setOnTouchListener(mTouchListener);
        mImageView.setOnTouchListener(mTouchListener);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProductHasChanged = true;
                trySelectImage();
            }
        });

        mButtonMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mQuantity = mQuantity - 1;
                refreshQuantity();
            }
        });

        mButtonPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mQuantity = mQuantity + 1;
                refreshQuantity();
            }
        });

        mOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                orderProducts();
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    private boolean saveProduct() {
        String nameString = mNameEditText.getText().toString().trim();
        String priceString = mEditPrice.getText().toString().trim();
        String quantityString = mTextViewQuantity.getText().toString().trim();
        String customerString = mCustomerName.getText().toString().trim();
        String emailString = mEmailField.getText().toString().trim();

        ContentValues values = new ContentValues();

        if (TextUtils.isEmpty(nameString)) {
            Toast.makeText(this, getString(R.string.product_name_required), Toast.LENGTH_SHORT).show();
            return false;
        }
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);

        if (TextUtils.isEmpty(priceString)) {
            Toast.makeText(this, getString(R.string.product_price_required), Toast.LENGTH_SHORT).show();
            return false;
        }
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, priceString);

        if (TextUtils.isEmpty(quantityString)) {
            Toast.makeText(this, getString(R.string.product_quantity_required), Toast.LENGTH_SHORT).show();
            return false;
        }

        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantityString);

        if (imageUri == null) {
            Toast.makeText(this, getString(R.string.product_picture_required), Toast.LENGTH_SHORT).show();
            return false;
        }
        values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, imageUri.toString());

        if (TextUtils.isEmpty(customerString)) {
            Toast.makeText(this, getString(R.string.customer_name_required), Toast.LENGTH_SHORT).show();
            return false;
        }
        values.put(ProductEntry.COLUMN_CUSTOMER_NAME, customerString);

        if (TextUtils.isEmpty(emailString)) {
            Toast.makeText(this, getString(R.string.customer_email_required), Toast.LENGTH_SHORT).show();
            return false;
        }
        values.put(ProductEntry.COLUMN_CUSTOMER_EMAIL, emailString);

        // Determine if this is a new or existing product by checking if mCurrentProductUri is null or not
        if (mCurrentProductUri == null) {
            Uri newUri = getContentResolver().insert(ProductContract.ProductEntry.CONTENT_URI, values);
            if (newUri == null) {
                Toast.makeText(this, getString(R.string.editor_insert_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);
            if (rowsAffected == 0) {
                Toast.makeText(this, getString(R.string.editor_update_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_update_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                if (saveProduct()) {
                    finish();
                }
                return true;
            case R.id.action_delete:
                confirmDeleteDialog();
                return true;
            case android.R.id.home:
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                unsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                };
        unsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_IMAGE,
                ProductEntry.COLUMN_CUSTOMER_NAME,
                ProductEntry.COLUMN_CUSTOMER_EMAIL};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentProductUri,         // Query the content URI for the current product
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if (cursor.moveToFirst()) {

            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int pictureColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);
            int customerColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_CUSTOMER_NAME);
            int emailColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_CUSTOMER_EMAIL);

            String name = cursor.getString(nameColumnIndex);
            String price = cursor.getString(priceColumnIndex);
            mQuantity = cursor.getInt(quantityColumnIndex);
            String imageUriString = cursor.getString(pictureColumnIndex);
            String customer = cursor.getString(customerColumnIndex);
            String email = cursor.getString(emailColumnIndex);

            mNameEditText.setText(name);
            mEditPrice.setText(price);
            mTextViewQuantity.setText(Integer.toString(mQuantity));
            imageUri = Uri.parse(imageUriString);
            mImageView.setImageURI(imageUri);
            mCustomerName.setText(customer);
            mEmailField.setText(email);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.setText("");
        mEditPrice.setText("");
        mTextViewQuantity.setText("");
        mCustomerName.setText("");
        mEmailField.setText("");
    }

    private void deleteProduct() {
        if (mCurrentProductUri != null) {
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);
            if (rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                imageUri = data.getData();
                Picasso.with(EditorActivity.this).load(data.getData()).into((mImageView));

                mImageView.setImageURI(imageUri);
                mImageView.invalidate();
            }
        }
    }

    public void trySelectImage() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        openImage();
    }

    private void openImage() {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        intent.setType("/Image/*");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.selectPicture)), 0);
    }

    private void unsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void confirmDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteProduct();
            }
        });

        builder.setNegativeButton(R.string.cancel, null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void orderProducts() {
        Intent intent = new Intent(android.content.Intent.ACTION_SENDTO);
        intent.setType("text/plain");
        intent.setData(Uri.parse("mailto:" + mEmailField.getText().toString().trim()));
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "New Order");
        String message = "Hello, we need " + mNameEditText.getText().toString().trim();
        intent.putExtra(android.content.Intent.EXTRA_TEXT, message);
        startActivity(intent);
    }

    public void refreshQuantity() {
        mTextViewQuantity.setText(String.valueOf(mQuantity));
    }
}