package org.fentanylsolutions.blpatches.core;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.Name("BL Patches Core")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(-1000)
@IFMLLoadingPlugin.TransformerExclusions({ "org.fentanylsolutions.blpatches.core." })
public class BlPatchesLoadingPlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        BetweenlandsJarRuntimePatcher.patchIfNecessary(data);
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
