
package com.atakmap.android.cotexplorer.plugin;

import android.content.Context;

import com.atak.plugins.impl.AbstractPluginTool;
import gov.tak.api.util.Disposable;

public class CotExplorerTool extends AbstractPluginTool implements Disposable {

    private final static String TAG = "CotExplorerTool";

    public CotExplorerTool(Context context) {
        super(context,
                context.getString(R.string.app_name),
                context.getString(R.string.app_name),
                context.getResources().getDrawable(R.drawable.cot_explorer),
                "com.atakmap.android.cotexplorer.SHOW_PLUGIN");
        PluginNativeLoader.init(context);
    }

    @Override
    public void dispose() {
    }

}