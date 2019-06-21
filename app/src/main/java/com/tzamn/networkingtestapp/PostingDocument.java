package com.tzamn.networkingtestapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.ContentValues.TAG;

public class PostingDocument extends AppCompatActivity {
    private int REQUEST_CODE_CAMERA = 100;
    private int PERMISSIONS_REQUEST_CODE = 101;
    private ImageView previewImageview;
    private Button openCameraButton, postButton;
    private String mCurrentPhotoPath;
    private Bitmap mImageBitmap;
    private String post_document_URL = "http://18.191.252.162/documentRecord";
    private ProgressDialog progressDialog;
    private String fileName = "";
    private String session;
    private File fileToPost;
    private byte[] byteArrayPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posting_document);
        initComponents();
        eventListeners();
    }

    private void convertImageToPDF() {
        try {
            File root = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Tiedcomm Posts");
            if (!root.exists()) {
                root.mkdir();
            }
            //create-delete file
            fileToPost = new File(root, fileName);
            if (fileToPost.exists()) {
                fileToPost.delete();
            }
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(fileToPost));
            document.open();
            byteArrayPost = getBytesFromBitmap(mImageBitmap);
            Image image = Image.getInstance(byteArrayPost);
            float scaler = ((document.getPageSize().getWidth() - document.leftMargin()
                    - document.rightMargin() - 0) / image.getWidth()) * 50; // 0 means you have no indentation. If you have any, change it.
            image.scalePercent(scaler);
            image.setAlignment(Image.ALIGN_CENTER | Image.ALIGN_TOP);
            document.add(image);

            document.close();

            fileName = "document.pdf";
            byteArrayPost = readFile(fileToPost, null, "file");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public byte[] readFile(File file, Uri uri, String type) throws IOException {
        ByteArrayOutputStream ous = null;
        InputStream ios = null;
        try {
            byte[] buffer = new byte[8192];
            ous = new ByteArrayOutputStream();
            if (type.equals("uri"))
                ios = getContentResolver().openInputStream(uri);
            else
                ios = new FileInputStream(file);
            int read;
            while ((read = ios.read(buffer)) != -1) {
                ous.write(buffer, 0, read);
            }
        } finally {
            try {
                if (ous != null)
                    ous.close();
            } catch (IOException e) {
            }

            try {
                if (ios != null)
                    ios.close();
            } catch (IOException e) {
            }
        }
        return ous.toByteArray();
    }

    private void eventListeners() {
        openCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions();
            }
        });
        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                preparePostDocument();
            }
        });
    }

    private void preparePostDocument() {
        if (mImageBitmap != null) {
            postDocument();

        }
        /*
        if (writeFile()) {
            Toast.makeText(this, "Creaccion correcta.", Toast.LENGTH_SHORT).show();
            if (!fileName.isEmpty())
                postDocument();
        } else
            Toast.makeText(this, "Error creando el documento.", Toast.LENGTH_SHORT).show();
            */
    }

    private boolean writeFile() {
        try {
            byteArrayPost = getBytesFromBitmap(mImageBitmap);
            return true;
        } catch (Exception e) {
            return false;
        }
        /*
        try {
            //get dir
            File root = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Tiedcomm Posts");
            if (!root.exists()) {
                root.mkdir();
            }
            //create-delete file
            fileToPost = new File(root, fileName);
            if (fileToPost.exists()) {
                fileToPost.delete();
            }
            //save file
            OutputStream os = new FileOutputStream(fileToPost);
            os.write(getBytesFromBitmap(mImageBitmap));
            os.close();
            return true;
        } catch (IOException e) {
            return false;
        }
        */
    }

    private void openCamera() {
        Intent cameraIntent = new Intent("android.media.action.IMAGE_CAPTURE");
        //Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error:´" + ex.getMessage());
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.tzamn.networkingtestapp", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "captured-image";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        if (!image.exists()) {
            try {
                image.createNewFile();
            } catch (IOException e) {

            }
        } else {
            image.delete();
            try {
                image.createNewFile();
            } catch (IOException e) {
            }
        }
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    public static Bitmap modifyOrientation(Bitmap bitmap, String image_absolute_path) throws IOException {
        ExifInterface ei = new ExifInterface(image_absolute_path);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotate(bitmap, 90);

            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotate(bitmap, 180);

            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotate(bitmap, 270);

            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return flip(bitmap, true, false);

            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return flip(bitmap, false, true);

            default:
                return bitmap;
        }
    }

    public static Bitmap rotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap flip(Bitmap bitmap, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();
        matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void initComponents() {
        session = getIntent().getExtras().getString("session");
        previewImageview = findViewById(R.id.imageViewPreview);
        openCameraButton = findViewById(R.id.buttonOpenCamera);
        postButton = findViewById(R.id.buttonPost);
    }

    private void requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            }
        } else {
            openCamera();
        }
    }

    void postDocument() {

        convertImageToPDF();

        String mediaType = "*";
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("newVersion", "false")
                .addFormDataPart("nested", "false")
                .addFormDataPart("isCompliance", "false")
                .addFormDataPart("nestedOrder", "null")
                .addFormDataPart("_id", "5cfabdc697c3377520141869")
                .addFormDataPart("idRecord", "5cfabd5397c3377520141863")
                .addFormDataPart("idDocType", "5c6b3101e5da49cd7ec29879")
                .addFormDataPart("idDocument", "5cfabdc697c3377520141869")
                .addFormDataPart("idDLT", "5c6b3101e5da49cd7ec29882")
                .addFormDataPart("linkedTo", "[\"5c6b3101e5da49cd7ec29882\"]")
                .addFormDataPart("uploadFile", fileName,
                        RequestBody.create(MediaType.parse(mediaType), byteArrayPost))
                .build();

        Request request = new Request.Builder()
                .header("cookie", "session=" + session)
                .url(post_document_URL)
                .post(requestBody)
                .build();

        displayProgresDialog("Subiendo", "Espere...");
        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int responseCode = response.code();
                final String myResponse = response.body().string();

                PostingDocument.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog.isShowing())
                            progressDialog.dismiss();

                        if (responseCode == 200) {
                            if (extractMessage(myResponse))
                                displaySuccess();
                        }
                    }
                });

            }
        });
    }

    private void displaySuccess() {
        Toast.makeText(PostingDocument.this, "Documento sub", Toast.LENGTH_SHORT).show();
        previewImageview.setBackgroundColor(Color.TRANSPARENT);
        postButton.setVisibility(View.INVISIBLE);
    }

    private boolean extractMessage(String myResponse) {
        try {
            JSONObject resp = new JSONObject(myResponse);
            if (resp.getString("type").equals("success"))
                return true;
            else
                return false;
        } catch (JSONException e) {
            return false;
        }
    }

    private void displayProgresDialog(String title, String message) {
        progressDialog = ProgressDialog.show(PostingDocument.this, title,
                message, true);
        progressDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Necesitas activar los permisos para continuar.", Toast.LENGTH_SHORT).show();
            }
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_CODE_CAMERA == requestCode) {
            if (resultCode == RESULT_OK) {
                try {
                    mImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(mCurrentPhotoPath));
                    try {
                        String realPath = PathUtils.getPath(this, Uri.parse(mCurrentPhotoPath));
                        mImageBitmap = modifyOrientation(mImageBitmap, realPath);
                    } catch (Exception e) {
                        Log.e("Error", e.getMessage());
                    }
                    Uri uri = Uri.parse(mCurrentPhotoPath);
                    fileName = uri.getLastPathSegment();
                    previewImageview.setImageBitmap(mImageBitmap);

                } catch (IOException e) {
                    Log.e("Error", e.getMessage());
                }
            }
        }
    }

    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

}
