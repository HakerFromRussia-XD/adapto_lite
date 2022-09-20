package ua.cn.stu.navigation.rx;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import ua.cn.stu.navigation.connection.ScanItem;

public class RxUpdateMainEvent {

  private static RxUpdateMainEvent instance;
  private final PublishSubject<ScanItem> scanListSubject;
  private final PublishSubject<Boolean> backPresedIventSubject;
  private final PublishSubject<Boolean> updateDebugSubject;
  private final PublishSubject<Boolean> updateHomeSubject;


  private RxUpdateMainEvent() {
    scanListSubject = PublishSubject.create();
    backPresedIventSubject = PublishSubject.create();
    updateDebugSubject = PublishSubject.create();
    updateHomeSubject = PublishSubject.create();
  }

  public static RxUpdateMainEvent getInstance() {
    if (instance == null) {
      instance = new RxUpdateMainEvent();
    }
    return instance;
  }

  public void updateScanList(ScanItem scanItem) { scanListSubject.onNext(scanItem); }
  public void updateDebugFragment() { updateDebugSubject.onNext(true); }
  public void updateHomeFragment() { updateHomeSubject.onNext(true); }
  public void updateBackPresedIvent() { backPresedIventSubject.onNext(true); }


  public Observable<ScanItem> getScanListObservable() { return scanListSubject; }
  public Observable<Boolean> getDebugFragmentObservable() { return updateDebugSubject; }
  public Observable<Boolean> getHomeFragmentObservable() { return updateHomeSubject; }
}
