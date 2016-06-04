package com.orcchg.chatclient.ui.authorization;

import com.orcchg.chatclient.data.viewobject.AuthFormVO;
import com.orcchg.chatclient.ui.base.MvpView;

public interface AuthMvpView extends MvpView {
    void showAuthForm(AuthFormVO viewObject);

    String getLogin();
    String getEmail();
    String getPassword();
}
