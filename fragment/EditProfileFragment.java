package com.restaurantsapp.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.restaurantsapp.R;
import com.restaurantsapp.RestaurantsApp;
import com.restaurantsapp.activity.MainActivity;
import com.restaurantsapp.simplecrop.CropImage;
import com.restaurantsapp.utils.Constants;
import com.restaurantsapp.utils.PreferenceUtils;
import com.restaurantsapp.utils.RoundedImageView;
import com.restaurantsapp.utils.Utils;
import com.restaurantsapp.webservice.WSUpdateProfile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Admin on 26-03-2016.
 */
public class EditProfileFragment extends MainFragment {

    private static final int REQUEST_CODE_GALLERY = 0x1;
    private static final int REQUEST_CODE_TAKE_PICTURE = 0x2;
    private static final int REQUEST_CODE_CROP_IMAGE = 0x3;
    private static final String TEMP_PHOTO_FILE_NAME = "temp_photo.jpg";
    private File imageFile;
    private boolean isProfilePicSelcted = false;
    private RoundedImageView roundedImageView;
    private RestaurantsApp restaurantsApp;
    private EditText editTextName;
    private EditText editTextContactNumber;
    private EditText editTextEmail;

    private Button buttonSignUp;
    private CheckBox checkBoxRm;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, null);
    }

    @Override
    public void initView(View view) {
        initActionBar();
        restaurantsApp = (RestaurantsApp) getActivity().getApplicationContext();
        roundedImageView = (RoundedImageView) view.findViewById(R.id.iv_profile);
        editTextName = (EditText) view.findViewById(R.id.fragment_signup_edittext_name);
        editTextContactNumber = (EditText) view.findViewById(R.id.fragment_signup_edittext_phone);
        editTextEmail = (EditText) view.findViewById(R.id.fragment_signup_edittext_email);
        buttonSignUp = (Button) view.findViewById(R.id.fragment_signup_button_submit);
        checkBoxRm = (CheckBox) view.findViewById(R.id.fragment_signup_checkbox_user);
        final String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            imageFile = new File(Environment.getExternalStorageDirectory(), TEMP_PHOTO_FILE_NAME);
        } else {
            imageFile = new File(getActivity().getFilesDir(), TEMP_PHOTO_FILE_NAME);
        }
        final PreferenceUtils preferenceUtils = new PreferenceUtils(getActivity());
        final String userName = preferenceUtils.getString(PreferenceUtils.USER_NAME);
        final String userContactNumber = preferenceUtils.getString(PreferenceUtils.USER_CONTACT_NUMBER);
        final String email = preferenceUtils.getString(PreferenceUtils.EMAIL);
        final String profileUrl = preferenceUtils.getString(PreferenceUtils.PROFILE_URL);
        final String userType = preferenceUtils.getString(PreferenceUtils.USER_TYPE);

        editTextName.setText(userName);
        editTextContactNumber.setText(userContactNumber);
        editTextEmail.setText(email);

        restaurantsApp.getImageLoader().displayImage(Constants.BASE_URL_PROFILE + profileUrl, roundedImageView, restaurantsApp.getDisplayImageOptions());
