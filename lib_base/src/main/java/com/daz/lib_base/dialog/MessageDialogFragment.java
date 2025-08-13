package com.daz.lib_base.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDialogFragment;

import com.daz.lib_base.R;
import com.daz.lib_base.utils.XLog;


/**
 * 作者：wx
 * 时间：2019/6/20 9:17
 * 描述：消息提示框,本提示框用法：1.左,右按钮可单独存在,需使用三个按钮时中间按钮才可用,时间紧任务重,不想多做判断...
 */
public class MessageDialogFragment extends AppCompatDialogFragment {
    public static final String INTENT_KEY_TOUCH = "INTENT_KEY_TOUCH";//是否可触摸消失
    public static final String INTENT_KEY_BACK_CANCEL = "INTENT_KEY_BACK_CANCEL";//是否可按返回消失

    public static final String INTENT_KEY_MESSAGE = "INTENT_KEY_MESSAGE";//描述消息
    public static final String INTENT_KEY_MESSAGE_SUB = "INTENT_KEY_MESSAGE_SUB";//描述消息
    public static final String INTENT_KEY_MESSAGE_TYPE = "INTENT_KEY_MESSAGE_TYPE";//描述类型

    public static final String INTENT_KEY_LEFT_TEXT = "INTENT_KEY_LEFT_BUTTON_TEXT";//左边按钮文本
    public static final String INTENT_KEY_RIGHT_TEXT = "INTENT_KEY_RIGHT_BUTTON_TEXT";//右边按钮文本

    private Dialog mDialog;

    private TextView tvMessage, tvMessageSub;
    private TextView btnLeft;
    private TextView btnRight;

    private int intentTouch = 0;//是否显示imgClose
    private int intentBackCancel = 0;//是否返回键消失
    private String intentMessage;//描述消息
    private String intentMessageSub;//描述消息
    private int intentMessageType;//描述类型

    private String intentLeftButton;//左按钮文本
    private String intentRightButton;//右按钮文本*/

    public void setDataCallback(MessageDialogFragmentDataCallback dataCallback) {
        this.dataCallback = dataCallback;
    }

    private MessageDialogFragmentDataCallback dataCallback;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        getIntentData();

        initView();

        return mDialog;
    }

    private void getIntentData() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            intentTouch = bundle.getInt(INTENT_KEY_TOUCH);//是否显示checkbox
            intentBackCancel = bundle.getInt(INTENT_KEY_BACK_CANCEL);//是否显示checkbox
            intentMessage = bundle.getString(INTENT_KEY_MESSAGE);//描述消息
            intentMessageSub = bundle.getString(INTENT_KEY_MESSAGE_SUB);//描述消息
            intentMessageType = bundle.getInt(INTENT_KEY_MESSAGE_TYPE);//描述类型
            intentLeftButton = bundle.getString(INTENT_KEY_LEFT_TEXT);//左边按钮文本
            intentRightButton = bundle.getString(INTENT_KEY_RIGHT_TEXT);//右边按钮文本

            /*Log.d("Tag", "信息: " + intentMessage
                    + "\n左按钮: " + intentLeftButton
                    + "\n右按钮: " + intentRightButton);*/
        }
    }

    private void initView() {
        mDialog = new Dialog(getActivity(), R.style.BattleDialog);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(R.layout.dialog_fragment_message);
        mDialog.setCanceledOnTouchOutside(intentTouch == 0);
        mDialog.setCancelable(intentBackCancel == 0);

        if (intentTouch != 0) {
            mDialog.setOnKeyListener((dialog, keyCode, event) -> keyCode == KeyEvent.KEYCODE_BACK);
        }

        Window window = mDialog.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(layoutParams);

        mDialog.findViewById(R.id.close).setOnClickListener(view -> dismiss());
        tvMessage = mDialog.findViewById(R.id.tv_message);
        tvMessage.setMovementMethod(ScrollingMovementMethod.getInstance());

        tvMessageSub = mDialog.findViewById(R.id.tv_message_sub);

        btnLeft = mDialog.findViewById(R.id.btn_left);
        btnRight = mDialog.findViewById(R.id.btn_right);


        if (!TextUtils.isEmpty(intentMessage)) {
            intentMessage = intentMessage.replace("\n", "<br/>");
            tvMessage.setText(Html.fromHtml(intentMessage));
        } else {
            tvMessage.setText("");
        }

        if (!TextUtils.isEmpty(intentMessageSub)) {
            intentMessageSub = intentMessageSub.replace("\n", "<br/>");
            tvMessageSub.setText(Html.fromHtml(intentMessageSub));
            tvMessageSub.setVisibility(View.VISIBLE);
        } else {
            tvMessageSub.setText("");
            tvMessageSub.setVisibility(View.GONE);
        }

        if (dataCallback == null) {
            XLog.e("MessageDialogFragment", "MessageDialogFragmentDataCallback 回调不能为空");
            return;
        }

        switch (intentMessageType) {
            case MessageDialogParams.TYPE_ORANGE:
                btnLeft.setBackgroundResource(R.drawable.shape_bg_orange_normal);
                btnRight.setBackgroundResource(R.drawable.shape_bg_orange);
                break;
        }

        if (TextUtils.isEmpty(intentLeftButton)) {
            btnLeft.setVisibility(View.GONE);
        }
        if (TextUtils.isEmpty(intentRightButton)) {
            btnRight.setVisibility(View.GONE);
        }
        btnLeft.setText(intentLeftButton);
        btnLeft.setOnClickListener(view -> {
            //左边按钮事件
            mDialog.dismiss();
            dataCallback.messageDialogClickLeftButtonListener(mDialog, intentMessageType, btnLeft.getText().toString());
        });

        btnRight.setText(intentRightButton);
        btnRight.setOnClickListener(v -> {
            //右边按钮事件
            mDialog.dismiss();
            dataCallback.messageDialogClickRightButtonListener(mDialog, intentMessageType, btnRight.getText().toString());
        });
    }
}

