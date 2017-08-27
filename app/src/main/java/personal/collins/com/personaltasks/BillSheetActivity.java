package personal.collins.com.personaltasks;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.tasks.TasksScopes;

import com.google.api.services.tasks.model.*;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class BillSheetActivity extends AppCompatActivity {
    private int selectedMonth = 1;
    private int selectedYear;
    private Date startDate;
    private Date endDate;

    private String CALENDAR_ID = "grm70h5srpq90vp7v6upsaoce4@group.calendar.google.com";
    GoogleAccountCredential mCredential;
    private TextView mOutputText;
    ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_sheet);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mOutputText = (TextView) findViewById(R.id.results);

        setSupportActionBar(toolbar);

        Spinner months = (Spinner) findViewById(R.id.monthSpinner);
        Spinner years = (Spinner) findViewById(R.id.yearSpinner);
        final Button generate = (Button) findViewById(R.id.generateButton);
        final Button viewBills = (Button) findViewById(R.id.viewBills);

        months.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedMonth = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        int year = Calendar.getInstance().get(Calendar.YEAR);

        final ArrayList<Integer> spinnerYears = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            spinnerYears.add(year + i);
        }

        ArrayAdapter<Integer> yearAdapter = new ArrayAdapter<Integer>(
                getApplicationContext(),
                android.R.layout.simple_spinner_dropdown_item,
                spinnerYears
        );

        years.setAdapter(yearAdapter);

        years.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedYear = spinnerYears.get(adapterView.getSelectedItemPosition());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDate = getDate(selectedMonth, selectedYear);
                endDate = getLastDayOfMonth(selectedMonth, selectedYear);
                generate.setEnabled(false);
                mOutputText.setText("");

                CallableAction toTaskListAction = new CallableAction() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public boolean run(List<Object> events) {
                        List<Event> e = (List<Event>)(Object)events;
                        new TaskRequestTask(mCredential, e)
                                .execute();
                        return true;
                    }
                };

                new CalendarRequestTask(mCredential, toTaskListAction).execute();
                generate.setEnabled(true);
            }
        });

        viewBills.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDate = getDate(selectedMonth, selectedYear);
                endDate = getLastDayOfMonth(selectedMonth, selectedYear);
                viewBills.setEnabled(false);
                mOutputText.setText("");

                CallableAction toTaskListAction = new CallableAction() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public boolean run(List<Object> events) {
                        mProgress.hide();
                        List<Event> bills = (List<Event>)(Object)events;
                        Collections.reverse(bills);

                        for (Event e : bills) {
                            String title = String.format("%s (%s)\n", e.getSummary(), e.getStart().getDate().toString());
                            mOutputText.append(title);
                        }

                        viewBills.setEnabled(true);
                        return true;
                    }
                };

                new CalendarRequestTask(mCredential, toTaskListAction).execute();
            }
        });

        mOutputText.setPadding(16, 16, 16, 16);
        mOutputText.setVerticalScrollBarEnabled(true);
        mOutputText.setMovementMethod(new ScrollingMovementMethod());

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Getting Calendar events ...");

        setupCredentials();
    }

    private void setupCredentials() {
        String accountName = getSharedPreferences(MainActivity.PREFERENCE_FILE_NAME, Context.MODE_PRIVATE)
                .getString(MainActivity.PREF_ACCOUNT_NAME, null);

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(MainActivity.SCOPES))
                .setBackOff(new ExponentialBackOff());

        mCredential.setSelectedAccountName(accountName);
    }

    private Date getDate(int month, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.YEAR, year);
        return calendar.getTime();
    }

    private Date getLastDayOfMonth(int month, int year) {
        Calendar calendar = Calendar.getInstance();
        // passing month-1 because 0-->jan, 1-->feb... 11-->dec
        calendar.set(year, month, 1);
        calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DATE));
        return calendar.getTime();
    }

    /**
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class CalendarRequestTask extends AsyncTask<Void, Void, List<Event>> {
        private com.google.api.services.calendar.Calendar cService = null;
        private Exception mLastError = null;
        private CallableAction action = null;

        CalendarRequestTask(GoogleAccountCredential credential, CallableAction action) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            cService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google calendar stuff")
                    .build();

            this.action = action;
        }

        /**
         * Background task to call Google Tasks API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<Event> doInBackground(Void... params) {
            try {
                List<Event> bills = getBillsFromCalendar();
                return bills;
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the first 10 task lists.
         * @return List of Strings describing task lists, or an empty list if
         *         there are no task lists found.
         * @throws IOException
         */
        private List<Event> getBillsFromCalendar() throws IOException {
            Events events = cService.events().list(CALENDAR_ID)
                    .setTimeMin(new DateTime(startDate))
                    .setTimeMax(new DateTime(endDate))
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            List<Event> items = events.getItems();
            Collections.reverse(items);
            return items;
        }


        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void onPostExecute(List<Event> output) {
            if (output == null || output.size() == 0) {
                mOutputText.setText("No results returned.");
            } else {
                action.run((List<Object>) (Object) output);
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
//                    showGooglePlayServicesAvailabilityErrorDialog(
//                            ((GooglePlayServicesAvailabilityIOException) mLastError)
//                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
//                    startActivityForResult(
//                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
//                            BillSheetActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }

    /**
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class TaskRequestTask extends AsyncTask<Void, Void, String> {
        private com.google.api.services.tasks.Tasks mService = null;
        private Exception mLastError = null;
        private List<Event> bills;

        TaskRequestTask(GoogleAccountCredential credential, List<Event> events) {
            this.bills = events;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.tasks.Tasks.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Tasks API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Tasks API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected String doInBackground(Void... params) {
            try {
                String tasksCreated = writeTasks();
                return tasksCreated;
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the first 10 task lists.
         * @return List of Strings describing task lists, or an empty list if
         *         there are no task lists found.
         * @throws IOException
         */
        private String writeTasks() throws IOException {
            TaskList taskList = new TaskList();
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);

            String month = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());

            taskList.setTitle(String.format("Bills for %s", month));
            TaskList result = mService.tasklists().insert(taskList).execute();

            for (Event e : this.bills) {
                String title = String.format("%s (%s)", e.getSummary(), e.getStart().getDate().toString());
                Task task = new com.google.api.services.tasks.model.Task();
                task.setTitle(title);
                mService.tasks().insert(result.getId(), task).execute();
            }

            return "Task list created";
        }

        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
        }

        @Override
        protected void onPostExecute(String output) {
            mProgress.hide();
            mOutputText.setText(output);
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
//                    showGooglePlayServicesAvailabilityErrorDialog(
//                            ((GooglePlayServicesAvailabilityIOException) mLastError)
//                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
//                    startActivityForResult(
//                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
//                            BillSheetActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }
}
