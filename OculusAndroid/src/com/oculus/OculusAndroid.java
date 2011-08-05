package com.oculus;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class OculusAndroid extends Activity {
	
	private EditText domainText;
	private EditText httpPortText;
	private EditText userNameText;
	private EditText passwordText;
    private Button logInButton;
    private Button exitButton;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.startup);
        
        domainText = (EditText)findViewById(R.id.domainEditText);
        httpPortText = (EditText)findViewById(R.id.httpPortEditText);
        userNameText = (EditText)findViewById(R.id.userNameEditText);
        passwordText = (EditText)findViewById(R.id.passwordEditText);   
        logInButton = (Button) findViewById(R.id.loginButton);
        exitButton = (Button) findViewById(R.id.exitButton);

        logInButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	launchVideoView();
            }
        });
        
        exitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	finish();
            }
        });

    }
    
    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getPreferences(0);
        String restoredText;

        restoredText = prefs.getString("domainText", null);
        if (restoredText != null) {
        	domainText.setText(restoredText, TextView.BufferType.EDITABLE);
        	domainText.setSelection(0);
        }

        restoredText = prefs.getString("httpPortText", "5080");
        if (restoredText != null) {
        	httpPortText.setText(restoredText, TextView.BufferType.EDITABLE);
        	httpPortText.setSelection(0);
        }
        
        restoredText = prefs.getString("userNameText", null);
        if (restoredText != null) {
        	userNameText.setText(restoredText, TextView.BufferType.EDITABLE);
        	userNameText.setSelection(0);
        }

    }

    /**
     * Any time we are paused we need to save away the current state, so it
     * will be restored correctly when we are resumed.
     */
    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences.Editor editor = getPreferences(0).edit();
        editor.putString("domainText", domainText.getText().toString());
        editor.putInt("selection-start", domainText.getSelectionStart());
        editor.putInt("selection-end", domainText.getSelectionEnd());
        
        editor.putString("httpPortText", httpPortText.getText().toString());
        editor.putInt("selection-start", httpPortText.getSelectionStart());
        editor.putInt("selection-end", httpPortText .getSelectionEnd());

        editor.putString("userNameText", userNameText.getText().toString());
        editor.putInt("selection-start", userNameText.getSelectionStart());
        editor.putInt("selection-end", userNameText .getSelectionEnd());

        editor.commit();
    }
    

    protected void launchVideoView() {
        Intent i = new Intent(this, VideoView.class);
        Bundle b = new Bundle();
        b.putString("domainText", domainText.getText().toString());
        b.putString("httpPortText", httpPortText.getText().toString());
        b.putString("userNameText", userNameText.getText().toString());
        b.putString("passwordText", passwordText.getText().toString());
        i.putExtras(b);
        startActivity(i);
        //finish();
    }
        
}