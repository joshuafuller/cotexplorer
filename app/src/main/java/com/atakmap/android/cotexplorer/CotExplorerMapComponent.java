
package com.atakmap.android.cotexplorer;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownMapComponent;

import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.atakmap.coremap.log.Log;
import com.atakmap.android.cotexplorer.plugin.R;

public class CotExplorerMapComponent extends DropDownMapComponent {

    private static final String TAG = "CotExplorerMapComponent";
    private Context pluginContext;
    private CotExplorerDropDownReceiver ddr;

    public void onCreate(final Context context, Intent intent,
            final MapView view) {

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        pluginContext = context;

        ddr = new CotExplorerDropDownReceiver(
                view, context);

        Log.d(TAG, "registering the plugin filter");
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(CotExplorerDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(ddr, ddFilter);

        ToolsPreferenceFragment.register(
                new ToolsPreferenceFragment.ToolPreference(
                        pluginContext.getString(R.string.preferences_title),
                        pluginContext.getString(R.string.preferences_summary),
                        pluginContext.getString(R.string.key_cotexplorer_preferences),
                        pluginContext.getResources().getDrawable(R.drawable.ic_launcher),
                        new PluginPreferencesFragment(
                                pluginContext)));

    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
        ToolsPreferenceFragment.unregister(pluginContext.getString(R.string.key_cotexplorer_preferences));
        unregisterReceiver(context, ddr);
    }

}