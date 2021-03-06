/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.formatting.commandLine;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarterEx;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.SchemeImportException;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.impl.source.codeStyle.CodeStyleSettingsLoader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * A launcher class for command-line formatter.
 */
public class FormatterStarter extends ApplicationStarterEx {

  public static final String FORMAT_COMMAND_NAME = "format";
  private static final Logger LOG = Logger.getInstance("#" + FormatterStarter.class.getName());

  @Override
  public boolean isHeadless() {
    return true;
  }

  @Override
  public String getCommandName() {
    return FORMAT_COMMAND_NAME;
  }

  @Override
  public void premain(String[] args) {
  }

  @Override
  public void main(String[] args) {
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    MessageOutput messageOutput = new MessageOutput(
      new PrintWriter(System.out),
      new PrintWriter(System.err));
    messageOutput.info(getAppInfo() + " Formatter\n");
    CodeStyleSettings settings = null;
    logArgs(args);
    if (args.length < 2) {
      showUsageInfo(messageOutput);
    }
    for (int i = 1; i < args.length; i ++) {
      if (args[i].startsWith("-")) {
        if (checkOption(args[i], "-h", "--help")) {
          showUsageInfo(messageOutput);
        }
        if (checkOption(args[i], "-s", "--settings")) {
          //noinspection AssignmentToForLoopParameter
          i ++;
          if (i >= args.length) {
            fatalError(messageOutput, "Mising settings file path.");
          }
          try {
            settings = readSettings(args[i]);
          }
          catch (SchemeImportException e) {
            fatalError(messageOutput, e.getLocalizedMessage() + "\n");
          }
        }
        else {
          fatalError(messageOutput, "Unknown option " + args[i]);
        }
      }
      else {
        FileSetFormatter fileSetFormatter = new FileSetFormatter(args[i], settings, messageOutput);
        try {
          fileSetFormatter.processFiles();
          messageOutput.info("\n" + fileSetFormatter.getProcessedFiles() + " files formatted.\n");
        }
        catch (IOException e) {
          fatalError(messageOutput, e.getLocalizedMessage());
        }
      }
    }
    ((ApplicationEx)ApplicationManager.getApplication()).exit(true, true);
  }

  private static void fatalError(@NotNull MessageOutput messageOutput, @NotNull String message) {
    messageOutput.error("ERROR: " + message + "\n");
    System.exit(1);
  }

  private static CodeStyleSettings readSettings(@NotNull String settingsPath) throws SchemeImportException {
    VirtualFile vFile = VfsUtil.findFileByIoFile(new File(settingsPath), true);
    CodeStyleSettingsLoader loader = new CodeStyleSettingsLoader();
    if (vFile == null) {
      throw new SchemeImportException("Cannot find file " + settingsPath);
    }
    return loader.loadSettings(vFile);
  }

  private static boolean checkOption(@NotNull String arg, String... variants) {
    for (String variant: variants) {
      if (variant.equals(arg)) return true;
    }
    return false;
  }

  private static String getAppInfo() {
    ApplicationInfoImpl appInfo = (ApplicationInfoImpl)ApplicationInfoEx.getInstanceEx();
    return String.format("%s, build %s", appInfo.getFullApplicationName(), appInfo.getBuild().asString());
  }


  private static void showUsageInfo(@NotNull MessageOutput messageOutput) {
    messageOutput.info("Usage: format [-s|--settings settingsPath] fileSpec...\n");
    messageOutput.info("  -s|--settings  A path to Intellij IDEA code style settings .xml file.\n");
    messageOutput.info("  fileSpec       A file specification, may contain wildcards.\n");
  }

  private static void logArgs(@NotNull String[] args) {
    StringBuilder sb = new StringBuilder();
    for (String arg : args) {
      if (sb.length() > 0) sb.append(",");
      sb.append(arg);
    }
    LOG.info("Arguments: " + sb);
  }
}
