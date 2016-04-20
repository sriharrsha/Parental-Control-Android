package in.ac.vit.sriharrsha.accesibilityscreenshot;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    private static Activity activity;
    public static Activity getActivity() {
        return activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;
    }
}