//        if (!TextUtils.isEmpty(userType) && userType.equalsIgnoreCase(Constants.USER_TYPE_USER)) {
//            checkBoxRm.setVisibility(View.GONE);
//        } else {
//            checkBoxRm.setVisibility(View.VISIBLE);
//        }
        roundedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseKarigarPhoto();
            }
        });
        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String name = editTextName.getText().toString().trim();
                final String contact = editTextContactNumber.getText().toString().trim();
                final String email = editTextEmail.getText().toString().trim();
                if (Utils.isOnline(getActivity(), true)) {
                    if (name.isEmpty() || contact.isEmpty() || email.isEmpty()) {
                        Toast.makeText(getActivity(), "Please complete all fields", Toast.LENGTH_SHORT).show();
                    } else if (contact.length() != 10) {
                        Toast.makeText(getActivity(), "Invalid contact number", Toast.LENGTH_SHORT).show();
                    } else if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() == false) {
                        Toast.makeText(getActivity(), "Invalid email", Toast.LENGTH_SHORT).show();
                    } else {

                        String profileImage = "";
                        if (isProfilePicSelcted && !imageFile.equals(null)) {
                            profileImage = imageFile.getPath();
                        }
                        final AsyncEditProfile asyncRegister = new AsyncEditProfile();
                        if (checkBoxRm.isChecked()) {
                            asyncRegister.execute(name, contact, profileImage);
                        } else {
                            asyncRegister.execute(name, contact, profileImage);
                        }
                    }
                }
            }
        });


    }

    @Override
    public void initActionBar() {
        ((MainActivity) getActivity()).setTopBar(getString(R.string.profile), true);
    }


    private void chooseKarigarPhoto() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        dialog.setTitle(getString(R.string.choose_option));
        dialog.setItems(R.array.take_photo_option, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        takePicture();
                        break;
                    case 1:
                        final Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                        photoPickerIntent.setType("image/*");
                        startActivityForResult(photoPickerIntent, REQUEST_CODE_GALLERY);
                        break;
                }
            }
        });
        dialog.show();

    }


    private void startCropImage() {
        final Intent intent = new Intent(getActivity(), CropImage.class);
        intent.putExtra(CropImage.IMAGE_PATH, imageFile.getPath());
        intent.putExtra(CropImage.SCALE, true);
        intent.putExtra(CropImage.ASPECT_X, 2);
        intent.putExtra(CropImage.ASPECT_Y, 2);
        startActivityForResult(intent, REQUEST_CODE_CROP_IMAGE);
    }

    private void takePicture() {
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            Uri mImageCaptureUri = null;
            final String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                mImageCaptureUri = Uri.fromFile(imageFile);
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
            intent.putExtra("return-data", true);
            startActivityForResult(intent, REQUEST_CODE_TAKE_PICTURE);
        } catch (ActivityNotFoundException e) {
            Log.d("TAG", "cannot take picture", e);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_GALLERY:
                try {
                    final InputStream inputStream = getActivity().getContentResolver().openInputStream(data.getData());
                    final FileOutputStream fileOutputStream = new FileOutputStream(imageFile);
                    copyStream(inputStream, fileOutputStream);
                    fileOutputStream.close();
                    inputStream.close();
                    startCropImage();
                } catch (Exception e) {
                    Utils.displayAlertDialog(getActivity(), "", e.getMessage());
                    e.printStackTrace();
                    // Log.e(TAG, "Error while creating temp file", e);
                }

                break;
            case REQUEST_CODE_TAKE_PICTURE:
                startCropImage();
                break;
            case REQUEST_CODE_CROP_IMAGE:
                final String path = data.getStringExtra(CropImage.IMAGE_PATH);
                if (path == null) {
                    return;
                }
                isProfilePicSelcted = true;
                restaurantsApp.getImageLoader().getInstance().displayImage("file:///" + imageFile.getPath(), roundedImageView);
                break;

        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    private class AsyncEditProfile extends AsyncTask<String, Void, Void> {
        private ProgressDialog progressDialog;
        private WSUpdateProfile wsUpdateProfile;
        private Boolean isError;
        private String registerMessage;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = Utils.showProgressDialog(getActivity(), getString(R.string.loading), false);
        }

        @Override
        protected Void doInBackground(String... params) {
            wsUpdateProfile = new WSUpdateProfile(getActivity());
            wsUpdateProfile.executeService(params[0], params[1], params[2]);
            return null;
        }

        @Override
        protected void onPostExecute(Void login) {
            super.onPostExecute(login);
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            if (wsUpdateProfile != null) {
                isProfilePicSelcted = false;
                isError = wsUpdateProfile.isError();
                registerMessage = wsUpdateProfile.getMessage();
                if (isError) {
                    ((MainActivity) getActivity()).updateProfileImageName();
                    Utils.displayAlertDialog(getActivity(), "", registerMessage);
                    getFragmentManager().popBackStack();
                } else {
                    Utils.displayAlertDialog(getActivity(), "", registerMessage);
                }
            } else {
                Toast.makeText(getActivity(), registerMessage, Toast.LENGTH_SHORT).show();

            }
        }
    }


}
