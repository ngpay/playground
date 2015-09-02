package com.example.sammy.ngpaytest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ShareActionProvider;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends ActionBarActivity implements View.OnClickListener,
        AdapterView.OnItemClickListener {
    TextView mainTextView;
    Button mainButton;
    EditText mainEditText;

    EditText mainPhoneNumberText;

    Button verifyNumberButton;

    ListView mainListView;
    ArrayAdapter mArrayAdapter;
    ArrayList mNameList = new ArrayList();

    JSONAdapter mJSONAdapter;

    ShareActionProvider mShareActionProvider;

    private static final String PREFS = "prefs";
    private static final String PREF_NAME = "name";
    SharedPreferences mSharedPreferences;

    ProgressDialog mDialog;

    private static final String QUERY_URL = "http://openlibrary.org/search.json?q=";

    private static final String TAG = MainActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView phnumTextView = null;

        String mPhoneNumber = getMy10DigitPhoneNumber();
        phnumTextView = (TextView) findViewById(R.id.my_phone_number);
        phnumTextView.setText(mPhoneNumber);


        // 1. Access the TextView defined in layout XML
        // and then set its text
        mainTextView = (TextView) findViewById(R.id.main_textview);
        mainTextView.setText("Aillaa....");

        // 2. Access the Button defined in layout XML
        // and listen for it here
        mainButton = (Button) findViewById(R.id.main_button);
        mainButton.setOnClickListener(this);

//        // and listen for it here
//        verifyNumberButton = (Button) findViewById(R.id.main_verifynumber_btn);
//        verifyNumberButton.setOnClickListener(this);


        mainPhoneNumberText = (EditText) findViewById(R.id.main_phonenumber);

        // 3. Access the EditText defined in layout XML
        mainEditText = (EditText) findViewById(R.id.main_edittext);

        // 4. Access the ListView
        mainListView = (ListView) findViewById(R.id.main_listview);

        // 5. Set this activity to react to list items being pressed
        mainListView.setOnItemClickListener(this);

        // 10. Create a JSONAdapter for the ListView
        mJSONAdapter = new JSONAdapter(this, getLayoutInflater());

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mJSONAdapter);

        // 7. Greet the user, or ask for their name if new
        displayWelcome();

        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Searching for Book");
        mDialog.setCancelable(false);

        registerSMSReader();
    }

    private String getMyPhoneNumber(){
        TelephonyManager mTelephonyMgr;
        mTelephonyMgr = (TelephonyManager)
                getSystemService(Context.TELEPHONY_SERVICE);
        return mTelephonyMgr.getLine1Number();
    }

    private String getMy10DigitPhoneNumber(){
        String s = getMyPhoneNumber();
        return s != null && s.length() > 2 ? s.substring(2) : null;
    }

    public void registerSMSReader() {
        MySMSReceiver BR_smsreceiver = null;
        BR_smsreceiver = new MySMSReceiver();
        BR_smsreceiver.setMainActivityHandler(this);
        IntentFilter fltr_smsreceived = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(BR_smsreceiver, fltr_smsreceived);
    }


    public void verifyNumberClicked(View view) {
        Log.d(TAG, "----------------------------------------------");
        SmsManager sm = SmsManager.getDefault();
        sm.sendTextMessage("09886034902", null, "[ngPay] One time verification code  #123456#", null, null);
//        mainPhoneNumberText.getText().toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu.
        // Adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Access the Share Item defined in menu XML
        MenuItem shareItem = menu.findItem(R.id.menu_item_share);

        // Access the object responsible for
        // putting together the sharing submenu
        if (shareItem != null) {
            mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
        }

        // Create an Intent to share your content
        setShareIntent();

        return true;
    }

    private void setShareIntent() {

        if (mShareActionProvider != null) {

            // create an Intent with the contents of the TextView
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Android Development");
            shareIntent.putExtra(Intent.EXTRA_TEXT, mainTextView.getText());

            // Make sure the provider knows
            // it should work with that Intent
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public void onClick(View v) {

        /*// Take what was typed into the EditText
        // and use in TextView
        mainTextView.setText(mainEditText.getText().toString()
                + " is learning Android development!");

        // Also add that value to the list shown in the ListView
        mNameList.add(mainEditText.getText().toString());

        // 6. The text you'd like to share has changed,
        // and you need to update
        setShareIntent();
        */

        // 9. Take what was typed into the EditText and use in search
        queryBooks(mainEditText.getText().toString());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Log the item's position and contents
        // to the console in Debug
        // Log.d("omg android", position + ": " + mNameList.get(position));

        // 12. Now that the user's chosen a book, grab the cover data
        JSONObject jsonObject = (JSONObject) mJSONAdapter.getItem(position);
        String coverID = jsonObject.optString("cover_i","");

        // create an Intent to take you over to a new DetailActivity
        Intent detailIntent = new Intent(this, DetailActivity.class);

        // pack away the data about the cover
        // into your Intent before you head out
        detailIntent.putExtra("coverID", coverID);

        // TODO: add any other data you'd like as Extras

        // start the next Activity using your prepared Intent
        startActivity(detailIntent);


    }

    public void displayWelcome() {

        // Access the device's key-value storage
        mSharedPreferences = getSharedPreferences(PREFS, MODE_PRIVATE);

        // Read the user's name,
        // or an empty string if nothing found
        String name = mSharedPreferences.getString(PREF_NAME, "");

        if (name.length() > 0) {

            // If the name is valid, display a Toast welcoming them
            Toast.makeText(this, "Welcome back, " + name + "!", Toast.LENGTH_LONG).show();
        }  else {

            // otherwise, show a dialog to ask for their name
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Hello!");
            alert.setMessage("What is your name?");

            // Create EditText for entry
            final EditText input = new EditText(this);
            alert.setView(input);

            // Make an "OK" button to save the name
            alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {

                    // Grab the EditText's input
                    String inputName = input.getText().toString();

                    // Put it into memory (don't forget to commit!)
                    SharedPreferences.Editor e = mSharedPreferences.edit();
                    e.putString(PREF_NAME, inputName);
                    e.commit();

                    // Welcome the new user
                    Toast.makeText(getApplicationContext(), "Welcome, " + inputName + "!", Toast.LENGTH_LONG).show();
                }
            });

            // Make a "Cancel" button
            // that simply dismisses the alert
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {}
            });

            alert.show();
        }
    }


    public void displaySMSVerification(String str) {
        Log.d(TAG, "------------------===========================");
        Log.d(TAG, str);
        if (str.contains("[ngPay]")) {

            Matcher m = Pattern.compile(
                    Pattern.quote("#")
                            + "(.*?)"
                            + Pattern.quote("#")
            ).matcher(str);

            while(m.find()){
                String match = m.group(1);
                System.out.println(">"+match+"<");
                //here you insert 'match' into the list
                Toast.makeText(getApplicationContext(), match, Toast.LENGTH_LONG).show();
                break;
            }

//            Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
        }

    }

    private void queryBooks(String searchString) {

        // Prepare your search string to be put in a URL
        // It might have reserved characters or something
        String urlString = "";
        try {
            urlString = URLEncoder.encode(searchString, "UTF-8");
        } catch (UnsupportedEncodingException e) {

            // if this fails for some reason, let the user know why
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // Create a client to perform networking
        AsyncHttpClient client = new AsyncHttpClient();

        // Show ProgressDialog to inform user that a task in the background is occurring
        mDialog.show();

        // Have the client get a JSONArray of data
        // and define how to respond
        client.get(QUERY_URL + urlString,
                new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(JSONObject jsonObject) {
                        mDialog.dismiss();
                        // Display a "Toast" message
                        // to announce your success
                        Toast.makeText(getApplicationContext(), "Success!", Toast.LENGTH_LONG).show();

                        // 8. For now, just log results
                        // Log.d("omg android", jsonObject.toString());
                        // update the data in your custom method.
                        mJSONAdapter.updateData(jsonObject.optJSONArray("docs"));


                    }

                    @Override
                    public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                        mDialog.dismiss();
                        // Display a "Toast" message
                        // to announce the failure
                        Toast.makeText(getApplicationContext(), "Error: " + statusCode + " " + throwable.getMessage(), Toast.LENGTH_LONG).show();

                        // Log error message
                        // to help solve any problems
                        Log.e("omg android", statusCode + " " + throwable.getMessage());

                        // 11. Dismiss the ProgressDialog

                    }
                });
    }



    /** Called when the user clicks the Send button */
    public void seeCallLogs(View view) {
        Intent intent = new Intent(this, MyCallLogActivity.class);
        startActivity(intent);
    }

    public void seeDeviceInfo(View view) {
        Intent intent = new Intent(this, MyDeviceInfoActivity.class);
        startActivity(intent);
    }

    public void checkInstalledApps(View view){
        Intent intent = new Intent(this, MyInstalledAppsActivity.class);
        startActivity(intent);
    }

    public void checkNetworkUsage(View view){
        Intent intent = new Intent(this, MyNetworkUsageActivity.class);
        startActivity(intent);
    }

    public void checkContactList(View view){
        Intent intent = new Intent(this, MyContactListActivity.class);
        startActivity(intent);
    }

    public void checkSMSList(View view){
        Intent intent = new Intent(this, MySMSListActivity.class);
        startActivity(intent);
    }

    public void checkNotification(View view){
        Intent intent = new Intent(this, MyNotificationActivity.class);
        startActivity(intent);
    }

    public void checkPager(View view){
        Intent intent = new Intent(this, MyPagerActivity.class);
        startActivity(intent);
    }

    public void checkImgSlider(View view){
        Intent intent = new Intent(this, ImageSliderActivity.class);
        startActivity(intent);
    }

}
