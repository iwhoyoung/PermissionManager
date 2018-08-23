package com.liyuanheng.www.permissionlibrary;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限管理
 *
 * @class PermissionManager
 * @date 2016-3-25 下午3:54:14
 */
public class PermissionManager {

    private static final String TAG = "PermissionManager";

    private static final int REQUEST_PERMISSION_CODE = 5123;
    private static final int REQUEST_PERMISSION_PAGE_CODE = 5124;

    private static PermissionManager sPermissionManager;
    private Object mContext;
    private List<String> mPermissions;
    private int mRequestCode;
    private List<String> deniedPermissions;
    private OnPermitListener mOnPermitListener;

    private boolean isAlertWinPermRequest = false;
    private boolean isWriteSetPermRequest = false;
    private boolean isHosted = false;
    // 用户是否确认了解释框的
    private boolean mIsPositive = false;

    public static PermissionManager getInstance(Activity context) {
        if (sPermissionManager == null)
            sPermissionManager = new PermissionManager(context);
        return sPermissionManager;
    }

    public static PermissionManager getInstance(Fragment context) {
        if (sPermissionManager == null)
            sPermissionManager = new PermissionManager(context);
        return sPermissionManager;
    }

    private PermissionManager setPermissions(String... permissions) {
        this.mPermissions = new ArrayList<>(Arrays.asList(permissions));
        return this;
    }

    private PermissionManager setRequestCode(int requestCode) {
        this.mRequestCode = requestCode;
        return this;
    }

    private PermissionManager(Object context) {
        this.mContext = context;
    }

