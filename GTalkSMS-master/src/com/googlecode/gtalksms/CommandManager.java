package com.googlecode.gtalksms;

import com.googlecode.gtalksms.cmd.AliasCmd;
import com.googlecode.gtalksms.cmd.ApplicationsCmd;
import com.googlecode.gtalksms.cmd.BatteryCmd;
import com.googlecode.gtalksms.cmd.BluetoothCmd;
import com.googlecode.gtalksms.cmd.CallCmd;
import com.googlecode.gtalksms.cmd.CameraCmd;
import com.googlecode.gtalksms.cmd.ClipboardCmd;
import com.googlecode.gtalksms.cmd.Cmd;
import com.googlecode.gtalksms.cmd.CommandActivationCmd;
import com.googlecode.gtalksms.cmd.CommandHandlerBase;
import com.googlecode.gtalksms.cmd.ContactCmd;
import com.googlecode.gtalksms.cmd.ExitCmd;
import com.googlecode.gtalksms.cmd.FileCmd;
import com.googlecode.gtalksms.cmd.GeoCmd;
import com.googlecode.gtalksms.cmd.HelpCmd;
import com.googlecode.gtalksms.cmd.KeyboardCmd;
import com.googlecode.gtalksms.cmd.LogsCmd;
import com.googlecode.gtalksms.cmd.MmsCmd;
import com.googlecode.gtalksms.cmd.MusicCmd;
import com.googlecode.gtalksms.cmd.NotificationsCmd;
import com.googlecode.gtalksms.cmd.RebootCmd;
import com.googlecode.gtalksms.cmd.RecipientCmd;
import com.googlecode.gtalksms.cmd.RingCmd;
import com.googlecode.gtalksms.cmd.SettingsCmd;
import com.googlecode.gtalksms.cmd.ShellCmd;
import com.googlecode.gtalksms.cmd.SmsCmd;
import com.googlecode.gtalksms.cmd.SystemCmd;
import com.googlecode.gtalksms.cmd.TextToSpeechCmd;
import com.googlecode.gtalksms.cmd.ToastCmd;
import com.googlecode.gtalksms.cmd.UrlsCmd;
import com.googlecode.gtalksms.cmd.VideoCmd;
import com.googlecode.gtalksms.cmd.WifiCmd;
import com.googlecode.gtalksms.tools.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Florent on 21/12/13.
 *
 * Manage and instantiate all the commands
 */
public class CommandManager {
    private static final Class<?>[] sCommands = new Class[] {
        VideoCmd.class,
        NotificationsCmd.class,
        ApplicationsCmd.class,
        LogsCmd.class,
        TextToSpeechCmd.class,
        ToastCmd.class,
        ClipboardCmd.class,
        CameraCmd.class,
        KeyboardCmd.class,
        BatteryCmd.class,
        GeoCmd.class,
        CallCmd.class,
        ContactCmd.class,
        ShellCmd.class,
        UrlsCmd.class,
        RingCmd.class,
        FileCmd.class,
        MusicCmd.class,
        MmsCmd.class,
        SmsCmd.class,
        ExitCmd.class,
        AliasCmd.class,
        SettingsCmd.class,
        BluetoothCmd.class,
        WifiCmd.class,
        RebootCmd.class,
        RecipientCmd.class,
        // used for debugging
        SystemCmd.class,
        // help & command activation commands need to be registered as last
        CommandActivationCmd.class,
        HelpCmd.class,
    };

    private final Set<CommandHandlerBase> mAvailableCommandSet = new HashSet<CommandHandlerBase>();
    private final Map<String, CommandHandlerBase> mActiveCommands = Collections.synchronizedMap(new HashMap<String, CommandHandlerBase>());
    private final Set<CommandHandlerBase> mActiveCommandSet = Collections.synchronizedSet(new HashSet<CommandHandlerBase>());

    public Set<CommandHandlerBase> getAvailableCommandSet() {
        return mAvailableCommandSet;
    }

    public Map<String, CommandHandlerBase> getActiveCommands() {
        return mActiveCommands;
    }

    public Set<CommandHandlerBase> getActiveCommandSet() {
        return mActiveCommandSet;
    }

    public void updateAndReturnStatus() {
        for (CommandHandlerBase c : mAvailableCommandSet) {
            c.updateAndReturnStatus();
        }
    }

    public CommandHandlerBase getActiveCommand(String cmd) {
        if (mActiveCommands.containsKey(cmd)) {
            return mActiveCommands.get(cmd);
        }
        return null;
    }

    /**
     * Instantiate all the commands via reflection
     */
    public void setupCommands(MainService mainService) {
        for (Class<?> commandClass : sCommands) {
            try {
                CommandHandlerBase cmd = (CommandHandlerBase) commandClass.getConstructor(MainService.class).newInstance(mainService);
                mAvailableCommandSet.add(cmd);

                if (cmd.updateAndReturnStatus()) {
                    for (Cmd subCmd : cmd.getCommands()) {
                        mActiveCommands.put(subCmd.getName().toLowerCase(), cmd);
                        if (subCmd.getAlias() != null) {
                            for (String a : subCmd.getAlias()) {
                                mActiveCommands.put(a.toLowerCase(), cmd);
                            }
                        }
                    }
                    mActiveCommandSet.add(cmd);
                }
            } catch (Exception e) {
                // Should not happen.
                Log.e("Failed to register command " + commandClass.getName(), e);
            }
        }
    }

    /**
     * Deactivate all commands
     */
    public void cleanupCommands() {
        // Make a copy of the activeCommandSet as deactivate() may remove a command from mActiveCommandSet
        Set<CommandHandlerBase> currentActiveCommandSet = new HashSet<CommandHandlerBase>(mActiveCommandSet);
        for (CommandHandlerBase cmd : currentActiveCommandSet) {
            try {
                cmd.deactivate();
            } catch (Exception e) {
                Log.e("Failed to cleanup command", e);
            }
        }
    }

    /**
     * Stop all the commands. Used for pending actions like ringing
     */
    public void stopCommands() {
        for (CommandHandlerBase c : mAvailableCommandSet) {
            c.stop();
        }
    }

    /**
     * Update and refresh the activation status of the command
     */
    public void updateCommandState() {
        for (CommandHandlerBase c : mAvailableCommandSet) {
            c.updateAndReturnStatus();
        }
    }
}
