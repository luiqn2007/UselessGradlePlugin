package gradle_plugin;

import lq2007.plugins.gradle_plugin.support.EnumLoopResult;
import lq2007.plugins.gradle_plugin.support.ISourcePlugin;
import lq2007.plugins.gradle_plugin.support.PluginContext;
import lq2007.plugins.gradle_plugin.support.PluginHelper;

import java.nio.file.Path;

public class PluginExp implements ISourcePlugin {

    private boolean finished = false;

    @Override
    public void begin(PluginContext context, PluginHelper helper) {
        System.out.println("Begin " + context.root());
    }

    @Override
    public void each(Path file, PluginContext context, PluginHelper helper) {
        System.out.println("  File: " + file);
    }

    @Override
    public EnumLoopResult finished(PluginContext context, PluginHelper helper) {
        if (!finished) {
            finished = true;
            return EnumLoopResult.CONTINUE;
        }
        return EnumLoopResult.FINISHED;
    }

    @Override
    public Path getLoopRoot(PluginHelper helper) {
        return helper.projectPath();
    }
}
