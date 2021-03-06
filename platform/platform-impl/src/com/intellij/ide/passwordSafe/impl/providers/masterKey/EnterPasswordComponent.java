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
package com.intellij.ide.passwordSafe.impl.providers.masterKey;

import com.intellij.ide.passwordSafe.HelpID;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * @author gregsh
 */
public class EnterPasswordComponent extends PasswordComponentBase {
  @NotNull
  private final Function<String, Boolean> myPasswordConsumer;

  public EnterPasswordComponent(@NotNull Function<String, Boolean> passwordConsumer) {
    super("Enter");

    myPasswordConsumer = passwordConsumer;

    myPromptLabel.setText("<html><br>Master password is required to unlock the password database.<br>" +
                          "The password database will be unlocked during this session<br>" +
                          "for all subsystems.</html>");
    UIUtil.setEnabled(myNewPasswordPanel, false, true);
    myNewPasswordPanel.setVisible(false);

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      myPasswordField.setText("pass");
    }
  }

  @Override
  public ValidationInfo doValidate() {
    return null;
  }

  @Override
  public ValidationInfo apply() {
    // enter password — only and only old key, so, we use EncryptionUtil.genPasswordKey
    String password = new String(myPasswordField.getPassword());
    if (!myPasswordConsumer.apply(password)) {
      return new ValidationInfo("Password is incorrect", myPasswordField);
    }
    return null;
  }

  @Override
  public String getHelpId() {
    return HelpID.ENTER_PASSWORD;
  }
}
