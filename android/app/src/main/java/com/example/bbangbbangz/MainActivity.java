package com.example.bbangbbangz;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String MODEL_PATH = "optimized_graph.tflite";
    private static final boolean QUANT = false;
    private static final String LABEL_PATH = "retrained_labels.txt";
    private static final int INPUT_SIZE = 224;

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();

    public static final String FILE_NAME = "temp.jpg";

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;

    private ImageView mMainImage;
    private TextView textView;
    String answer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());

        ImageButton CameraButton= (ImageButton) findViewById(R.id.cameraButton);
        CameraButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                startCamera();
            }
        });

        ImageButton GalleryButton=(ImageButton) findViewById(R.id.galleryButton);
        GalleryButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                startGalleryChooser();
            }
        });

        ImageButton CartButton=(ImageButton) findViewById(R.id.cartButton);
        CartButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (answer == null) Toast.makeText(getApplicationContext(), "Select the photo or take a picture.", Toast.LENGTH_LONG).show();
                else goShop();
            }
        });

        mMainImage = findViewById(R.id.main_image);
        initTensorFlowAndLoadModel();
    }

    public void goShop() {
        if (answer.contains("acne")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.acnestudios.com/kr/en/home")));
        } else if (answer.contains("adidas")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://shop.adidas.co.kr/adiMain.action?NFN_ST=Y")));
        } else if (answer.contains("champion")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://joes-shop.championusa.kr")));
        } else if (answer.contains("cath")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.cathkidstonkorea.co.kr/main/index.asp")));
        } else if (answer.contains("celine")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.celine.com/en-int/home")));
        } else if (answer.contains("muji")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.mujikorea.net/display/displayShop.lecs?storeNo=1&siteNo=13013&displayNo=MJ1A84&displayMallNo=MJ1")));
        } else if (answer.contains("nike")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.nike.com/kr/ko_kr/")));
        } else if (answer.contains("prada")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.prada.com/en.html?cc=AP")));
        } else if (answer.contains("stussy")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.stussy.co.kr/")));
        } else if (answer.contains("toms")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.toms.com/")));
        } else if (answer.contains("north")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.thenorthfacekorea.co.kr/")));
        } else if (answer.contains("browne")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.thombrowne.co.kr/public/display/main/view")));
        } else if (answer.contains("hilfiger")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.hfashionmall.com/sfmweb/display/display.do?screenNumber=1104&categoryNumber=10500&utm_source=naver&utm_medium=display&utm_campaign=pc_brand&utm_content=th_main")));
        } else if (answer.contains("ysl")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ysl.com/kr")));
        }
    }

    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }

    public void startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            originuploadImage(data.getData());
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            originuploadImage(photoUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
            case GALLERY_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, GALLERY_PERMISSIONS_REQUEST, grantResults)) {
                    startGalleryChooser();
                }
                break;
        }
    }

    public Bitmap rotate(Bitmap bitmap, int degrees) {
        if (degrees != 0 && bitmap != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees);
            Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
            if (bitmap != converted) {
                bitmap = null;
                bitmap = converted;
                converted = null;
            }
        }
        return bitmap;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_PATH,
                            LABEL_PATH,
                            INPUT_SIZE,
                            QUANT);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    public void originuploadImage(Uri uri) {
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
                mMainImage.setImageBitmap(bitmap);
                final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);

                answer = results.toString();
                if (answer.contains("nike")) {
                    String newanswer = "Nike";
                    textView.setText(newanswer);
                } else if (answer.contains("celine")) {
                    String newanswer = "Celine";
                    textView.setText(newanswer);
                } else if (answer.contains("adidas")) {
                    String newanswer = "Adidas";
                    textView.setText(newanswer);
                } else if (answer.contains("muji")) {
                    String newanswer = "MUJI";
                    textView.setText(newanswer);
                } else if (answer.contains("cath")) {
                    String newanswer = "Cath Kidston";
                    textView.setText(newanswer);
                } else if (answer.contains("tommy")) {
                    String newanswer = "Tommy Hilfiger";
                    textView.setText(newanswer);
                } else if (answer.contains("ysl")) {
                    String newanswer = "YSL";
                    textView.setText(newanswer);
                } else if (answer.contains("acne")) {
                    String newanswer = "Acne Studios";
                    textView.setText(newanswer);
                } else if (answer.contains("champion")) {
                    String newanswer = "Champion";
                    textView.setText(newanswer);
                } else if (answer.contains("toms")) {
                    String newanswer = "Toms";
                    textView.setText(newanswer);
                } else if (answer.contains("north")) {
                    String newanswer = "The North Face";
                    textView.setText(newanswer);
                } else if (answer.contains("thom")) {
                    String newanswer = "Thom Browne";
                    textView.setText(newanswer);
                } else if (answer.contains("stussy")) {
                    String newanswer = "Stussy";
                    textView.setText(newanswer);
                } else if (answer.contains("prada")) {
                    String newanswer = "Prada";
                    textView.setText(newanswer);
                } else textView.setText(answer);

            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
        }
    }
}
