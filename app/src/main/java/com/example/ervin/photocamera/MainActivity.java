package com.example.ervin.photocamera;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.kosalgeek.android.photoutil.ImageBase64;
import com.kosalgeek.android.photoutil.ImageLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST = 1100;
    private static final int GALLERY_REQUEST = 1002;
    String ipSaya = "http://10.0.251.219:8088/php/Belajar/belajarUpload.php";


    Button camera;
    Button gallery;
    Button save;

    ImageView mImageView;
    Uri file_uri;
    private String encoded_string, image_name,imagePath;
    private Bitmap imgBitmap;

    AlertDialog.Builder builder;
    AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        camera = findViewById(R.id.testCamera);
        gallery = findViewById(R.id.testGallery);
        save = findViewById(R.id.testSave);
        builder = new AlertDialog.Builder(this);
        file_uri=null;

        Toast.makeText(this, "misalnya gw tambah", Toast.LENGTH_SHORT).show();
        
        //cek permission
        checkPermission();
        mImageView = findViewById(R.id.justIcon);
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCamera();
            }
        });
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmSave();
            }
        });
    }
    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if(checkSelfPermission(Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED
                    &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED
                    &&
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
            }else{
                Toast.makeText(this, "Please open Say YESSSS", Toast.LENGTH_SHORT).show();

                if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(this,new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                    },CAMERA_REQUEST);
                    return;
                }

            }
        }
    }

    private void openGallery() {

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N){
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), GALLERY_REQUEST  );
        }else{
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            startActivityForResult(intent, GALLERY_REQUEST);
        }
    }


    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            file_uri = Uri.fromFile(createImageFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, file_uri);

        } else if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
            if(intent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                }catch (Exception e){
                    Toast.makeText(this, "Error photo file Nougat", Toast.LENGTH_SHORT).show();
                }
                if(photoFile!=null) {
                    Uri photoURI = FileProvider.getUriForFile(this, getPackageName() +".provider", photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(intent, CAMERA_REQUEST);
                }
            }
        }else{
            intent.putExtra(MediaStore.EXTRA_OUTPUT, getFileUriCamera());
            Toast.makeText(this, file_uri.getPath().toString(), Toast.LENGTH_SHORT).show();
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (intent.resolveActivity(getApplicationContext().getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST);
        }
    }

    public Uri getFileUriCamera() {
        //String authorize = getApplicationContext().getPackageName()+".fileprovider";
        String authorize = BuildConfig.APPLICATION_ID + ".provider";
        file_uri = FileProvider.getUriForFile(MainActivity.this,
                authorize,
                createImageFile());
        return file_uri;
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
//        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_DCIM), "Camera");
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(imageFileName,".jpg",storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        imagePath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK ) {
            try{
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N){
                    imagePath = file_uri.getPath();
                    Toast.makeText(this, imagePath+"", Toast.LENGTH_SHORT).show();
                    //Bitmap imageBitmap = BitmapFactory.decodeFile(file_uri.getPath());
                    Bitmap img = ImageLoader.init().from(imagePath).requestSize(512,512).getBitmap();
                    mImageView.setImageBitmap(img);
                    Log.d("yg dishow camera",imagePath);
                }else if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
                    Toast.makeText(this, imagePath+"", Toast.LENGTH_SHORT).show();
                    Glide.with(this).load(imagePath).into(mImageView);
                }

            }catch (Exception e){
                Toast.makeText(this,"Error camera", Toast.LENGTH_SHORT).show();
                Log.d("camera_error",e.getMessage());
            }
           
        }else if (resultCode == RESULT_OK &&requestCode == GALLERY_REQUEST){
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                try {
                    file_uri = data.getData();
                    imagePath = getRealPathFromURI(file_uri);
                    Bitmap img = BitmapFactory.decodeFile(imagePath);
                    mImageView.setImageBitmap(img);
                    Log.d("yg dishow gallery", imagePath);
                } catch (Exception e) {
                    Toast.makeText(this, "error gallery!", Toast.LENGTH_SHORT).show();
                }
            }else{
                try {
                    Uri fileUri = data.getData();
                    imagePath = getRealPathFromURI(fileUri);
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fileUri);
                    Log.d("uri_path",imagePath);
                    mImageView.setImageBitmap(bitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "error gallery!", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };

        CursorLoader cursorLoader = new CursorLoader(
                this,
                contentUri, proj, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();

        int column_index =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private void confirmSave() {
        builder.setMessage("Are you sure want to save ?");
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
               if(imagePath!=null){
                   Toast.makeText(MainActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                   new Encode_image().execute();
               }else {
                   Toast.makeText(MainActivity.this, "file tidak ada", Toast.LENGTH_SHORT).show();
               }

                return;
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        dialog = builder.show();
    }

    private class Encode_image extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {

            try {

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    imgBitmap = ImageLoader.init().from(imagePath).requestSize(512,512).getBitmap();
                    encoded_string = ImageBase64.encode(imgBitmap);
                } else if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
                    File file = new File(imagePath);
                    InputStream ims = new FileInputStream(file);
                    imgBitmap = BitmapFactory.decodeStream(ims);
                    encoded_string = ImageBase64.encode(imgBitmap);
                }

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                image_name = timeStamp+".jpg";
                Log.d("tagg",encoded_string);
                Log.d("tagg",image_name);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

           return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            makeRequest();
        }
    }

    private void makeRequest() {
        StringRequest req = new StringRequest(Request.Method.POST, ipSaya,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(MainActivity.this, "Berhasil respond", Toast.LENGTH_LONG).show();

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, "Error response", Toast.LENGTH_SHORT).show();
                    }
                }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> map = new HashMap<>();
                map.put("encoded_string",encoded_string);
                map.put("image_name",image_name);
                return map;
            }
        };
        Volley.newRequestQueue(MainActivity.this).add(req);
    }

}
