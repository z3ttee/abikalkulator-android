package de.zitzmanncedric.abicalc.activities.settings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import de.zitzmanncedric.abicalc.AppCore;
import de.zitzmanncedric.abicalc.R;
import de.zitzmanncedric.abicalc.api.Subject;
import de.zitzmanncedric.abicalc.api.calculation.Average;
import de.zitzmanncedric.abicalc.database.AppDatabase;
import de.zitzmanncedric.abicalc.dialogs.ProgressDialog;
import de.zitzmanncedric.abicalc.listener.OnButtonTouchListener;
import de.zitzmanncedric.abicalc.views.AppActionBar;
import needle.Needle;
import needle.UiRelatedTask;

public class SettingsGoalsActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "SettingsGoalsActivity";

    private AppActionBar actionBar;

    private EditText defaultPointsInput;
    private EditText goalAverageInput;
    private EditText goalPointsInput;

    private boolean keyboardActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_goals);

        actionBar = findViewById(R.id.app_toolbar);
        actionBar.setShowClose(true);
        actionBar.setShowSave(true);
        actionBar.getCloseView().setOnClickListener(this);
        actionBar.getSaveView().setOnClickListener(this);

        defaultPointsInput = findViewById(R.id.input_default_points);
        goalAverageInput = findViewById(R.id.input_goalavg);
        goalPointsInput = findViewById(R.id.input_goalpoints);

        defaultPointsInput.setText(String.valueOf(AppCore.getSharedPreferences().getInt("defaultAVG", 8)));
        goalAverageInput.setText(String.valueOf(AppCore.getSharedPreferences().getFloat("goalAVG", 2.0f)));
        goalPointsInput.setText(String.valueOf(AppCore.getSharedPreferences().getInt("goalPoints", 643)));

        ConstraintLayout containerDefaultPoints = findViewById(R.id.container_input_defaultpoints);
        ConstraintLayout containerGoalAverage = findViewById(R.id.container_input_goalavg);
        ConstraintLayout containerGoalPoints = findViewById(R.id.container_input_goalpoints);

        containerDefaultPoints.setClipToOutline(true);
        containerDefaultPoints.setOnTouchListener(new OnButtonTouchListener());
        containerDefaultPoints.setOnClickListener(v -> {
            if(!keyboardActive) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInputFromWindow(defaultPointsInput.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
                keyboardActive = true;
            }
            defaultPointsInput.requestFocus();
            defaultPointsInput.setSelection(defaultPointsInput.getText().length());

        });
        containerGoalAverage.setClipToOutline(true);
        containerGoalAverage.setOnTouchListener(new OnButtonTouchListener());
        containerGoalAverage.setOnClickListener(v -> {
            if(!keyboardActive) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInputFromWindow(goalAverageInput.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
                keyboardActive = true;
            }
            goalAverageInput.requestFocus();
            goalAverageInput.setSelection(goalAverageInput.getText().length());
        });
        containerGoalPoints.setClipToOutline(true);
        containerGoalPoints.setOnTouchListener(new OnButtonTouchListener());
        containerGoalPoints.setOnClickListener(v -> {
            if(!keyboardActive) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInputFromWindow(goalPointsInput.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
                keyboardActive = true;
            }
            goalPointsInput.requestFocus();
            goalPointsInput.setSelection(goalPointsInput.getText().length());
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        keyboardActive = false;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == actionBar.getCloseView().getId()) {
            finish();
            return;
        }
        if(v.getId() == actionBar.getSaveView().getId()) {
            ProgressDialog dialog = new ProgressDialog(SettingsGoalsActivity.this);
            dialog.setTitle(getString(R.string.notice_settings_beingsaved));
            dialog.show();

            Needle.onBackgroundThread().execute(new UiRelatedTask<Void>() {
                @Override
                protected Void doWork() {
                    try {
                        float goalAverage = Float.parseFloat(goalAverageInput.getText().toString());
                        if(goalAverage < 1.0f) goalAverageInput.setText("1.0");
                        if(goalAverage > 4.0f) goalAverageInput.setText("4.0");

                        try {
                            int goalPoints = Integer.parseInt(goalPointsInput.getText().toString());
                            if(goalPoints > 900) {
                                goalPointsInput.setText(String.valueOf(900));
                            }
                            if(goalPoints < 300) {
                                goalPointsInput.setText(String.valueOf(300));
                            }
                        } catch (Exception ex){
                            goalPointsInput.setText(String.valueOf(300));
                        }

                        try {
                            int defaultPoints = Integer.parseInt(defaultPointsInput.getText().toString());
                            if(defaultPoints > 15) {
                                defaultPointsInput.setText(String.valueOf(15));
                            }
                        } catch (NumberFormatException ex){
                            defaultPointsInput.setText(String.valueOf(0));
                        }

                        AppCore.getSharedPreferences().edit().putInt("defaultAVG", Integer.parseInt(defaultPointsInput.getText().toString())).apply();
                        AppCore.getSharedPreferences().edit().putFloat("goalAVG", Float.parseFloat(goalAverageInput.getText().toString().replace(",",".").substring(0, (Math.min(goalAverageInput.getText().length(), 3))))).apply();
                        AppCore.getSharedPreferences().edit().putInt("goalPoints", Integer.parseInt(goalPointsInput.getText().toString())).apply();

                        // TODO: Update seminarfach grades
                        for (Subject subject : AppDatabase.getInstance().userSubjects) {
                            if(subject.isExam()){
                                for (int i = 0; i < 5; i++) {
                                    int termID = i;
                                    Average.getOfTermAndSubject(subject, termID, (result) -> AppDatabase.getInstance().updateQuickAverage(subject, termID, result));
                                }
                            } else {
                                for (int i = 0; i < 4; i++) {
                                    int termID = i;
                                    Average.getOfTermAndSubject(subject, termID, (result) -> AppDatabase.getInstance().updateQuickAverage(subject, termID, result));
                                }
                            }
                        }
                    } catch (Exception ex){
                        ex.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void thenDoUiRelatedWork(Void aVoid) {
                    dialog.dismiss();
                    Toast.makeText(SettingsGoalsActivity.this, getString(R.string.notice_settings_saved), Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }
}