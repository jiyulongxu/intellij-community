/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.idea.svn.commandLine;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.util.WaitForProgressToShow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.svn.SvnBundle;
import org.jetbrains.idea.svn.dialogs.SimpleCredentialsDialog;
import org.tmatesoft.svn.core.SVNURL;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Konstantin Kolosovsky.
 */
public class TerminalSshModule extends LineCommandAdapter implements CommandRuntimeModule, InteractiveCommandListener {

  private static final Logger LOG = Logger.getInstance(TerminalSshModule.class);

  private static final Pattern PASSPHRASE_PROMPT = Pattern.compile("Enter passphrase for key \\'(.*)\\':\\s?");
  private static final Pattern PASSWORD_PROMPT = Pattern.compile("(.*)\\'s password:\\s?");

  @NotNull private final CommandRuntime myRuntime;
  @NotNull private final CommandExecutor myExecutor;

  // TODO: Do not accept executor here and make it as command runtime module
  public TerminalSshModule(@NotNull CommandRuntime runtime, @NotNull CommandExecutor executor) {
    myExecutor = executor;
    myRuntime = runtime;
  }

  @Override
  public void onStart(@NotNull Command command) throws SvnBindException {
  }

  @Override
  public boolean handlePrompt(String line, Key outputType) {
    return checkPassphrase(line) || checkPassword(line);
  }

  private boolean checkPassphrase(@NotNull String line) {
    Matcher matcher = PASSPHRASE_PROMPT.matcher(line);

    return matcher.matches() && handleAuthPrompt(SimpleCredentialsDialog.Mode.SSH_PASSPHRASE, matcher.group(1));
  }

  private boolean checkPassword(@NotNull String line) {
    Matcher matcher = PASSWORD_PROMPT.matcher(line);

    return matcher.matches() && handleAuthPrompt(SimpleCredentialsDialog.Mode.SSH_PASSWORD, matcher.group(1));
  }

  private boolean handleAuthPrompt(@NotNull final SimpleCredentialsDialog.Mode mode, @NotNull final String key) {
    @NotNull final SVNURL repositoryUrl = myExecutor.getCommand().getRepositoryUrl();
    final Project project = myRuntime.getVcs().getProject();
    final Ref<String> answer = new Ref<String>();

    Runnable command = new Runnable() {
      public void run() {
        SimpleCredentialsDialog dialog = new SimpleCredentialsDialog(project);
        dialog.setup(mode, repositoryUrl.toDecodedString(), key, true);
        dialog.setTitle(SvnBundle.message("dialog.title.authentication.required"));
        dialog.show();
        if (dialog.isOK()) {
          answer.set(dialog.getPassword());
        }
        // TODO: Correctly handle "cancel" - kill the process
        // TODO: and perform "cleanup" on working copy
      }
    };

    WaitForProgressToShow.runOrInvokeAndWaitAboveProgress(command);

    if (!answer.isNull()) {
      sendAnswer(answer.get());
    }

    return !answer.isNull();
  }

  private boolean sendAnswer(@NotNull String answer) {
    try {
      myExecutor.write(answer + "\n");
      return true;
    }
    catch (SvnBindException e) {
      // TODO: handle this more carefully
      LOG.info(e);
    }
    return false;
  }
}
