package org.fentanylsolutions.blpatches;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = BlPatchesMod.MOD_ID,
    name = "BL Patches",
    version = Tags.VERSION,
    dependencies = "required-after:thebetweenlands",
    customProperties = { @Mod.CustomProperty(k = "license", v = "LGPLv3+SNEED") },
    acceptedMinecraftVersions = "[1.7.10]")
public class BlPatchesMod {

    public static final String MOD_ID = "blpatches";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    @SidedProxy(
        clientSide = "org.fentanylsolutions.blpatches.ClientProxy",
        serverSide = "org.fentanylsolutions.blpatches.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}
