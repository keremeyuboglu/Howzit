package com.example.howzit.ui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.howzit.ConnectActivity;
import com.example.howzit.Contact;
import com.example.howzit.ContactDao;
import com.example.howzit.Listener;
import com.example.howzit.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link QRCameraFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class QRCameraFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    PreviewView previewView;
    ProcessCameraProvider provider;
    Preview preview;
    ImageAnalysis imageAnalysis;
    CameraSelector cameraSelector;
    private ContactDao contactDao;


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    public QRCameraFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment QRCameraFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static QRCameraFragment newInstance(String param1, String param2) {
        QRCameraFragment fragment = new QRCameraFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
            startCamera();
            contactDao = ((ConnectActivity)requireActivity()).getContactDao();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("aramak", "pause");
        if(provider != null){
            provider.unbindAll();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("aramak", "resume");
        if(provider != null){
            provider.bindToLifecycle(requireActivity(), cameraSelector, preview, imageAnalysis);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("aramak", "detach");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_q_r_camera, container, false);
        TextView t1 = root.findViewById(R.id.text1);
        TextView t2 = root.findViewById(R.id.text2);
        previewView = root.findViewById(R.id.preview);

        t1.setText(mParam1);
        t2.setText(mParam2);


        return root;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> pr = ProcessCameraProvider.getInstance(requireContext());


        pr.addListener(new Runnable() {

            @Override
            public void run() {
                try {
                    provider = pr.get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Preview.SurfaceProvider sp = previewView.getSurfaceProvider();
                preview = new Preview.Builder().build();
                preview.setSurfaceProvider(sp);
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                try{
                    provider.unbindAll();
                    ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
                    imageAnalysis = new ImageAnalysis.Builder().build();
                    imageAnalysis.setAnalyzer(cameraExecutor, new QRAnalyzer());
                }catch (Exception e){
                    Log.e("PreviewUseCase", e.toString());
                }


            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    public class QRAnalyzer implements ImageAnalysis.Analyzer {


        BarcodeScanner scanner;


        public QRAnalyzer()
        {

            scanner = BarcodeScanning.getClient();
        }

        private void returnResults(String result){
            JSONObject jsonresult;

            try {
                jsonresult = new JSONObject(result);
                Contact contact = new Contact();
                contact.key = jsonresult.getString("encryption_key");
                contact.mac_address = jsonresult.getString("mac_address");
                contactDao.insert(contact);
                final Toast toast = Toast.makeText(requireActivity(), "Contact added!", Toast.LENGTH_SHORT);
                toast.show();
                requireActivity().onBackPressed();
            } catch (JSONException e) {
                e.printStackTrace();
                final Toast toast = Toast.makeText(requireActivity(), "Not proper qr code!", Toast.LENGTH_SHORT);
                toast.show();
            } catch(Exception e){
                e.printStackTrace();
                Log.d("helllothere", "sfasfadfasfasdf");
            }

        }

        @Override
        public void analyze(@NonNull ImageProxy image) {
            @SuppressLint("UnsafeExperimentalUsageError") Image im = image.getImage();
            if(image == null)
                return;

                InputImage inputImage = InputImage.fromMediaImage(im, image.getImageInfo().getRotationDegrees());

                scanner.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {
                        Log.d("qrsuccess", ""+barcodes.size());
                        for(Barcode br : barcodes){
                            Log.d("naberkanki", br.getRawValue());
                            returnResults(br.getRawValue());
                        }
                    }
                }).addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<Barcode>> task) {
                        image.close();
                    }
                });

        }
    }


}