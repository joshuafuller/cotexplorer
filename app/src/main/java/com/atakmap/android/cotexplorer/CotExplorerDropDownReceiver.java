package com.atakmap.android.cotexplorer;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;

import com.atakmap.android.contact.ContactPresenceDropdown;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.cot.importer.CotImporterManager;
import com.atakmap.android.gui.EditText;
import com.atakmap.android.importexport.CotEventFactory;
import com.atakmap.android.importexport.send.SendDialog;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;

import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.AbstractMapItemSelectionTool;
import com.atakmap.android.cotexplorer.plugin.R;
import com.atakmap.comms.CommsLogger;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.android.cotexplorer.plugin.PluginNativeLoader;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class CotExplorerDropDownReceiver extends DropDownReceiver implements
        OnStateListener, CommsLogger, View.OnClickListener {

    public static final String TAG = "cotexplorer";

    public static final String SHOW_PLUGIN = "com.atakmap.android.cotexplorer.SHOW_PLUGIN";
    private final Context pluginContext;
    private final Context appContext;
    private final MapView mapView;
    private final View mainView;
    final InspectionMapItemSelectionTool imis;

    private boolean paused = false;
    private RecyclerView cotexplorerlog = null;
    private LogAdapter logAdapter;
    private Button sendBtn, clearBtn, pauseBtn, saveBtn, inspectBtn = null;
    private ImageButton filterBtn = null;
    private SharedPreferences _sharedPreference = null;
    private String cotFilter = "";
    private List<String> fullLog = new ArrayList<>();
    private static boolean isRegistered = false;

    /**************************** CONSTRUCTOR *****************************/

    public CotExplorerDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);
        this.pluginContext = context;
        this.appContext = mapView.getContext();
        this.mapView = mapView;
        this.imis = new InspectionMapItemSelectionTool();

        if (!isRegistered) {
            CommsMapComponent.getInstance().registerCommsLogger(this);
            isRegistered = true; // Prevent duplicate registrations
        }

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mainView = inflater.inflate(R.layout.main_layout, null);
        cotexplorerlog = mainView.findViewById(R.id.cotexplorerlog);
        logAdapter = new LogAdapter(convertToSpannableList(fullLog));// Initialize adapter with your log list
        cotexplorerlog.setLayoutManager(new LinearLayoutManager(context));
        cotexplorerlog.setAdapter(logAdapter);
        clearBtn = mainView.findViewById(R.id.clearBtn);
        pauseBtn = mainView.findViewById(R.id.pauseBtn);
        filterBtn = mainView.findViewById(R.id.filterBtn);
        saveBtn = mainView.findViewById(R.id.saveBtn);
        sendBtn = mainView.findViewById(R.id.sendBtn);
        inspectBtn = mainView.findViewById(R.id.inspectBtn);

        _sharedPreference = PreferenceManager.getDefaultSharedPreferences(mapView.getContext().getApplicationContext());

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditText et = new EditText(mapView.getContext());

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(mapView.getContext());
                alertDialog.setTitle("Enter CoT to send");
                alertDialog.setView(et);
                alertDialog.setNegativeButton("Cancel", null);

                alertDialog.setPositiveButton("Send…", (dialogInterface, i) -> {
                    final String xml = et.getText() != null ? et.getText().toString() : null;
                    if (xml == null || xml.trim().isEmpty()) {
                        Toast.makeText(mapView.getContext(), "No CoT XML", Toast.LENGTH_LONG).show();
                        return;
                    }

                    CotEvent evt = null;
                    try { evt = CotEvent.parse(xml); } catch (Exception ignore) {}
                    if (evt == null || !evt.isValid()) {
                        Toast.makeText(mapView.getContext(), "Invalid CoT", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Ensure a stable UID
                    if (evt.getUID() == null || evt.getUID().trim().isEmpty()) {
                        try {
                            evt.setUID(java.util.UUID.randomUUID().toString());
                        } catch (Throwable t) {
                            String fixedXml = xml.replaceFirst("<event\\b",
                                    "<event uid=\"" + java.util.UUID.randomUUID() + "\"");
                            try {
                                evt = CotEvent.parse(fixedXml);
                            } catch (Exception e2) {
                                Toast.makeText(mapView.getContext(), "Invalid CoT (no uid)", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                    }

                    // Import locally
                    CommsMapComponent.ImportResult r;
                    try {
                        CotImporterManager mgr = CotImporterManager.getInstance();
                        if (mgr == null) {
                            Toast.makeText(mapView.getContext(), "Importer not ready", Toast.LENGTH_LONG).show();
                            return;
                        }
                        r = mgr.importData(evt, new Bundle());
                    } catch (Exception e) {
                        Toast.makeText(mapView.getContext(), "Import error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (r == CommsMapComponent.ImportResult.SUCCESS || r == CommsMapComponent.ImportResult.IGNORE) {
                        final String uid = evt.getUID();
                        MapItem item = mapView.getMapItem(uid);
                        if (item != null) {
                            openContactListForSend(item);
                        } else {
                            // wait briefly for UI-thread creation of the item
                            CotEvent finalEvt = evt;
                            waitForMapItem(uid, 750, 75,
                                    () -> {
                                        MapItem mi = mapView.getMapItem(uid);
                                        if (mi != null) openContactListForSend(mi);
                                        else {
                                            CotMapComponent.getExternalDispatcher().dispatch(finalEvt);
                                            Toast.makeText(mapView.getContext(),
                                                    "Sent via broadcast (no MapItem)", Toast.LENGTH_SHORT).show();
                                        }
                                    },
                                    () -> {
                                        CotMapComponent.getExternalDispatcher().dispatch(finalEvt);
                                        Toast.makeText(mapView.getContext(),
                                                "Sent via broadcast (timeout waiting for MapItem)",
                                                Toast.LENGTH_SHORT).show();
                                    }
                            );
                        }
                    } else {
                        // Import failed – fall back to immediate broadcast
                        CotMapComponent.getExternalDispatcher().dispatch(evt);
                        Toast.makeText(mapView.getContext(),
                                "Import failed; broadcast sent", Toast.LENGTH_SHORT).show();
                    }
                });
                alertDialog.setNeutralButton("Plot", (dialogInterface, i) -> {
                    processCotXml(et.getContext(), et.getText().toString());
                });

                alertDialog.show();
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the directory path from PluginNativeLoader
                String dirPath = PluginNativeLoader.getCotExplorerDir();
                Log.d(TAG, "Directory path: " + dirPath);

                // Generate timestamped filename
                String timestamp = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.getDefault())
                        .format(new Date());
                File file = new File(dirPath, "cotexplorer-" + timestamp + ".txt");

                try {
                    FileWriter fw = new FileWriter(file);
                    // Iterate through fullLog and write each entry to the file
                    for (String log : fullLog) {
                        fw.write(log + "\n");
                    }
                    fw.flush();
                    fw.close();

                    Toast.makeText(mapView.getContext(),
                            "Log written to " + file.getAbsolutePath(),
                            Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to save log file", e);
                    Toast.makeText(mapView.getContext(),
                            "Error saving log file",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fullLog.clear(); // Clear the list of logs
                logAdapter.clearLogs(); // Clear the logs in the adapter and notify the RecyclerView
            }
        });

        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (paused) {
                    pauseBtn.setText("Pause");
                    paused = false;
                } else {
                    pauseBtn.setText("Paused");
                    paused = true;
                }
            }
        });

        filterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(mapView.getContext());
                alertBuilder.setTitle("Set filter");
                final EditText input = new EditText(mapView.getContext());
                input.setText(cotFilter);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                alertBuilder.setView(input);

                alertBuilder.setPositiveButton("OK", (dialogInterface, i) -> {
                    cotFilter = input.getText().toString().trim();
                    Log.i(TAG, "Filter set to: '" + cotFilter + "'");
                    _sharedPreference.edit().putString("plugin_cotexplorer_type", cotFilter).apply(); // Save immediately
                    applyFilter();
                });

                alertBuilder.setNegativeButton("Cancel", (dialogInterface, i) -> {});

                alertBuilder.setNeutralButton("Clear", (dialogInterface, i) -> {
                    cotFilter = ""; // Properly reset the filter
                    _sharedPreference.edit().remove("plugin_cotexplorer_type").apply(); // Ensure no old values persist
                    Log.i(TAG, "Filter cleared.");
                    applyFilter();
                });

                alertBuilder.setCancelable(true);
                alertBuilder.show();
            }
        });

        final BroadcastReceiver inspectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                AtakBroadcast.getInstance().unregisterReceiver(this);
                final Button itemInspect = mainView
                        .findViewById(R.id.inspectBtn);
                itemInspect.setSelected(false);

                String uid = intent.getStringExtra("uid");
                if (uid == null)
                    return;

                MapItem mi = getMapView().getMapItem(uid);

                if (mi == null)
                    return;

                com.atakmap.coremap.log.Log.d(TAG, "Full MapItem dump: " + mi.toString());

                final MapItem itemToInspect = ATAKUtilities.findAssocShape(mi);
                final CotEvent cotEvent = CotEventFactory
                        .createCotEvent(itemToInspect);

                String val;
                if (cotEvent != null)
                    val = cotEvent.toString();
                else
                    val = "error turning a map item into CoT";

                AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                        getMapView().getContext());
                TextView showText = new TextView(getMapView().getContext());
                showText.setText(prettyPrintXml(val));
                showText.setTextIsSelectable(true);
                showText.setPadding(32, 32, 32, 32); // Add padding (left, top, right, bottom) in pixels
                showText.setOnLongClickListener(new View.OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        // Copy the Text to the clipboard
                        ClipboardManager manager = (ClipboardManager) getMapView()
                                .getContext()
                                .getSystemService(Context.CLIPBOARD_SERVICE);
                        TextView showTextParam = (TextView) v;
                        manager.setText(showTextParam.getText());
                        Toast.makeText(v.getContext(),
                                "Copied to clipboard", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });

                builderSingle.setTitle("CoT Inspector");
                builderSingle.setView(showText);
                builderSingle.setPositiveButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss(); // Close the dialog
                    }
                });
                builderSingle.show();
            }
        };

        inspectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean val = inspectBtn.isSelected();
                if (val) {
                    imis.requestEndTool();
                } else {

                    AtakBroadcast.getInstance().registerReceiver(
                            inspectionReceiver,
                            new AtakBroadcast.DocumentedIntentFilter(
                                    "com.atakmap.android.cotexplorer.InspectionMapItemSelectionTool.Finished"));
                    Bundle extras = new Bundle();
                    ToolManagerBroadcastReceiver.getInstance().startTool(
                            "com.atakmap.android.cotexplorer.InspectionMapItemSelectionTool",
                            extras);

                }
                inspectBtn.setSelected(!val);
            }
        });
    }

    private List<SpannableString> convertToSpannableList(List<String> logs) {
        List<SpannableString> spannableLogs = new ArrayList<>();
        for (String log : logs) {
            spannableLogs.add(new SpannableString(log));
        }
        return spannableLogs;
    }

    public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
        private List<SpannableString> logs;

        public LogAdapter(List<SpannableString> logs) {
            this.logs = logs;
        }

        public void addLog(SpannableString log) {
            logs.add(log);
            notifyItemInserted(logs.size() - 1);
        }

        public void updateLogs(List<SpannableString> newLogs) {
            logs.clear();
            logs.addAll(newLogs);
            notifyDataSetChanged(); // Properly update RecyclerView
        }

        // Method to clear logs
        public void clearLogs() {
            logs.clear();
            notifyDataSetChanged();
        }

        @Override
        public LogViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new LogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(LogViewHolder holder, int position) {
            SpannableString logEntry = logs.get(position);

            // Create a new SpannableStringBuilder to append the separator
            SpannableStringBuilder logWithSeparator = new SpannableStringBuilder();
            logWithSeparator.append(logEntry);
            logWithSeparator.append("\n----------\n"); // Add a newline separator

            holder.logText.setText(logWithSeparator);

            // Adjust text size dynamically
            float textSize = getDynamicTextSize(holder.logText.getContext());
            holder.logText.setTextSize(textSize);
        }

        private float getDynamicTextSize(Context context) {
            float screenWidthDp = context.getResources().getDisplayMetrics().widthPixels /
                    context.getResources().getDisplayMetrics().density;

            if (screenWidthDp >= 600) {
                return 16; // Tablets
            } else if (screenWidthDp >= 360) {
                return 14; // Phones
            } else {
                return 12; // Smaller devices
            }
        }

        @Override
        public int getItemCount() {
            return logs.size();
        }

        public class LogViewHolder extends RecyclerView.ViewHolder {
            TextView logText;
            private boolean selecting = false;

            public LogViewHolder(View itemView) {
                super(itemView);
                logText = itemView.findViewById(android.R.id.text1);

                logText.setTextIsSelectable(true);
                logText.setLongClickable(true);
                logText.setFocusable(true);
                logText.setFocusableInTouchMode(true);

                logText.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
                    @Override public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                        selecting = true;
                        ViewParent p = logText.getParent();
                        if (p != null) p.requestDisallowInterceptTouchEvent(true);
                        return true;
                    }
                    @Override public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) { return false; }
                    @Override public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) { return false; }
                    @Override public void onDestroyActionMode(android.view.ActionMode mode) {
                        selecting = false;
                        ViewParent p = logText.getParent();
                        if (p != null) p.requestDisallowInterceptTouchEvent(false);
                    }
                });

                logText.setOnTouchListener((v, ev) -> {
                    ViewParent p = v.getParent();
                    switch (ev.getActionMasked()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                        case android.view.MotionEvent.ACTION_MOVE:
                            if (p != null) p.requestDisallowInterceptTouchEvent(selecting);
                            break;
                        case android.view.MotionEvent.ACTION_UP:
                        case android.view.MotionEvent.ACTION_CANCEL:
                            if (p != null) p.requestDisallowInterceptTouchEvent(false);
                            break;
                    }
                    return false; // let TextView handle selection
                });
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action != null && action.equals(SHOW_PLUGIN)) {
            showDropDown(mainView, HALF_WIDTH, FULL_HEIGHT,
                    FULL_WIDTH, HALF_HEIGHT, false, this);
        }
    }

    private void writeLog(final String log) {
        if (paused) return;

        new Handler(Looper.getMainLooper()).post(() -> {
            SpannableString spannableLog = new SpannableString(log);

            if (!cotFilter.isEmpty() && log.contains(cotFilter)) {
                int start = log.indexOf(cotFilter);
                int end = start + cotFilter.length();
                spannableLog.setSpan(
                        new BackgroundColorSpan(0x80FFFF33),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }

            if (cotFilter.isEmpty() || log.contains(cotFilter)) {
                logAdapter.addLog(spannableLog);
                cotexplorerlog.scrollToPosition(logAdapter.getItemCount() - 1);
            }
        });
    }

    private void applyFilter() {
        // Get the latest filter value
        cotFilter = _sharedPreference.getString("plugin_cotexplorer_type", "").trim();
        Log.i(TAG, "🔹 Applying filter: '" + cotFilter + "'");
        Log.i(TAG, "Log size before filtering: " + fullLog.size());

        // Create a list of filtered logs based on fullLog
        List<SpannableString> filteredLogs = new ArrayList<>();

        // Iterate through fullLog to filter the messages
        for (String log : fullLog) {
            Log.i(TAG, "Filtering log: " + log);
            if (cotFilter.isEmpty() || log.contains(cotFilter)) { // Apply filter to log type
                SpannableString spannableLog = new SpannableString(log);

                if (!cotFilter.isEmpty()) {
                    int start = log.indexOf(cotFilter);
                    if (start >= 0) {
                        int end = start + cotFilter.length();
                        spannableLog.setSpan(
                                new BackgroundColorSpan(0x80FFFF33), // Highlight filter match
                                start,
                                end,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                    }
                }

                filteredLogs.add(spannableLog); // Add to filtered logs
                Log.i(TAG, "Filtered logs count: " + filteredLogs.size());
            }
        }


        Log.i(TAG, "Filtered logs count after filtering: " + filteredLogs.size());

        // Update RecyclerView with the filtered logs (without modifying fullLog)
        logAdapter.updateLogs(filteredLogs);
        cotexplorerlog.scrollToPosition(logAdapter.getItemCount() - 1);
    }

    final class InspectionMapItemSelectionTool
            extends AbstractMapItemSelectionTool {
        public InspectionMapItemSelectionTool() {
            super(getMapView(),
                    "com.atakmap.android.cotexplorer.InspectionMapItemSelectionTool",
                    "com.atakmap.android.cotexplorer.InspectionMapItemSelectionTool.Finished",
                    "Select Map Item on the screen",
                    "Invalid Selection");
        }

        @Override
        protected boolean isItem(MapItem mi) {
            return true;
        }

    }

    private void processCotXml(Context context, String cotXml) {
        if (cotXml == null || cotXml.trim().isEmpty()) {
            Toast.makeText(context, "No CoT XML to process", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Parse XML -> CotEvent
            CotEvent event = CotEvent.parse(cotXml);
            if (event == null) {
                Toast.makeText(context, "Invalid CoT XML", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the importer manager
            CotImporterManager mgr = CotImporterManager.getInstance();
            if (mgr == null) {
                Toast.makeText(context, "Importer not ready (no Map/Importer instance)", Toast.LENGTH_SHORT).show();
                return;
            }

            // Optional extras for import (keep empty unless you need flags)
            Bundle extras = new Bundle();
            // e.g. extras.putBoolean("centerOnImport", true);

            CommsMapComponent.ImportResult r = mgr.importData(event, extras);
            switch (r) {
                case SUCCESS:
                    Toast.makeText(context, "Processed CoT locally", Toast.LENGTH_SHORT).show();
                    break;
                case IGNORE:
                    Toast.makeText(context, "No importer handled this CoT", Toast.LENGTH_SHORT).show();
                    break;
                case FAILURE:
                default:
                    Toast.makeText(context, "Failed to process CoT", Toast.LENGTH_SHORT).show();
                    break;
            }
        } catch (Exception e) {
            Toast.makeText(context, "Error parsing/processing CoT: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openContactListForSend(MapItem mapItem) {
        // Mirror core behavior: resolve associated shape, ensure shareable
        MapItem itemToSend = ATAKUtilities.findAssocShape(mapItem);
        if (itemToSend == null) itemToSend = mapItem;
        itemToSend.setMetaBoolean("shared", true);

        Intent contactList = new Intent(ContactPresenceDropdown.SEND_LIST);
        contactList.putExtra("targetUID", itemToSend.getUID());
        AtakBroadcast.getInstance().sendBroadcast(contactList);
    }

    private void waitForMapItem(final String uid,
                                final long timeoutMs,
                                final long stepMs,
                                final Runnable onFound,
                                final Runnable onTimeout) {
        final long start = System.currentTimeMillis();
        final Handler h = new Handler(Looper.getMainLooper());

        final Runnable poll = new Runnable() {
            @Override public void run() {
                MapItem mi = mapView.getMapItem(uid);
                if (mi != null) {
                    onFound.run();
                } else if (System.currentTimeMillis() - start >= timeoutMs) {
                    onTimeout.run();
                } else {
                    h.postDelayed(this, stepMs);
                }
            }
        };
        h.post(poll);
    }

    static String prettyPrintXml(String xml) {
        if (xml == null) return null;
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            // Some Android runtimes honor this:
            try { tf.setAttribute("indent-number", 4); } catch (IllegalArgumentException ignored) {}
            Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            // Xalan-style indent hint (works on most Androids)
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            StringWriter out = new StringWriter();
            t.transform(new StreamSource(new StringReader(xml)), new StreamResult(out));
            return out.toString().trim();
        } catch (Exception e) {
            return xml; // fallback: show raw if formatting fails
        }
    }

    @Override
    public void disposeImpl() {
        Log.i(TAG, "Unregistering logReceive()");
        CommsMapComponent.getInstance().unregisterCommsLogger(this);
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

    @Override
    public void logSend(CotEvent cotEvent, String s) {
        Log.i(TAG, "Sent event: " + cotEvent.getType());

        // Ensure we always get the latest filter
        cotFilter = _sharedPreference.getString("plugin_cotexplorer_type", "").trim();
        String eventMessage = String.format("S: %s", cotEvent.toString().trim()); // Use the full message

        Log.i(TAG, "Current filter: '" + cotFilter + "'");
        Log.i(TAG, "Full CoT Message: " + eventMessage);

        synchronized (fullLog) {
            fullLog.add(eventMessage);  // Always add to fullLog, even if duplicate
            Log.i(TAG, "Added to fullLog: " + eventMessage);
        }

        if (cotFilter.isEmpty()) {
            Log.i(TAG, "No filter applied, displaying event.");
            writeLog(eventMessage);
        } else if (eventMessage.contains(cotFilter)) { // Check full message
            Log.i(TAG, "Log matches filter, adding to RecyclerView.");
            writeLog(eventMessage);
        } else {
            Log.i(TAG, "Log does NOT match filter, skipping UI update.");
            Log.i(TAG, "eventMessage.contains(cotFilter) == " + eventMessage.contains(cotFilter));
        }
    }

    @Override
    public void logSend(CotEvent cotEvent, String[] strings) {
        Log.i(TAG, "Sent event: " + cotEvent.getType());

        // Ensure we always get the latest filter
        cotFilter = _sharedPreference.getString("plugin_cotexplorer_type", "").trim();
        String eventMessage = String.format("S: %s", cotEvent.toString().trim()); // Use the full message

        Log.i(TAG, "Current filter: '" + cotFilter + "'");
        Log.i(TAG, "Full CoT Message: " + eventMessage);

        synchronized (fullLog) {
            fullLog.add(eventMessage);  // Always add to fullLog, even if duplicate
            Log.i(TAG, "Added to fullLog: " + eventMessage);
        }

        if (cotFilter.isEmpty()) {
            Log.i(TAG, "No filter applied, displaying event.");
            writeLog(eventMessage);
        } else if (eventMessage.contains(cotFilter)) { // Check full message
            Log.i(TAG, "Log matches filter, adding to RecyclerView.");
            writeLog(eventMessage);
        } else {
            Log.i(TAG, "Log does NOT match filter, skipping UI update.");
            Log.i(TAG, "eventMessage.contains(cotFilter) == " + eventMessage.contains(cotFilter));
        }
    }

    @Override
    public void logReceive(CotEvent cotEvent, String s, String s1) {
        Log.i(TAG, "Received event: " + cotEvent.getType());

        // Ensure we always get the latest filter
        cotFilter = _sharedPreference.getString("plugin_cotexplorer_type", "").trim();
        String eventMessage = String.format("R: %s", cotEvent.toString().trim()); // Use the full message
        Log.i(TAG, "Current filter: '" + cotFilter + "'");
        Log.i(TAG, "Full CoT Message: " + eventMessage);

        synchronized (fullLog) {
            fullLog.add(eventMessage);  // Always add to fullLog, even if duplicate
            Log.i(TAG, "Added to fullLog: " + eventMessage);
        }

        if (cotFilter.isEmpty()) {
            Log.i(TAG, "No filter applied, displaying event.");
            writeLog(eventMessage);
        } else if (eventMessage.contains(cotFilter)) { // Check full message
            Log.i(TAG, "Log matches filter, adding to RecyclerView.");
            writeLog(eventMessage);
        } else {
            Log.i(TAG, "Log does NOT match filter, skipping UI update.");
            Log.i(TAG, "eventMessage.contains(cotFilter) == " + eventMessage.contains(cotFilter));
        }
    }

    @Override
    public void onClick(View view) {
        Log.i(TAG, "onClick");

    }
}