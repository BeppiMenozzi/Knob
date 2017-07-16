package it.beppi.knobselector;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Random;

import it.beppi.knoblibrary.Knob;

public class SampleActivity extends AppCompatActivity {
    BTimer bt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);


        final Knob knob1 = (Knob) findViewById(R.id.knob1);
        final Knob knob2 = (Knob) findViewById(R.id.knob2);
        final Knob knob3 = (Knob) findViewById(R.id.knob3);
        final Knob knob4 = (Knob) findViewById(R.id.knob4);
        final Knob knob5 = (Knob) findViewById(R.id.knob5);

        final TextView textView1 = (TextView) findViewById(R.id.textView1);
        final TextView textView4 = (TextView) findViewById(R.id.textView4);

        Button buttonRndVal = (Button) findViewById(R.id.button_rnd_val);
        Button buttonRndNum = (Button) findViewById(R.id.button_rnd_num);
        final Random rnd = new Random();

        textView1.setText(Integer.toString(knob1.getState()));

        buttonRndVal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int newVal = rnd.nextInt(knob1.getNumberOfStates());
                knob1.setState(newVal);
            }
        });

        buttonRndNum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int newNum = 2 + rnd.nextInt(60);
                knob1.setNumberOfStates(newNum);
                if (knob1.getState() >= newNum) knob1.setState(newNum-1);
            }
        });

        knob1.setOnStateChanged(new Knob.OnStateChanged() {
            @Override
            public void onState(int state) {
                textView1.setText(Integer.toString(state));
            }
        });

        knob3.setState(Calendar.getInstance().get(Calendar.SECOND));
        bt = new BTimer(1000, new Runnable() {
            @Override public void run() { knob3.toggle(); }});
        bt.start();

        textView4.setText(Integer.toString(knob4.getState()));
        knob4.setOnStateChanged(new Knob.OnStateChanged() {
            @Override
            public void onState(int state) {
                textView4.setText(Integer.toString(state));
            }
        });


        knob2.setUserBehaviour(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "This is a test click listener\nfor a knob with behaviour 'user'", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        bt.stop();
        super.onDestroy();
    }
}
