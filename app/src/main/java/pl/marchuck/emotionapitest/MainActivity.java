package pl.marchuck.emotionapitest;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pixelcan.emotionanalysisapi.ApiService;
import com.pixelcan.emotionanalysisapi.EmotionRestClient;
import com.pixelcan.emotionanalysisapi.ResponseCallback;
import com.pixelcan.emotionanalysisapi.models.FaceAnalysis;
import com.pixelcan.emotionanalysisapi.util.NetworkUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    final static int SELECT_PHOTO = 666;
    Context context;
    ResponseCallback callback;
    Bitmap scaledBMP;
    ImageView imageView;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
        context = this;
        imageView = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.textView1);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .show();
            }
        });
        EmotionRestClient.init(this, "ad296dfd81de4130b011d6d808b877b4");
    }

    public void detect(Bitmap bmp, final ResponseCallback callback) {
        if (!NetworkUtils.hasInternetConnection(context)) {
            callback.onError(context.getString(com.pixelcan.emotionanalysisapi.R.string.no_internet_connection));
            return;
        }
        // convert the image to bytes array
        //byte[] data;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] data = stream.toByteArray();

        RequestBody requestBody = RequestBody
                .create(MediaType.parse("application/octet-stream"), data);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.projectoxford.ai/emotion/v1.0/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Service setup

        ApiService apiService = retrofit.create(ApiService.class);
        Call<FaceAnalysis[]> call = apiService.analyzePicture("ad296dfd81de4130b011d6d808b877b4", requestBody);
        call.enqueue(new Callback<FaceAnalysis[]>() {
            @Override
            public void onResponse(Response<FaceAnalysis[]> response) {
                if (response.isSuccess()) {
                    // request successful (status code 200, 201)
                    FaceAnalysis[] result = response.body();
                    if (result != null) {
                        callback.onSuccess(result);
                    } else {
                        callback.onError(response.message());
                    }
                } else {
                    //request not successful (like 400,401,403 etc)
                    try {
                        callback.onError(response.errorBody().string());
                    } catch (IOException e) {
                        callback.onError(response.message());
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getContentResolver().query(
                            selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();

                    final Bitmap yourSelectedImage = BitmapFactory.decodeFile(filePath);
                    scaledBMP = Bitmap.createScaledBitmap(yourSelectedImage, 256, 256, false);

                    callback = new ResponseCallback() {
                        @Override
                        public void onError(String errorMessage) {
                            Log.d(TAG, "onError: ");
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                            textView.setText(errorMessage);
                        }

                        @Override
                        public void onSuccess(FaceAnalysis[] response) {
                            Log.d(TAG, "onSuccess: ");
                            Toast.makeText(context, "Sukces, kurwa!", Toast.LENGTH_LONG).show();


                            imageView.setImageBitmap(scaledBMP);

                            if (response == null || response.length == 0) {
                                Toast.makeText(getApplicationContext(),"NULL result, retry!",Toast.LENGTH_SHORT).show();
                                Matrix matrix = new Matrix();
                                matrix.postRotate(90);
                                scaledBMP = Bitmap.createBitmap(scaledBMP,
                                                0, 0, scaledBMP.getWidth(), scaledBMP.getHeight(), matrix, true);
                                detect(scaledBMP, callback);
                                return;
                            }
                            FaceAnalysis faceAnalysis = response[0];
                            String str = "";
                            str += "anger =  " + String.format("%.2f", faceAnalysis.getScores().getAnger() * 100) + "%\n";
                            str += "contempt =  " + String.format("%.2f", faceAnalysis.getScores().getContempt() * 100) + "%\n";
                            str += "disgust =  " + String.format("%.2f", faceAnalysis.getScores().getDisgust() * 100) + "%\n";
                            str += "fear =  " + String.format("%.2f", faceAnalysis.getScores().getFear() * 100) + "%\n";
                            str += "happiness =  " + String.format("%.2f", faceAnalysis.getScores().getHappiness() * 100) + "%\n";
                            str += "neutral =  " + String.format("%.2f", faceAnalysis.getScores().getNeutral() * 100) + "%\n";
                            str += "sadness =  " + String.format("%.2f", faceAnalysis.getScores().getSadness() * 100) + "%\n";
                            str += "surprise =  " + String.format("%.2f", faceAnalysis.getScores().getSurprise() * 100) + "%\n";

                            textView.setText(str);
//                                for (FaceAnalysis faceAnalysis : response) {
//                                Log.d(TAG, "anger =  " + faceAnalysis.getScores().getAnger());
//                                Log.d(TAG, "contempt =  " + faceAnalysis.getScores().getContempt());
//                                Log.d(TAG, "disgust =  " + faceAnalysis.getScores().getDisgust());
//                                Log.d(TAG, "fear =  " + faceAnalysis.getScores().getFear());
//                                Log.d(TAG, "happiness =  " + faceAnalysis.getScores().getHappiness());
//                                Log.d(TAG, "neutreal =  " + faceAnalysis.getScores().getNeutral());
//                                Log.d(TAG, "sadness =  " + faceAnalysis.getScores().getSadness());
//                                Log.d(TAG, "surprise =  " + faceAnalysis.getScores().getSurprise());
//                            }
                        }
                    };
                    detect(scaledBMP, callback);
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
