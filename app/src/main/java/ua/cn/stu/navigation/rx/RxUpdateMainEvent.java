package ua.cn.stu.navigation.rx;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import ua.cn.stu.navigation.connection.ScanItem;

public class RxUpdateMainEvent {

  private static RxUpdateMainEvent instance;
  private final PublishSubject<ScanItem> scanListSubject;
  private final PublishSubject<Boolean> refillingSubject;
  private final PublishSubject<Boolean> percentSinhronizationProfileSubject;
  private final PublishSubject<Boolean> selectBasalProfileSubject;
  private final PublishSubject<Boolean> backPresedIventSubject;
  private final PublishSubject<Boolean> updateChatSubject;


  private RxUpdateMainEvent() {
    scanListSubject = PublishSubject.create();
    refillingSubject = PublishSubject.create();
    percentSinhronizationProfileSubject = PublishSubject.create();
    selectBasalProfileSubject = PublishSubject.create();
    backPresedIventSubject = PublishSubject.create();
    updateChatSubject = PublishSubject.create();
  }

  public static RxUpdateMainEvent getInstance() {
    if (instance == null) {
      instance = new RxUpdateMainEvent();
    }
    return instance;
  }

  public void updateScanList(ScanItem scanItem) { scanListSubject.onNext(scanItem); }
  public void updateRefilling(Boolean variable) { refillingSubject.onNext(variable); }
  public void updatePercentSinhronizationProfile() { percentSinhronizationProfileSubject.onNext(true); }
  public void updateSelectBasalProfile() { selectBasalProfileSubject.onNext(true); }
  public void updateBackPresedIvent() { backPresedIventSubject.onNext(true); }
  public void updateChat() { updateChatSubject.onNext(true); }


  public Observable<ScanItem> getScanListObservable() { return scanListSubject; }
  public Observable<Boolean> getRefiliingObservable() { return refillingSubject; }
  public Observable<Boolean> getPercentSinhronizationProfileSubjectObservable() { return percentSinhronizationProfileSubject; }
  public Observable<Boolean> getSelectBasalProfileSubjectObservable() { return selectBasalProfileSubject; }
  public Observable<Boolean> getBackPresedIventSubjectObservable() { return backPresedIventSubject; }
  public Observable<Boolean> getUpdateChatSubjectObservable() { return updateChatSubject; }
}