    /**
     * 请求权限
     *
     * @return PermissionManager
     */
    public PermissionManager request() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            request(mContext, mPermissions, mRequestCode);
        return this;
    }

    /**
     * 一键请求权限
     */
    public void oneKeyRequest(String... permissions) {
        mRequestCode = REQUEST_PERMISSION_CODE;
        mPermissions = new ArrayList<>(Arrays.asList(permissions));
        isHosted = true;
        if (mContext instanceof OnPermitListener)
            setOnPermitListener((OnPermitListener) mContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            request(mContext, mPermissions, mRequestCode);
    }

    private void request(Object object, List<String> permissions, int requestCode) {
        for (String permission : permissions) {
            if (permission.equals("android.permission.SYSTEM_ALERT_WINDOW"))
                isAlertWinPermRequest = true;
            if (permission.equals("android.permission.WRITE_SETTINGS"))
                isWriteSetPermRequest = true;
        }
        if (isAlertWinPermRequest)
            permissions.remove("android.permission.SYSTEM_ALERT_WINDOW");
        if (isWriteSetPermRequest)
            permissions.remove("android.permission.WRITE_SETTINGS");
        // 根据权限集合去查找是否已经授权过
        Map<String, List<String>> map = findDeniedPermissions(getActivity(object), permissions);
        List<String> deniedPermissions = map.get("deny");
        List<String> rationales = map.get("rationale");
        if (deniedPermissions.size() == 0 && checkSpecialPermissionIfNeed()) {
            if (mOnPermitListener != null)
                mOnPermitListener.onPermissionGranted();
            return;
        }
        //应用采用adb安装后第一次也会被调用
        if (rationales.size() > 0 || !checkSpecialPermissionIfNeed() && mOnPermitListener != null) {
            mOnPermitListener.onPermissionDeniedForever(rationales/*.toArray(new String[rationales.size()])*/);
        }
        if (deniedPermissions.size() == 0) {
            onPermissionResult(REQUEST_PERMISSION_CODE, null, null);
            return;
        }
        if (object instanceof Activity) {
            ActivityCompat.requestPermissions((Activity) object, deniedPermissions.toArray(new String[deniedPermissions.size()]), requestCode);
        } else if (object instanceof Fragment) {
            ((Fragment) object).requestPermissions(deniedPermissions.toArray(new String[deniedPermissions.size()]), requestCode);
        } else {
            throw new IllegalArgumentException(object.getClass().getName() + " is not supported");
        }
    }

    /**
     * 根据requestCode处理响应的权限
     * 需在onRequestPermissionsResult（）调用
     */
    public void onPermissionResult(int requestCode, String[] permissions, int[] results) {
        switch (requestCode) {
            case REQUEST_PERMISSION_CODE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    deniedPermissions = new ArrayList<>();
                    if(results !=null) {
                        for (int i = 0; i < results.length; i++) {
                            //未授权
                            if (results[i] != PackageManager.PERMISSION_GRANTED) {
                                deniedPermissions.add(permissions[i]);
                            }
                        }
                    }
                    if (deniedPermissions.size() > 0 || !checkSpecialPermissionIfNeed()) {
                        if (isHosted) {
                            GoPermissionSettingDialog(true, false);
                        }
                        if (mOnPermitListener != null)
                            mOnPermitListener.onPermissionDenied(deniedPermissions);
                    } else if (mOnPermitListener != null)
                        mOnPermitListener.onPermissionGranted();
                }
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_PERMISSION_PAGE_CODE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!isAlertWinPermRequest && !isWriteSetPermRequest)
                        return;
                    if (getDeniedPermissions().size() > 0 || !checkSpecialPermissionIfNeed()) {
                        if (isHosted) {
                            GoPermissionSettingDialog(true, false);
                        }
                        if (mOnPermitListener != null)
                            mOnPermitListener.onPermissionDenied(deniedPermissions);
                    } else if (mOnPermitListener != null)
                        mOnPermitListener.onPermissionGranted();
                }
                break;
        }
    }

    private Map<String, List<String>> findDeniedPermissions(Activity activity, List<String> permissions) {
        Map<String, List<String>> map = new HashMap<>();
        List<String> denyList = new ArrayList<>();//未授权的权限
        List<String> rationaleList = new ArrayList<>();//需要显示提示框的权限
        for (String value : permissions) {
            if (ContextCompat.checkSelfPermission(activity, value) != PackageManager.PERMISSION_GRANTED) {
                denyList.add(value);
                if (!shouldShowRequestPermissionRationale(value)) {
                    rationaleList.add(value);
                }
            }
        }
        map.put("deny", denyList);
        map.put("rationale", rationaleList);
        return map;
    }

    private Activity getActivity(Object object) {
        if (object instanceof Fragment) {
            return ((Fragment) object).getActivity();
        } else if (object instanceof Activity) {
            return (Activity) object;
        }
        return null;
    }

    /**
     * 当用户拒绝某权限时并点击不再提醒的按钮时，
     * 下次应用再请求该权限时，需要给出合适的响应（比如给个展示对话框）
     * 返回false为永久不提醒或被允许,第一次安装必定返回false，返回true为允许
     */
    private boolean shouldShowRequestPermissionRationale(String permission) {
        if (mContext instanceof Activity) {
            return ActivityCompat.shouldShowRequestPermissionRationale((Activity) mContext, permission);
        } else if (mContext instanceof Fragment) {
            return ((Fragment) mContext).shouldShowRequestPermissionRationale(permission);
        } else {
            throw new IllegalArgumentException(mContext.getClass().getName() + " is not supported");
        }
    }

    public void GoPermissionSettingDialog(final boolean hasResult, final boolean isForce) {
        final Activity mActivity = getActivity(mContext);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mActivity);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setMessage(R.string.tip_no_permission);
        dialogBuilder.setTitle(R.string.setting);
        dialogBuilder.setPositiveButton(R.string.go_setting, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", mActivity.getPackageName(), null));
                if (hasResult)
                    mActivity.startActivityForResult(intent, REQUEST_PERMISSION_PAGE_CODE);
                else
                    mActivity.startActivity(intent);
            }
        });
        if (isForce)
            dialogBuilder.setNegativeButton(R.string.exit_app, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mActivity.finish();
                }
            });
        else
            dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
        dialogBuilder.create().show();
    }

    public boolean checkSpecialPermissionIfNeed() {
        Log.i(TAG, "isAlertWinPermRequest:" + checkFloatPermission(getActivity(mContext)) + ",isWriteSetPermRequest:" + checkWriteSettingPermission(getActivity(mContext)));
        return (!isAlertWinPermRequest || checkFloatPermission(getActivity(mContext)))
                && (!isWriteSetPermRequest || checkWriteSettingPermission(getActivity(mContext)));
    }

    public static boolean checkFloatPermission(Context context) {
        /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return true;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                Class cls = Class.forName("android.content.Context");
                Field declaredField = cls.getDeclaredField("APP_OPS_SERVICE");
                declaredField.setAccessible(true);
                Object obj = declaredField.get(cls);
                if (!(obj instanceof String)) {
                    return false;
                }
                String str2 = (String) obj;
                obj = cls.getMethod("getSystemService", String.class).invoke(context, str2);
                cls = Class.forName("android.app.AppOpsManager");
                Field declaredField2 = cls.getDeclaredField("MODE_ALLOWED");
                declaredField2.setAccessible(true);
                Method checkOp = cls.getMethod("checkOp", Integer.TYPE, Integer.TYPE, String.class);
                int result = (Integer) checkOp.invoke(obj, 24, Binder.getCallingUid(), context.getPackageName());
                return result == declaredField2.getInt(cls);
            } catch (Exception e) {
                return false;
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AppOpsManager appOpsMgr = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                if (appOpsMgr == null)
                    return false;
                int mode = appOpsMgr.checkOpNoThrow("android:system_alert_window", android.os.Process.myUid(), context
                        .getPackageName());
                return mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_IGNORED;
            } else {
                return Settings.canDrawOverlays(context);
            }
        }*/
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;
        else
            return Settings.canDrawOverlays(context);
    }

    public static boolean checkWriteSettingPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;
        else
            return Settings.System.canWrite(context);
    }

/*    public List<String> getDeniedPermissions() {
        return deniedPermissions;
    }*/

    public List<String> getDeniedPermissions() {
        List<String> denyList = new ArrayList<>();//未授权的权限
        if (mPermissions == null)
            return denyList;
        for (String value : mPermissions)
            if (ContextCompat.checkSelfPermission((Context) mContext, value) != PackageManager.PERMISSION_GRANTED)
                denyList.add(value);
        return denyList;
    }

    public boolean isHosted() {
        return isHosted;
    }

    public void setHosted(boolean hosted) {
        isHosted = hosted;
    }

    public void setOnPermitListener(OnPermitListener listener) {
        mOnPermitListener = listener;
    }

    public interface OnPermitListener {

        /**
         * 用户允许申请的权限组中所有权限时调用
         */
        void onPermissionGranted();

        /**
         * 用户禁止权限组中任一权限时调用
         */
        void onPermissionDenied(List<String> deniedPermission);

        /**
         * 永久被禁用权限提示
         */
        void onPermissionDeniedForever(List<String> strings);

    }
}
