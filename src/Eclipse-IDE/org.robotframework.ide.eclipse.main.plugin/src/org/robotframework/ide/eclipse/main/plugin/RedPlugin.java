/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.rf.ide.core.executor.RedSystemProperties;
import org.rf.ide.core.executor.RobotRuntimeEnvironment;
import org.rf.ide.core.fileWatcher.RedFileWatcher;
import org.robotframework.ide.eclipse.main.plugin.console.RedSessionProcessListener;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModelManager;
import org.robotframework.ide.eclipse.main.plugin.preferences.InstalledRobotEnvironments;
import org.robotframework.red.graphics.ColorsManager;
import org.robotframework.red.graphics.FontsManager;
import org.robotframework.red.graphics.ImagesManager;

public class RedPlugin extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "org.robotframework.ide.eclipse.main.plugin";

    private static RedPlugin plugin;

    public static RedPlugin getDefault() {
        return plugin;
    }

    public static RobotModelManager getModelManager() {
        return RobotModelManager.getInstance();
    }

    static ImageDescriptor getImageDescriptor(final String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    @Override
    public void start(final BundleContext context) {
        try {
            super.start(context);
            if (RedSystemProperties.shouldShowSessionConsole()) {
                RobotRuntimeEnvironment.addProcessListener(new RedSessionProcessListener());
            }
        } catch (final Exception e) {
            throw new IllegalStateException("Unable to start RED plugin", e);
        }
        plugin = this;
    }

    @Override
    public void stop(final BundleContext context) {
        try {
            super.stop(context);
        } catch (final Exception e) {
            throw new IllegalStateException("Unable to stop RED plugin", e);
        }
        plugin = null;
        ColorsManager.disposeColors();
        FontsManager.disposeFonts();
        ImagesManager.disposeImages();
        RobotModelManager.getInstance().dispose();
        RedFileWatcher.getInstance().closeWatchService();
    }

    public RedPreferences getPreferences() {
        return new RedPreferences(getPreferenceStore());
    }

    public RobotRuntimeEnvironment getActiveRobotInstallation() {
        return InstalledRobotEnvironments.getActiveRobotInstallation(new RedPreferences(getPreferenceStore()));
    }

    public RobotRuntimeEnvironment getRobotInstallation(final File file) {
        return InstalledRobotEnvironments.getRobotInstallation(new RedPreferences(getPreferenceStore()), file);
    }

    public List<RobotRuntimeEnvironment> getAllRuntimeEnvironments() {
        final List<RobotRuntimeEnvironment> allRobotInstallation = InstalledRobotEnvironments
                .getAllRobotInstallation(new RedPreferences(getPreferenceStore()));
        return allRobotInstallation == null ? new ArrayList<RobotRuntimeEnvironment>() : allRobotInstallation;
    }

    public static void logInfo(final String message) {
        getDefault().getLog().log(new Status(IStatus.INFO, PLUGIN_ID, message));
    }

    public static void logWarning(final String message) {
        getDefault().getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message));
    }

    public static void logWarning(final String message, final Throwable cause) {
        getDefault().getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message, cause));
    }

    public static void logError(final String message, final Throwable cause) {
        getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, cause));
    }
}
