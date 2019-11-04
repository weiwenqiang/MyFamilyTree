package com.cxb.myfamilytree.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.bumptech.glide.Glide;
import com.cxb.myfamilytree.R;
import com.cxb.myfamilytree.app.BaseActivity;
import com.cxb.myfamilytree.config.Constants;
import com.cxb.myfamilytree.model.FamilyBean;
import com.cxb.myfamilytree.presenter.AddFamilyPresenter;
import com.cxb.myfamilytree.utils.DateTimeUtils;
import com.cxb.myfamilytree.utils.FileUtils;
import com.cxb.myfamilytree.utils.ImageUtils;
import com.cxb.myfamilytree.utils.SDCardUtils;
import com.cxb.myfamilytree.view.IAddFamilyView;
import com.cxb.myfamilytree.widget.dialog.AlertDialogFragment;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import static com.cxb.myfamilytree.model.FamilyBean.SEX_FEMALE;
import static com.cxb.myfamilytree.model.FamilyBean.SEX_MALE;

/**
 * 添加家庭成员
 */

public class AddFamilyActivity extends BaseActivity implements IAddFamilyView {

    private static final String ADD_TYPE = "add_type";//添加类型
    public static final String FAMILY_INFO = "family_info";//家人信息

    private static final int REQUEST_CODE_SELECT_PICTURE = 100;
    private static final int REQUEST_CODE_CUTE_PICTURE = 101;

    private ImageView ivAvatar;
    private EditText mEditName;
    private EditText mEditCall;
    private EditText mEditBirthday;
    private RadioGroup mGenderGroup;
    private Button btnDelete;

    private AlertDialogFragment mDeleteDialog;
    private AlertDialogFragment mModifyDialog;

    private FamilyBean mSelectFamily;
    private String mAddType;
    private File mCutePhotoFile;
    private String mTempPath;
    private String mAvatarPath;

    private AddFamilyPresenter mPresenter;

    public static void show(Activity activity, int requestCode, FamilyBean family, String type) {
        Intent intent = new Intent();
        intent.setClass(activity, AddFamilyActivity.class);
        intent.putExtra(AddFamilyActivity.FAMILY_INFO, family);
        intent.putExtra(AddFamilyActivity.ADD_TYPE, type);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_add_family;
    }

    @Override
    protected void initData() {
        super.initData();
        final Intent intent = getIntent();
        mAddType = intent.getStringExtra(ADD_TYPE);
        mSelectFamily = intent.getParcelableExtra(FAMILY_INFO);

        mTempPath = SDCardUtils.getExternalCacheDir(this);
        FileUtils.createdirectory(mTempPath);

        mPresenter = new AddFamilyPresenter();
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarBackEnable();

        mPresenter.attachView(this);

        ivAvatar = findViewById(R.id.iv_avatar);
        mEditName = findViewById(R.id.et_name);
        mEditCall = findViewById(R.id.et_call);
        mEditBirthday = findViewById(R.id.et_birthday);
        mGenderGroup = findViewById(R.id.rg_gender);
        btnDelete = findViewById(R.id.btn_delete);

        final String familyName = mSelectFamily.getMemberName();
        if (Constants.TYPE_ADD_SPOUSE.equals(mAddType)) {
            setToolbarTitle(R.string.add_spouse);
            setToolbarSubTitle(String.format(getString(R.string.add_who_s_spouse), familyName));
        } else if (Constants.TYPE_ADD_PARENT.equals(mAddType)) {
            setToolbarTitle(R.string.add_parent);
            setToolbarSubTitle(String.format(getString(R.string.add_who_s_parent), familyName));
        } else if (Constants.TYPE_ADD_CHILD.equals(mAddType)) {
            setToolbarTitle(R.string.add_child);
            setToolbarSubTitle(String.format(getString(R.string.add_who_s_child), familyName));
        } else if (Constants.TYPE_ADD_BROTHERS_AND_SISTERS.equals(mAddType)) {
            setToolbarTitle(R.string.add_brother_and_sister);
            setToolbarSubTitle(String.format(getString(R.string.add_who_s_brother_and_sister), familyName));
        } else {
            setToolbarTitle(R.string.family_information);
            mAvatarPath = mSelectFamily.getMemberImg();
            if (!TextUtils.isEmpty(mAvatarPath)) {
                final File file = new File(mAvatarPath);
                Glide.with(this)
                        .load(file)
                        .placeholder(R.drawable.ic_add_a_photo)
                        .error(R.drawable.ic_add_a_photo)
                        .centerCrop()
                        .dontAnimate()
                        .into(ivAvatar);
            }

            mEditName.setText(mSelectFamily.getMemberName());
            mEditCall.setText(mSelectFamily.getCall());
            mEditBirthday.setText(mSelectFamily.getBirthday());

            final boolean isMy = Constants.MY_ID.endsWith(mSelectFamily.getMemberId());

            btnDelete.setVisibility(isMy ? View.GONE : View.VISIBLE);
            btnDelete.setOnClickListener(mClick);

            final int count = mGenderGroup.getChildCount();
            if (count == 2) {
                final RadioButton maleButton = (RadioButton) mGenderGroup.getChildAt(0);
                final RadioButton femaleButton = (RadioButton) mGenderGroup.getChildAt(1);
                if (SEX_MALE.equals(mSelectFamily.getSex())) {
                    maleButton.setChecked(true);
                    femaleButton.setChecked(false);
                } else {
                    maleButton.setChecked(false);
                    femaleButton.setChecked(true);
                }
            }
        }

        ivAvatar.setOnClickListener(mClick);
        mEditBirthday.setOnClickListener(mClick);
        mEditBirthday.setLongClickable(false);
    }


