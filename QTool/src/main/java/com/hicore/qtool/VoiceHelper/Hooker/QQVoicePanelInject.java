package com.hicore.qtool.VoiceHelper.Hooker;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.hicore.HookItem;
import com.hicore.ReflectUtils.MClass;
import com.hicore.ReflectUtils.MField;
import com.hicore.ReflectUtils.MMethod;
import com.hicore.ReflectUtils.QQReflect;
import com.hicore.ReflectUtils.ResUtils;
import com.hicore.ReflectUtils.XPBridge;
import com.hicore.UIItem;
import com.hicore.Utils.Utils;
import com.hicore.qtool.EmoHelper.Panel.EmoPanel;
import com.hicore.qtool.HookEnv;
import com.hicore.qtool.R;
import com.hicore.qtool.VoiceHelper.Panel.VoicePanel;
import com.hicore.qtool.VoiceHelper.Panel.VoicePanelController;
import com.hicore.qtool.XposedInit.ItemLoader.BaseHookItem;
import com.hicore.qtool.XposedInit.ItemLoader.BaseUiItem;
import com.hicore.qtool.XposedInit.ItemLoader.HookLoader;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;


@HookItem(isRunInAllProc = false,isDelayInit = false)
@UIItem(itemType = 1,itemName = "语音面板",itemDesc = "可以在QQ的发送语音界面注入打开语音面板按钮",mainItemID = 1,ID = "VoicePanelInject")
public class QQVoicePanelInject extends BaseHookItem implements BaseUiItem {
    boolean IsEnable = false;
    @Override
    public boolean startHook() throws Throwable {
        Member[] m = getMethod();

        XPBridge.HookAfter(m[0],param -> {
            int mSpeakID = HookEnv.AppContext.getResources().getIdentifier("press_to_speak_iv","id", HookEnv.AppContext.getPackageName());
            RelativeLayout RLayout = (RelativeLayout) param.thisObject;
            ResUtils.StartInject(RLayout.getContext());
            ImageView image = new ImageView(RLayout.getContext());
            image.setImageResource(R.drawable.voice_panel);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(Utils.dip2px(RLayout.getContext(),25), Utils.dip2px(RLayout.getContext(),25));
            params.addRule(RelativeLayout.BELOW,mSpeakID);
            params.addRule(RelativeLayout.CENTER_IN_PARENT,1);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,1);

            RLayout.addView(image,params);
            image.setOnClickListener(v-> VoicePanel.createVoicePanel());
        });

        XPBridge.HookAfter(m[1],param -> {
            if (IsEnable){
                Object arr = param.getResult();
                Object ret = Array.newInstance(arr.getClass().getComponentType(),Array.getLength(arr)+1);
                System.arraycopy(arr, 0, ret, 1, Array.getLength(arr));
                Object MenuItem = MClass.NewInstance(MClass.loadClass("com.tencent.mobileqq.utils.dialogutils.QQCustomMenuItem"),3100,"保存到QT");
                MField.SetField(MenuItem,"c",Integer.MAX_VALUE-1);
                Array.set(ret,0,MenuItem);

                param.setResult(ret);
            }
        });

        XPBridge.HookBefore(m[2],param -> {
            int InvokeID = (int) param.args[0];
            Context mContext = (Context) param.args[1];
            Object chatMsg = param.args[2];
            if (InvokeID == 3100){
                String PTTPath = MMethod.CallMethodNoParam(chatMsg,"getLocalFilePath",String.class);
                VoicePanel.preSaveVoice(mContext,PTTPath);
            }
        });
        return true;
    }

    @Override
    public boolean isEnable() {
        return IsEnable;
    }

    @Override
    public boolean check() {
        Member[] methods = getMethod();
        for (Member m : methods){
            if (m == null)return false;
        }
        return true;
    }

    @Override
    public void SwitchChange(boolean IsCheck) {
        IsEnable = IsCheck;
        if (IsCheck){
            HookLoader.CallHookStart(QQVoicePanelInject.class.getName());
        }
    }

    @Override
    public void ListItemClick() {

    }
    public Member[] getMethod(){
        Member[] m = new Member[3];
        Class clz = MClass.loadClass("com.tencent.mobileqq.activity.aio.audiopanel.PressToSpeakPanel");
        for(Constructor cons : clz.getDeclaredConstructors()){
            if (cons.getParameterCount() == 2){
                if (cons.getParameterTypes()[0] == Context.class && cons.getParameterTypes()[1] == AttributeSet.class){
                    cons.setAccessible(true);
                    m[0] = cons;
                    break;
                }
            }
        }

        m[1] = QQReflect.GetItemBuilderMenuBuilder(MClass.loadClass("com.tencent.mobileqq.activity.aio.item.PttItemBuilder"),"a");
        m[2] = MMethod.FindMethod("com.tencent.mobileqq.activity.aio.item.PttItemBuilder","a",void.class,new Class[]{
                int.class, Context.class, MClass.loadClass("com.tencent.mobileqq.data.ChatMessage")});

        return m;
    }
}