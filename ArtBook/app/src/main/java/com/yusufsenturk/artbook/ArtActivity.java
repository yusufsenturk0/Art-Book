package com.yusufsenturk.artbook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.yusufsenturk.artbook.databinding.ActivityArtBinding;

import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {
    private ActivityArtBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    Bitmap selectedImage;

    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityArtBinding.inflate(getLayoutInflater());
        View view=binding.getRoot();
        setContentView(view);

        registerLauncher();
        database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);


        Intent intent=getIntent();
        String info=intent.getStringExtra("info");

        if(info.equals("new")){
            binding.nameText.setText("");
            binding.artistText.setText("");
            binding.yearText.setText("");
            binding.button.setVisibility(View.VISIBLE);
            binding.button2.setVisibility(View.INVISIBLE);

            binding.imageView.setImageResource(R.drawable.selectimage);

            //new art
        }else{
            int artId=intent.getIntExtra("artId",1);
            binding.button.setVisibility(View.INVISIBLE);
            binding.button2.setVisibility(View.VISIBLE);

            try{
                Cursor cursor=database.rawQuery("SELECT * FROM arts WHERE id = ?",new String[] {String.valueOf(artId)});

                int artNameIx=cursor.getColumnIndex("artname");
                int painterNameIx=cursor.getColumnIndex("paintername");
                int yearIx=cursor.getColumnIndex("year");
                int imageIx=cursor.getColumnIndex("image");

                while(cursor.moveToNext()){

                    binding.nameText.setText(cursor.getString(artNameIx));
                    binding.artistText.setText(cursor.getString(painterNameIx));
                    binding.yearText.setText(cursor.getString(yearIx));


                    //fotoyu database e byte olarak kaydediyoz burda onu bitmap a çevirip iamgeView a atıyoruz
                    byte[] bytes=cursor.getBlob(imageIx);

                    Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    binding.imageView.setImageBitmap(bitmap);
                }


                cursor.close();
            }catch (Exception e){
                e.printStackTrace();
            }


        }

    }
    public void Save(View view){

        String name=binding.nameText.getText().toString();
        String artistName=binding.artistText.getText().toString();
        String year=binding.yearText.getText().toString();

        Bitmap smallImage=makeSmallerImage(selectedImage,300);

        //veri tabanına kydetmek içi 1ve 0 lara çevirmemiz lazım resimi
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray= outputStream.toByteArray();

        try{



            database=this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, paintername VARCHAR, year VARCHAR, image BLOB)");

            String sqlString="INSERT INTO arts (artname, paintername, year, image) VALUES(?, ?, ?, ?)";
            SQLiteStatement sqLiteStatement=database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,name);
            sqLiteStatement.bindString(2,artistName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();


        }catch (Exception e){
            e.printStackTrace();
        }

        Intent intent=new Intent(ArtActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);


    }
    //sqLite a kaydedilecek resimlerin boyutları küçük olmalı bu nethod resimlerin boyutlarını küçültüyor.
    public Bitmap makeSmallerImage(Bitmap image,int maximumSize){
        int width=image.getWidth();
        int height=image.getHeight();

        float bitmapRatio=(float)width/(float)height;
        //bitmapRatio 1 den büyükse yatay küçükse dikey bir görsel var demektir

        if(bitmapRatio>1){
            //yatay görsel
            width=maximumSize;
            height=(int)(width/bitmapRatio);

        }else{
            //dikey görsel
            height=maximumSize;
            width=(int)(height*bitmapRatio);
        }

        return image.createScaledBitmap(image,width,height,true);
    }



    public void selectImage(View view){
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
            //android sdk 33 ve üstü için READ_MEDIA_IMAGES

            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)!= PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_MEDIA_IMAGES)){

                    Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                        }
                    }).show();
                }else{
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                }

                //izin verilmedi
            }else{
                Intent intentToGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncher.launch(intentToGallery);

            }

        }
        else{
            //android  32 ve altı için READ_EXTARNEL_STORAGE

            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){

                    Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                        }
                    }).show();
                }else{
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }

                //izin verilmedi
            }else{
                Intent intentToGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncher.launch(intentToGallery);

            }
        }




    }

    private void registerLauncher(){
        activityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode()==RESULT_OK){
                    Intent intentFromResult=result.getData();
                    if(intentFromResult!=null){
                        Uri imageData= intentFromResult.getData();
                        try{

                            ImageDecoder.Source source=ImageDecoder.createSource(getContentResolver(),imageData);
                            selectedImage=ImageDecoder.decodeBitmap(source);
                            binding.imageView.setImageBitmap(selectedImage);




                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        });



        permissionLauncher=registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    Intent intentToGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);

                }else{
                    Toast.makeText(ArtActivity.this, "Permission needed", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    public void Delete(View view) {
        String atArtname=binding.nameText.getText().toString();
        database.execSQL("DELETE FROM arts WHERE artname= ?",new String[] {atArtname});

        Intent intent=new Intent(this,MainActivity.class);
        startActivity(intent);
    }
}