    private void showModifyDialog(final String name, final String call, final String birthday, final String gender) {
        if (mModifyDialog == null) {
            final DialogInterface.OnClickListener dialogClick = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    if (DialogInterface.BUTTON_POSITIVE == which) {
                        final boolean isChangeGender = !gender.equals(mSelectFamily.getSex());
                        final String oldPath = mSelectFamily.getMemberImg();
                        if (!TextUtils.isEmpty(oldPath)) {
                            final File file = new File(oldPath);
                            if (file.exists()) {
                                file.delete();
                            }
                        }

                        mSelectFamily.setMemberImg(mAvatarPath);
                        mSelectFamily.setMemberName(name);
                        mSelectFamily.setCall(call);
                        mSelectFamily.setBirthday(birthday);
                        mSelectFamily.setSex(gender);
                        mPresenter.updateFamilyInfo(mSelectFamily, isChangeGender);
                    }
                }
            };

            mModifyDialog = new AlertDialogFragment();
            mModifyDialog.setCancelable(true);
            mModifyDialog.setConfirmButton(getString(R.string.edit), dialogClick);
            mModifyDialog.setCancelButton(getString(R.string.cancel), dialogClick);
        }

        if (TextUtils.isEmpty(mSelectFamily.getSpouseId()) || gender.equals(mSelectFamily.getSex())) {
            mModifyDialog.setMessage("是否要修改该亲人的信息？");
        } else {
            mModifyDialog.setMessage("更改性别后，配偶的性别也相应更改，是否继续修改该亲人的信息？");
        }

        if (mModifyDialog.isAdded()) {
            mModifyDialog.dismiss();
        } else {
            mModifyDialog.show(getSupportFragmentManager(), "ModifyDialog");
        }
    }

    private void showDeleteDialog() {
        if (mDeleteDialog == null) {
            final DialogInterface.OnClickListener dialogClick = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    if (DialogInterface.BUTTON_POSITIVE == which) {
                        mPresenter.deleteFamily(mSelectFamily);
                    }
                }
            };

            mDeleteDialog = new AlertDialogFragment();
            mDeleteDialog.setCancelable(true);
            mDeleteDialog.setConfirmButton(getString(R.string.delete), dialogClick);
            mDeleteDialog.setCancelButton(getString(R.string.cancel), dialogClick);
            mDeleteDialog.setTitle("温馨提示");
            mDeleteDialog.setMessage("删除\"" + mSelectFamily.getMemberName() + "\"后，其相关的家谱分支将不会显示，是否继续删除该亲人？");
        }

        if (mDeleteDialog.isAdded()) {
            mDeleteDialog.dismiss();
        } else {
            mDeleteDialog.show(getSupportFragmentManager(), "DeleteDialog");
        }
    }

    private void doSelectPicture() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(pickIntent, REQUEST_CODE_SELECT_PICTURE);
    }

    private void doCutPicture(Uri inputUri) {
        if (inputUri.toString().contains("file://")) {
            final String path = inputUri.getPath();
            final File inputFile = new File(path);
            inputUri = ImageUtils.getImageContentUri(this, inputFile);
        }

        mCutePhotoFile = new File(mTempPath, System.currentTimeMillis() + ".jpg");
        final Uri outputUri = Uri.fromFile(mCutePhotoFile);

        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(inputUri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 500);
        intent.putExtra("outputY", 500);
        intent.putExtra("scale", true);
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true);
        startActivityForResult(intent, REQUEST_CODE_CUTE_PICTURE);
    }

    private void doConfirm() {
        final String name = mEditName.getText().toString();
        final String call = mEditCall.getText().toString();
        final String birthday = mEditBirthday.getText().toString();
        final String gender = mGenderGroup.getCheckedRadioButtonId() == R.id.rb_female ? SEX_FEMALE : SEX_MALE;
        if (TextUtils.isEmpty(name)) {
            showToast(getString(R.string.name_can_not_null));
        } else if (TextUtils.isEmpty(call)) {
            showToast(getString(R.string.call_can_not_null));
        } else {
            if (TextUtils.isEmpty(mAddType)) {
                showModifyDialog(name, call, birthday, gender);
            } else {
                final FamilyBean family = new FamilyBean();
                family.setMemberImg(mAvatarPath);
                family.setMemberId(String.valueOf(System.currentTimeMillis()));
                family.setMemberName(name);
                family.setCall(call);
                family.setBirthday(birthday);
                family.setSex(gender);
                if (Constants.TYPE_ADD_SPOUSE.equals(mAddType)) {
                    mPresenter.addSpouse(mSelectFamily, family);
                } else if (Constants.TYPE_ADD_PARENT.equals(mAddType)) {
                    mPresenter.addParent(mSelectFamily, family);
                } else if (Constants.TYPE_ADD_CHILD.equals(mAddType)) {
                    mPresenter.addChild(mSelectFamily, family);
                } else if (Constants.TYPE_ADD_BROTHERS_AND_SISTERS.equals(mAddType)) {
                    mPresenter.addBrothersAndSisters(mSelectFamily, family);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (REQUEST_CODE_SELECT_PICTURE == requestCode) {
                if (data != null) {
                    doCutPicture(data.getData());
                }
            } else if (REQUEST_CODE_CUTE_PICTURE == requestCode) {
                if (mCutePhotoFile != null && mCutePhotoFile.exists()) {
                    mAvatarPath = mCutePhotoFile.getAbsolutePath();

                    Glide.with(this)
                            .load(mCutePhotoFile)
                            .placeholder(R.drawable.ic_add_a_photo)
                            .error(R.drawable.ic_add_a_photo)
                            .centerCrop()
                            .dontAnimate()
                            .into(ivAvatar);
                } else {
                    showSnackbar("图片不存在");
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.detachView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_confirm:
                hideKeyboard();
                doConfirm();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.confirm, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void setResultAndFinish() {
        Intent intent = new Intent();
        intent.putExtra(FAMILY_INFO, mSelectFamily.getMemberId());
        setResult(RESULT_OK, intent);
        onBackPressed();
    }

    @Override
    public void showToast(String toast) {
        showSnackbar(toast);
    }

    private final View.OnClickListener mClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.iv_avatar:
                    doSelectPicture();
                    break;
                case R.id.et_birthday:
                    final String dateText = mEditBirthday.getText().toString();
                    break;
                case R.id.btn_delete:
                    showDeleteDialog();
                    break;
            }
        }
    };

}
