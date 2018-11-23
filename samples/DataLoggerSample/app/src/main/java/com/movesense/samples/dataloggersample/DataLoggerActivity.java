package com.movesense.samples.dataloggersample;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsResponseListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.util.ArrayList;

public class DataLoggerActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private static final String URI_MDS_LOGBOOK_ENTRIES = "suunto://MDS/Logbook/{0}/Entries";
    private static final String URI_MDS_LOGBOOK_DATA= "suunto://MDS/Logbook/{0}/ById/{1}/Data";

    private static final String URI_LOGBOOK_ENTRIES = "suunto://{0}/Mem/Logbook/Entries";
    private static final String URI_DATALOGGER_STATE = "suunto://{0}/Mem/DataLogger/State";
    private static final String URI_DATALOGGER_CONFIG = "suunto://{0}/Mem/DataLogger/Config";

    static DataLoggerActivity s_INSTANCE = null;
    private static final String LOG_TAG = DataLoggerActivity.class.getSimpleName();

    public static final String SERIAL = "serial";
    String connectedSerial;

    private DataLoggerState mDLState;
    private TextView mDataLoggerStateTextView;

    private ListView mLogEntriesListView;
    private static ArrayList<MdsLogbookEntriesResponse.LogEntry> mLogEntriesArrayList = new ArrayList<>();
    ArrayAdapter<MdsLogbookEntriesResponse.LogEntry> mLogEntriesArrayAdapter;

    public static final String SCHEME_PREFIX = "suunto://";

    private Mds getMDS() {return MainActivity.mMds;}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        s_INSTANCE = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datalogger);

        // Init state UI
        mDataLoggerStateTextView = (TextView)findViewById(R.id.textViewDLState);

        // Init Log list
        mLogEntriesListView = (ListView)findViewById(R.id.listViewLogbookEntries);
        mLogEntriesArrayAdapter = new ArrayAdapter<MdsLogbookEntriesResponse.LogEntry>(this,
                android.R.layout.simple_list_item_1, mLogEntriesArrayList);
        mLogEntriesListView.setAdapter(mLogEntriesArrayAdapter);
        mLogEntriesListView.setOnItemClickListener(this);


        // Find serial in opening intent
        Intent intent = getIntent();
        connectedSerial = intent.getStringExtra(SERIAL);

        updateDataLoggerUI();

        configureDataLogger();
        fetchDataLoggerState();
        refreshLogList();
    }

    private void updateDataLoggerUI() {
        Log.d(LOG_TAG, "updateDataLoggerUI() state: " + mDLState);

        mDataLoggerStateTextView.setText(mDLState != null ? mDLState.toString() : "--");
        findViewById(R.id.buttonStartLogging).setEnabled(mDLState != null);
        findViewById(R.id.buttonStopLogging).setEnabled(mDLState != null);

        if (mDLState != null) {
            if (mDLState.content == 2) {
                findViewById(R.id.buttonStartLogging).setVisibility(View.VISIBLE);
                findViewById(R.id.buttonStopLogging).setVisibility(View.GONE);
            }
            if (mDLState.content == 3) {
                findViewById(R.id.buttonStopLogging).setVisibility(View.VISIBLE);
                findViewById(R.id.buttonStartLogging).setVisibility(View.GONE);
            }
        }
    }

    private void configureDataLogger() {
        // Access the DataLogger/Config
        String configUri = MessageFormat.format(URI_DATALOGGER_CONFIG, connectedSerial);

        // Create the config object
        DataLoggerConfig.DataEntry[] entries = {new DataLoggerConfig.DataEntry("/Meas/Acc/13")};
        DataLoggerConfig config = new DataLoggerConfig(new DataLoggerConfig.Config(new DataLoggerConfig.DataEntries(entries)));
        String jsonConfig = new Gson().toJson(config,DataLoggerConfig.class);

        Log.d(LOG_TAG, "Config request: " + jsonConfig);
        getMDS().put(configUri, jsonConfig, new MdsResponseListener() {
            @Override
            public void onSuccess(String data) {
                updateDataLoggerUI();
                Log.i(LOG_TAG, "PUT config succesful: " + data);
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "PUT DataLogger/Config returned error: " + e);
            }
        });
    }

    private void fetchDataLoggerState() {
        // Access the DataLogger/State
        String stateUri = MessageFormat.format(URI_DATALOGGER_STATE, connectedSerial);

        getMDS().get(stateUri, null, new MdsResponseListener() {
            @Override
            public void onSuccess(String data) {
                Log.i(LOG_TAG, "GET state succesful: " + data);

                mDLState = new Gson().fromJson(data, DataLoggerState.class);
                updateDataLoggerUI();
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "GET DataLogger/State returned error: " + e);
            }
        });
    }

    private void setDataLoggerState(boolean bStartLogging) {
        // Access the DataLogger/State
        String stateUri = MessageFormat.format(URI_DATALOGGER_STATE, connectedSerial);

        int newState = bStartLogging ? 3 : 2;
        String payload = "{\"newState\":" + newState + "}";
        getMDS().put(stateUri, payload, new MdsResponseListener() {
            @Override
            public void onSuccess(String data) {
                Log.i(LOG_TAG, "PUT state succesful: " + data);

                mDLState.content = newState;
                updateDataLoggerUI();
                // Update log list if we stopped
                if (!bStartLogging)
                    refreshLogList();
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "PUT DataLogger/State returned error: " + e);
            }
        });
    }

    public void onStartLoggingClicked(View view) {
        setDataLoggerState(true);
    }

    public void onStopLoggingClicked(View view) {
        setDataLoggerState(false);
    }

    private void refreshLogList() {
        // Access the /Logbook/Entries
        String entriesUri = MessageFormat.format(URI_MDS_LOGBOOK_ENTRIES, connectedSerial);

        getMDS().get(entriesUri, null, new MdsResponseListener() {
            @Override
            public void onSuccess(String data) {
                Log.i(LOG_TAG, "GET LogEntries succesful: " + data);

                MdsLogbookEntriesResponse entriesResponse = new Gson().fromJson(data, MdsLogbookEntriesResponse.class);
                findViewById(R.id.buttonRefreshLogs).setEnabled(true);

                mLogEntriesArrayList.clear();
                for (MdsLogbookEntriesResponse.LogEntry logEntry : entriesResponse.logEntries) {
                    Log.d(LOG_TAG, "Entry: " + logEntry);
                    mLogEntriesArrayList.add(logEntry);
                }
                mLogEntriesArrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "GET LogEntries returned error: " + e);
            }
        });
    }

    public void onRefreshLogsClicked(View view) {
        refreshLogList();
    }

    public void onEraseLogsClicked(View view) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("Erase Logs")
                .setMessage("Are you sure you want to wipe all logbook entries?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        eraseAllLogs();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }

    private void eraseAllLogs() {
        // Access the Logbook/Entries resource
        String entriesUri = MessageFormat.format(URI_LOGBOOK_ENTRIES, connectedSerial);

        findViewById(R.id.buttonStartLogging).setEnabled(false);
        findViewById(R.id.buttonStopLogging).setEnabled(false);
        findViewById(R.id.buttonRefreshLogs).setEnabled(false);

        getMDS().delete(entriesUri, null, new MdsResponseListener() {
            @Override
            public void onSuccess(String data) {
                Log.i(LOG_TAG, "DELETE LogEntries succesful: " + data);
                refreshLogList();
                updateDataLoggerUI();
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "DELETE LogEntries returned error: " + e);
                refreshLogList();
                updateDataLoggerUI();
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG,"onDestroy()");

        if (mDLState != null && mDLState.content == 3) {
            setDataLoggerState(false);
        }
        DataLoggerActivity.s_INSTANCE = null;

        super.onDestroy();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MdsLogbookEntriesResponse.LogEntry entry = mLogEntriesArrayList.get(position);
        fetchLogEntry(entry.id);
    }

    private void fetchLogEntry(final int id) {
        findViewById(R.id.headerProgress).setVisibility(View.VISIBLE);
        // GET the /MDS/Logbook/Data proxy
        String logDataUri = MessageFormat.format(URI_MDS_LOGBOOK_DATA, connectedSerial, id);
        final Context me = this;
        getMDS().get(logDataUri, null, new MdsResponseListener() {
            @Override
            public void onSuccess(final String data) {
                String loggableData = data.substring(0, Math.min(8192, data.length()));
                Log.i(LOG_TAG, "GET Log Data succesful: " + loggableData);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(me)
                        .setTitle("Log JSON Data")
                        .setMessage(loggableData).setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        saveLogToFile("MovesenseLog_" + id, data);
                                    }
                                }
                        );

                findViewById(R.id.headerProgress).setVisibility(View.GONE);
                alertDialogBuilder.show();
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "GET Log Data returned error: " + e);
                findViewById(R.id.headerProgress).setVisibility(View.GONE);
            }
        });
    }

    private void saveLogToFile(String filename, String data) {
        // Get the directory for the user's public pictures directory.
        final File path =
                Environment.getExternalStoragePublicDirectory
                        (
                                Environment.DIRECTORY_DOWNLOADS + "/MovesenseLogs/"
                        );

        // Make sure the path directory exists.
        if(!path.exists())
        {
            // Make it, if it doesn't exit
            path.mkdirs();
        }

        final File file = new File(path, filename + ".json");

        // Save data to the file
        Log.d(LOG_TAG, "Writing data to file: " + file.getAbsolutePath());

        try
        {
            FileOutputStream fOut = new FileOutputStream(file.getAbsolutePath(), false);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);

            // Write in pieces in case the file is big
            final int BLOCK_SIZE= 4096;
            for (int startIdx=0;startIdx<data.length();startIdx+=BLOCK_SIZE) {
                int endIdx = Math.min(data.length(), startIdx + BLOCK_SIZE);
                myOutWriter.write(data.substring(startIdx, endIdx));
            }

            myOutWriter.flush();
            myOutWriter.close();

            fOut.flush();
            fOut.close();
        }
        catch (IOException e)
        {
            Log.e(LOG_TAG, "File write failed: ", e);
        }

        // re-scan files so that they get visible in Windows
        MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, null);
    }

    public void onCreateNewLogClicked(View view) {
        createNewLog();
    }

    private void createNewLog() {
        // Access the Logbook/Entries resource
        String entriesUri = MessageFormat.format(URI_LOGBOOK_ENTRIES, connectedSerial);

        getMDS().post(entriesUri, null, new MdsResponseListener() {
            @Override
            public void onSuccess(String data) {
                Log.i(LOG_TAG, "POST LogEntries succesful: " + data);
                IntResponse logIdResp = new Gson().fromJson(data, IntResponse.class);

                TextView tvLogId = (TextView)findViewById(R.id.textViewCurrentLogID);
                tvLogId.setText("" + logIdResp.content);
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "POST LogEntries returned error: " + e);
                TextView tvLogId = (TextView)findViewById(R.id.textViewCurrentLogID);
                tvLogId.setText("##");
            }
        });

    }
}
