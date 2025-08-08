module net.tessa.mcmtforge.syncfu {
    requires cpw.mods.modlauncher;
    requires org.apache.logging.log4j;
    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;
    provides cpw.mods.modlauncher.api.ITransformationService with net.tessa.mcmtforge.syncfu.SyncFuTransformer;

}