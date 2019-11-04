//package com.cxb.myfamilytree.presenter;
//
//import android.content.res.Resources;
//import androidx.annotation.NonNull;
//
//import com.cxb.myfamilytree.model.IThemeModel;
//import com.cxb.myfamilytree.model.ThemeBean;
//import com.cxb.myfamilytree.model.ThemeModel;
//import com.cxb.myfamilytree.view.IThemeListView;
//
//import java.util.List;
//
//import io.reactivex.android.schedulers.AndroidSchedulers;
//import io.reactivex.disposables.CompositeDisposable;
//import io.reactivex.functions.Action;
//import io.reactivex.functions.Consumer;
//import io.reactivex.schedulers.Schedulers;
//
///**
// * 主题Presenter实现
// */
//
//public class ThemeListPresenter implements IBasePresenter<IThemeListView> {
//
//    private IThemeModel mModel;
//    private IThemeListView mView;
//
//    private CompositeDisposable mDisposable;
//
//    public ThemeListPresenter() {
//        mDisposable = new CompositeDisposable();
//        mModel = new ThemeModel();
//    }
//
//    @Override
//    public void attachView(IThemeListView view) {
//        mView = view;
//    }
//
//    @Override
//    public void detachView() {
//        if (mDisposable != null && !mDisposable.isDisposed()) {
//            mDisposable.dispose();
//        }
//        mView = null;
//    }
//
//    public void getThemeList(@NonNull Resources resources) {
//        mDisposable.add(
//                mModel.getThemeList(resources)
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(new Consumer<List<ThemeBean>>() {
//                            @Override
//                            public void accept(List<ThemeBean> themeList) throws Exception {
//                                if (mView != null) mView.showThemeList(themeList);
//                            }
//                        }, new Consumer<Throwable>() {
//                            @Override
//                            public void accept(Throwable throwable) throws Exception {
//
//                            }
//                        }));
//    }
//
//    public void saveTheme(@NonNull String theme) {
//        mDisposable.add(
//                mModel.saveTheme(theme)
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .doOnComplete(new Action() {
//                            @Override
//                            public void run() throws Exception {
//                                if (mView != null) mView.recreateActivity();
//                            }
//                        })
//                        .subscribe());
//    }
//}
