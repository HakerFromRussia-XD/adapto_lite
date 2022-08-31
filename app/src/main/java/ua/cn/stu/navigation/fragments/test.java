package ua.cn.stu.navigation.fragments;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import ua.cn.stu.navigation.R;

public class test extends Activity {
    ImageView iii;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        iii = findViewById(R.id.speed_arrow_iv);

        ObjectAnimator alphaA = ObjectAnimator.ofFloat(iii, View.ALPHA, 0, 1);
        alphaA.setDuration(4000);

        ObjectAnimator rotatinA = ObjectAnimator.ofFloat(iii, View.ROTATION, 0, 180);
        rotatinA.setDuration(4000);

        AnimatorSet anim = new AnimatorSet();
        anim.play(alphaA).with(rotatinA);
        anim.start();
    }
}
