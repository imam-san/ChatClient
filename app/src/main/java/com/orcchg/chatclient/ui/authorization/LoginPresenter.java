package com.orcchg.chatclient.ui.authorization;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.orcchg.chatclient.data.ApiStatusFactory;
import com.orcchg.chatclient.data.DataManager;
import com.orcchg.chatclient.data.Mapper;
import com.orcchg.chatclient.data.model.LoginForm;
import com.orcchg.chatclient.data.model.Status;
import com.orcchg.chatclient.data.viewobject.AuthFormVO;
import com.orcchg.chatclient.data.viewobject.LoginFormMapper;
import com.orcchg.chatclient.ui.base.BasePresenter;
import com.orcchg.chatclient.ui.chat.ChatActivity;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class LoginPresenter extends BasePresenter<LoginMvpView> {

    DataManager mDataManager;  // TODO: inject
    private Subscription mSubscriptionGet;
    private Subscription mSubscriptionSend;

    LoginPresenter(DataManager dataManager) {
        mDataManager = dataManager;
    }

    boolean hasRequestedLoginForm() {
        return mSubscriptionGet != null && !mSubscriptionGet.isUnsubscribed();
    }

    void unsubscribe() {
        mSubscriptionGet.unsubscribe();
        mSubscriptionSend.unsubscribe();
    }

    /* Login */
    // --------------------------------------------------------------------------------------------
    void requestLoginForm() {
        final Mapper<LoginForm, AuthFormVO> mapper = new LoginFormMapper();

        mSubscriptionGet = mDataManager.getLoginForm()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap(new Func1<LoginForm, Observable<AuthFormVO>>() {
                @Override
                public Observable<AuthFormVO> call(LoginForm loginForm) {
                    AuthFormVO viewObject = mapper.map(loginForm);
                    return Observable.just(viewObject);
                }
            }).subscribe(processAuthForm());
    }

    void sendLoginForm() {
        String login = getMvpView().getLogin();
        String password = getMvpView().getPassword();
        LoginForm form = new LoginForm(login, password);

        mSubscriptionSend = mDataManager.sendLoginForm(form)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(processStatus());
    }

    // --------------------------------------------------------------------------------------------
    private Observer<AuthFormVO> processAuthForm() {
        return new Observer<AuthFormVO>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Timber.e("Error: %s", Log.getStackTraceString(e));
            }

            @Override
            public void onNext(AuthFormVO viewObject) {
                getMvpView().showAuthForm(viewObject);
            }
        };
    }

    // --------------------------------------------------------------------------------------------
    private Observer<Status> processStatus() {
        return new Observer<Status>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Status status) {
                @ApiStatusFactory.Status int code = ApiStatusFactory.getStatusByCode(status.getCode());
                switch (code) {
                    case ApiStatusFactory.STATUS_SUCCESS:
                        Timber.i("Successfully logged in");
                        String userName = getMvpView().getLogin();
                        Activity activity1 = (Activity) getMvpView();
                        Intent intent1 = new Intent(activity1, ChatActivity.class);
                        intent1.putExtra(ChatActivity.EXTRA_USER_ID, status.getId());
                        intent1.putExtra(ChatActivity.EXTRA_USER_NAME, userName);
                        activity1.startActivity(intent1);
                        break;
                    case ApiStatusFactory.STATUS_WRONG_PASSWORD:
                        Timber.d("Wrong password");
                        getMvpView().onWrongPassword();
                        break;
                    case ApiStatusFactory.STATUS_NOT_REGISTERED:
                        Timber.d("Not registered");
                        Activity activity2 = (Activity) getMvpView();
                        Intent intent2 = new Intent(activity2, RegistrationActivity.class);
                        activity2.startActivity(intent2);
                        break;
                    case ApiStatusFactory.STATUS_ALREADY_REGISTERED:
                        Timber.e("Server's responded with forbidden error: already registered");
                        break;
                    case ApiStatusFactory.STATUS_ALREADY_LOGGED_IN:
                        getMvpView().onAlreadyLoggedIn();
                        break;
                    case ApiStatusFactory.STATUS_INVALID_FORM:
                        String message = "Client's requested with invalid form";
                        Timber.e(message);
                        throw new RuntimeException(message);
                    case ApiStatusFactory.STATUS_INVALID_QUERY:
                        Timber.e("Server's responded with forbidden error: invalid query");
                        break;
                    case ApiStatusFactory.STATUS_UNAUTHORIZED:
                        Timber.e("Server's responded with forbidden error: unauthorized");
                        break;
                    case ApiStatusFactory.STATUS_UNKNOWN:
                    default:
                        break;
                }
            }
        };
    }
}