package com.naganath.cs478.project4;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

public class gameActivity extends AppCompatActivity {

    private static final int GRID_SIZE = 10;

    private GridLayout gridLayout;
    private Thread currentThread;
    private Runnable currentRunnable;
    private Handler uiHandler;
    private RandomThread randomThread;
    private IterativeThread iterativeThread;
    private int curPlayerColor;
    private int iteratorValue = 0;
    private int gopherIndex = -1;
    private TextView curTextView;
    private Boolean isAuto = Boolean.TRUE;
    private static final int SLEEP_TIME = 500;

    View.OnClickListener modeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            isAuto = !isAuto;
            int textRId = -1;
            if(isAuto) {
                textRId = R.string.manual;
                runNextThread(SLEEP_TIME);
            } else  {
                textRId = R.string.automate;
            }
            ((Button)v).setText(textRId);
            findViewById(R.id.next_button).setEnabled(!isAuto);
        }
    };

    View.OnClickListener nextClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            runNextThread(0);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        gridLayout = findViewById(R.id.grid_layout);
        populateGridLayout(gridLayout);
        curPlayerColor = R.color.p1_color;
        gopherIndex = new Random().nextInt(100);
        gridLayout.getChildAt(gopherIndex).setBackgroundColor(Color.BLACK);
        findViewById(R.id.mode_button).setOnClickListener(modeClickListener);
        findViewById(R.id.next_button).setOnClickListener(nextClickListener);
        findViewById(R.id.next_button).setEnabled(!isAuto);
        initializeITC();
        runNextThread(SLEEP_TIME);
    }

    private void killThreads() {
        if(randomThread.isAlive())
            randomThread.interrupt();
        if(iterativeThread.isAlive())
            iterativeThread.interrupt();
    }

    @Override
    protected void onStop() {
        super.onStop();
        killThreads();
    }

    private void runNextThread(int sleepTime) {
        currentRunnable = getNextRunnable(currentRunnable);
        if(currentThread == null || currentThread == randomThread) {
            currentThread = iterativeThread;
            curTextView = findViewById(R.id.p1_display);
            iterativeThread.iterativeHandler.postDelayed(currentRunnable, 1 * sleepTime);
        } else  {
            curTextView = findViewById(R.id.p2_display);
            currentThread = randomThread;
            randomThread.randomHandler.postDelayed(currentRunnable, 1 * sleepTime);
        }
    }

    private Runnable getNextRunnable(Runnable runnable) {
        if( runnable == null || runnable.getClass().toString().equals(RandomIdGenerator.class.toString()) ) {
            return new IterativeIdGenerator(GRID_SIZE, iteratorValue++);
        }else {
            return new RandomIdGenerator(GRID_SIZE);
        }
    }

    @SuppressLint("HandlerLeak")
    private void initializeITC() {
        uiHandler = new Handler() {
            public void handleMessage(Message msg) {
                int index = -1;
                switch (msg.what) {
                    case 0:
                        index = msg.arg1;
                        TextView textView = (TextView) gridLayout.getChildAt(index);

                        if(gopherIndex == index) {
                            curTextView.setText("Success");

                            String winningText =  currentThread == randomThread ? "Player 2 ": "Player 1 ";
                            winningText +=  "Wins !";
                            Toast.makeText(getApplicationContext(), winningText, Toast.LENGTH_LONG).show();
                            killThreads();
                            return;
                        }else if("V".equals(textView.getTag() )) {
                            curTextView.setText("Disaster");
                        }
                        else if(isMatch(index, 1)) {
                            curTextView.setText("Near Miss");
                        }else if(isMatch(index, 2)) {
                            curTextView.setText("Close Guess");
                        }else {
                            curTextView.setText("Complete Miss");
                        }

                        textView.setBackgroundResource(curPlayerColor);
                        textView.setTag("V");
                        curPlayerColor = curPlayerColor == R.color.p1_color ? R.color.p2_color : R.color.p1_color;
                        if (isAuto)
                            runNextThread(SLEEP_TIME);

                }

            }
        };
        randomThread = new RandomThread();
        randomThread.start();
        iterativeThread = new IterativeThread();
        iterativeThread.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean isMatch( int curIndex, int boundary) {
        int row = gopherIndex / 10;
        int rowStart = row - boundary  >= 0 ? row- boundary : 0;
        int rowEnd = row + boundary < GRID_SIZE ? row + boundary : GRID_SIZE-1;
        int col = gopherIndex % 10;
        int colStart = col - boundary >= 0 ? col - boundary : 0;
        int colEnd = col + boundary < GRID_SIZE ? col - boundary : GRID_SIZE  -1 ;
        int curRow = curIndex / 10;
        int curCol = curIndex % 10;
        return (rowStart <= curRow  && curRow <= rowEnd) && (colStart <= curCol && curCol <=colEnd);
    }


    private void populateGridLayout(GridLayout gridLayout) {

        gridLayout.setAlignmentMode(GridLayout.ALIGN_MARGINS);
        gridLayout.setColumnCount(GRID_SIZE);
        gridLayout.setRowCount(GRID_SIZE);

        for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
            TextView textView = new TextView(getApplicationContext());
            textView.setBackgroundResource(R.color.blankColor);

            LayoutParams param = new LayoutParams();
            param.height = 100;
            param.width = 100;
            param.setMargins(0, 20, 20, 0);
            param.setGravity(Gravity.CENTER);
            param.columnSpec = GridLayout.spec(i % GRID_SIZE);
            param.rowSpec = GridLayout.spec(i / GRID_SIZE);

            textView.setLayoutParams(param);
            gridLayout.addView(textView, i);
        }
    }


    class RandomThread extends Thread {
        public Handler randomHandler;
        @Override
        public void run(){
            Looper.prepare();
            randomHandler = new Handler();
            Looper.loop();
        }
    }

    class RandomIdGenerator implements  Runnable {
        private int gridSize;
        private Random random;
        public RandomIdGenerator(int gridSize) {
            this.gridSize = gridSize;
            random = new Random();
        }

        @Override
        public void run() {
            int randomDigit = random.nextInt(gridSize * gridSize);
            uiHandler.obtainMessage(0, randomDigit, randomDigit).sendToTarget();
        }
    }

    class IterativeThread extends  Thread {
        public Handler iterativeHandler;
        @Override
        public void run() {
            Looper.prepare();
            iterativeHandler = new Handler() ;
            Looper.loop();
        }
    }

    class IterativeIdGenerator implements Runnable {

        private int gridSize;
        private int index;
        public IterativeIdGenerator(int gridSize, int index) {
            this.gridSize = gridSize;
            this.index = index;
        }
        @Override
        public void run() {
            if( index < -1 || gridSize * GRID_SIZE < index) {
                index = 0;
            }

            uiHandler.obtainMessage(0, index, index).sendToTarget();
        }

    }
}
