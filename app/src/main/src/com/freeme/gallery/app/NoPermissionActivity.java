package com.freeme.gallery.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;

import com.freeme.gallery.R;
import com.mediatek.gallery3d.util.PermissionHelper;

public class NoPermissionActivity extends Activity implements View.OnClickListener {
    private static final int REQUEST_SETTINGS = 4;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView( R.layout.no_permissions);
        findViewById(R.id.photos_permissions_required_setting_button).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        startActivityForResult(intent, REQUEST_SETTINGS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_SETTINGS) {
            if (PermissionHelper.checkStoragePermission(this)) {
                        finish();
            }
        }
    }

}
