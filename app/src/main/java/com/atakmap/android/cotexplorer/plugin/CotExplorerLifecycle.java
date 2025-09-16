
package com.atakmap.android.cotexplorer.plugin;

import com.atak.plugins.impl.AbstractPlugin;
import com.atak.plugins.impl.PluginContextProvider;

import gov.tak.api.plugin.IPlugin;
import gov.tak.api.plugin.IServiceController;

import com.atakmap.android.cotexplorer.CotExplorerMapComponent;

public class CotExplorerLifecycle extends AbstractPlugin implements IPlugin {

    public CotExplorerLifecycle(IServiceController serviceController) {
        super(serviceController, new CotExplorerTool(serviceController.getService(PluginContextProvider.class).getPluginContext()), new CotExplorerMapComponent());
    }
}

