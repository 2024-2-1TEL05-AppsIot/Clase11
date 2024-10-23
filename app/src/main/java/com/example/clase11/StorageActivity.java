package com.example.clase11;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.clase11.databinding.ActivityStorageBinding;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StorageActivity extends AppCompatActivity {

    ActivityStorageBinding binding;
    FirebaseStorage storage;
    StorageReference reference;

    private static final int TOAST_DURATION = Toast.LENGTH_LONG;

    private final static String TAG = "msg-test";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStorageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        storage = FirebaseStorage.getInstance();
        reference = storage.getReference();

        binding.btnUploadFile.setOnClickListener(view -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        binding.btnRefresh.setOnClickListener(view -> {
            listarArchivos();
        });


        binding.btnDownloadAndSave.setOnClickListener(view -> {

            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                descargarYguardar();

            } else {
                String permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
                requestReadPermissionLauncher.launch(permission);
            }
        });



        binding.logoutBtn.setOnClickListener(view -> {
            AuthUI.getInstance().signOut(StorageActivity.this)
                    .addOnCompleteListener(task -> {
                        Intent intent = new Intent(StorageActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    });
        });

        listarArchivos();
    }

    public void descargarYMostrarPeroNoGuardar(){
        StorageReference islandRef = reference.child("images/1000000010");

        final long three_MEGABYTE = 1024 * 1024 * 3;
        islandRef.getBytes(three_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                InputStream is = new ByteArrayInputStream(bytes);
                Bitmap bmp = BitmapFactory.decodeStream(is);
                binding.imageViewDownload.setImageBitmap(bmp);
                // Data for "images/island.jpg" is returns, use this as needed
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
    }

    public void descargarYMostrarPeroNoGuardarForma2(){
        StorageReference islandRef = reference.child("images/1000000011");
        Glide.with(StorageActivity.this).load(islandRef).into(binding.imageViewDownload);
    }

    public void descargarYguardar() {

        String selectedItem = (String) binding.spinnerFileList.getSelectedItem();

        File directorio = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File localFile = new File(directorio, selectedItem);

        StorageReference docRef = storage.getReference().child("images").child(selectedItem);
        binding.progressBarDownload.setVisibility(View.VISIBLE);
        docRef.getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "archivo descargado");
                    Toast.makeText(StorageActivity.this,"Archivo descargado",TOAST_DURATION).show();
                    binding.progressBarDownload.setVisibility(View.INVISIBLE);
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "error", e.getCause());
                })
                .addOnProgressListener(snapshot -> {
                    long bytesTransferred = snapshot.getBytesTransferred();
                    long totalByteCount = snapshot.getTotalByteCount();

                    binding.textViewDownloadPercentValue.setText(Math.round((bytesTransferred * 1.0f / totalByteCount) * 100) + "%");

                    binding.progressBarDownload.setProgress(Math.round((bytesTransferred * 1.0f / totalByteCount) * 100));
                    //
                });

        Glide.with(this).load(docRef).into( binding.imageViewDownload);
    }

    public void listarArchivos(){
        StorageReference listRef = storage.getReference().child("images");

        listRef.listAll()
                .addOnSuccessListener(listResult -> {
                    String[] items = new String[listResult.getItems().size()];
                    int i = 0;

                    for (StorageReference item : listResult.getItems()) {
                        // All the items under listRef.
                        //Log.d(TAG,"item.getName(): " + item.getName());
                        //Log.d(TAG,"item.getPath(): " + item.getPath());
                        items[i++] = item.getName();
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(StorageActivity.this,
                            android.R.layout.simple_spinner_dropdown_item,
                            items);
                    binding.spinnerFileList.setAdapter(adapter);

                })
                .addOnFailureListener(e -> e.printStackTrace());
    }

    ActivityResultLauncher<String> requestReadPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    descargarYguardar();
                }
            }
    );

    ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                String hash = "file";
                if (uri != null) {
                    File file= new File(uri.getPath());

                    //Log.d(TAG, "file.getName(): " + file.getName());
                    //Log.d(TAG, "uri.getLastPathSegment(): " + uri.getLastPathSegment());
                    //Log.d(TAG, "uri.getEncodedPath(): " + uri.getEncodedPath());
                    String name = file.getName().substring(0, file.getName().lastIndexOf("."));
                    hash = obtenerMD5(uri.getLastPathSegment() +
                            String.valueOf(System.currentTimeMillis()));

                    StorageReference imagesRef = reference.child(
                            "images/" + name + "_" + hash + ".jpg");

                    StorageMetadata metadata = new StorageMetadata.Builder()
                            .setCustomMetadata("Autor", "Oscar DÍaz")
                            .setCustomMetadata("Curso", "ITEL05")
                            .build();

                    UploadTask uploadTask = imagesRef.putFile(uri, metadata);

                    uploadTask.addOnFailureListener(exception -> {
                        exception.printStackTrace();
                    }).addOnSuccessListener(taskSnapshot -> {
                        // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                        Toast.makeText(StorageActivity.this, "Archivo subido correctamente", TOAST_DURATION).show();
                    }).addOnProgressListener(snapshot -> {
                        long bytesTransferred = snapshot.getBytesTransferred();
                        long totalByteCount = snapshot.getTotalByteCount();
                        double porcentajeSubida = Math.round((bytesTransferred * 1.0f / totalByteCount) * 100);

                        binding.textViewUploadPercentValue.setText(porcentajeSubida + "%");
                        //
                    });
                } else {
                    Log.d(TAG, "No media selected");
                }
            });

    private static String obtenerMD5(String texto) {
        try {
            // Obtener una instancia de MessageDigest para MD5
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Actualizar el digest con el byte array de los bytes del texto
            md.update(texto.getBytes());

            // Calcular el hash y obtener los bytes resultantes
            byte[] hashBytes = md.digest();

            // Convertir los bytes a una representación hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

}