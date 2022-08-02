package ua.cn.stu.navigation.ble;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static final HashMap<String, String> attributes = new HashMap();
    // Sample Characteristics.
    public static String MIO_MEASUREMENT_NEW_TEST = "43686172-4d74-726b-0201-526f64696f6e";

    public static String NOTIFICATION_PUMP_STATUS = "c8b61521-a676-4fba-968b-cbccbdd224c9";
    public static String LOG_POINTER = "c8b61522-a676-4fba-968b-cbccbdd224c9";
    public static String NOTIFICATION_PUMP_LOG = "c8b61523-a676-4fba-968b-cbccbdd224c9";
    public static String REGISTER_POINTER = "c8b61501-a676-4fba-968b-cbccbdd224c9"; //передаём сюда номер регистра в который пишем или из которого читаем
    public static String REGISTER_DATA = "c8b61502-a676-4fba-968b-cbccbdd224c9"; //передаём сюда номер регистра в который пишем или из которого читаем
    public static String PASS_DATA = "c8b61531-a676-4fba-968b-cbccbdd224c9"; //пароль помпы для первоначального подключения

//    public static String BOLUS_CONTROL = "c8b6150b-a676-4fba-968b-cbccbdd224c9";//передаём сюда количество единиц болюса, который хотим вколоть
//    public static String PUMP_SEARCH = "c8b61501-a676-4fba-968b-cbccbdd224c9";// W 0-1 - включение режима поиска
//    public static String DIA = "c8b61503-a676-4fba-968b-cbccbdd224c9";// R|W 0-6000 время действия инсулина 2 Байта MSB
//    public static String CR = "c8b61505-a676-4fba-968b-cbccbdd224c9";// R|W 0-150 углеводный коэффициент 1 Байт
//    public static String ISF = "c8b61507-a676-4fba-968b-cbccbdd224c9";// R|W 0-300 фактор чувствительности к инсулину 2 Байта
//    public static String TARGET_GK = "c8b61509-a676-4fba-968b-cbccbdd224c9";// R|W 0-150 целевая гликемия 1 Байт
//    public static String IOB = "c8b6150d-a676-4fba-968b-cbccbdd224c9";// R|W 0-50.00 Ед Insulin On Board 2 Байта
//    public static String COB = "c8b6150f-a676-4fba-968b-cbccbdd224c9";// W 0-2000 г Carbs On Board 2 Байта
//    public static String BATTERY_CHARGE = "c8b61511-a676-4fba-968b-cbccbdd224c9";// R 0-100 заряд батареи 1 Байт
//    public static String ACCUMULATOR_CHARGE = "c8b61513-a676-4fba-968b-cbccbdd224c9";// R 0-100 заряд аккумулятора 1 Байт
//    public static String INSULIN_IN_PUMP = "c8b61515-a676-4fba-968b-cbccbdd224c9";// R 0-3000 кол-во оставшегося в резервуаре инсулина 2 Байта
//    public static String BOLUS_MODE = "c8b61517-a676-4fba-968b-cbccbdd224c9";// R|W 0-3 режим болюса 1 Байт 0-простой болюс 1-кв. волна 2-суперболюс 3-дв. волна 4-кв. дв. волна
//    public static String BOLUS_SPEED = "c8b61518-a676-4fba-968b-cbccbdd224c9";// R|W 0-3000 скорость болюса 2 Байта
//    public static String BOLUS_START_STOP = "c8b61519-a676-4fba-968b-cbccbdd224c9";// R|W 0-1 старт стоб болюса 1 Байт
//    public static String BASAL_SELECT_PROFILE = "c8b6151b-a676-4fba-968b-cbccbdd224c9";// R|W 0-5 ID текущего активного профиля 1 Байт
//    public static String BASAL_ON_OFF = "c8b6151c-a676-4fba-968b-cbccbdd224c9";// R|W 0-1 вкл/выкл использование профиля 1 Байт
//    public static String BASAL_ID_SETTINGS = "c8b6151d-a676-4fba-968b-cbccbdd224c9";// R|W 0-5 ID профиля для настройки/чтения параметров 1 Байт (неведомая хуйня)
//    public static String BASAL_TIME_INTERVAL = "c8b6151e-a676-4fba-968b-cbccbdd224c9";// R|W 0-23 временной интервал 1 Байт
//    public static String BASAL_SPEED_SELECTED_INTERVAL = "c8b6151f-a676-4fba-968b-cbccbdd224c9";// R|W 0-5000 cкорость данного временного интервала 2 Байта
//    public static String BASAL_NAME_SELECTED_INTERVAL = "c8b61520-a676-4fba-968b-cbccbdd224c9";// R|W STRING имя данного профиля 15 Байт (пустые заняты 0x00)
//    public static String BASAL_CURRENT_SPEED = "c8b61521-a676-4fba-968b-cbccbdd224c9";// R|W 0-5000 текущая базальная скорость 2 Байта
//    public static String BASAL_TEMPORARY_ON_OFF = "c8b61522-a676-4fba-968b-cbccbdd224c9";// R|W 0-1 вкл/выкл использование временного базала 1 Байт
//    public static String BASAL_TEMPORARY_SPEED = "c8b61523-a676-4fba-968b-cbccbdd224c9";// R|W 0-5000 cкорость ввода временного базала 2 Байта
//    public static String BASAL_TEMPORARY_TIME_ACTIVE = "c8b61524-a676-4fba-968b-cbccbdd224c9";// R|W 0-1440 время активности временного базала 2 Байта
//    public static String DIARY_DATE = "c8b61526-a676-4fba-968b-cbccbdd224c9";// R|W 1-31 1-12 20-99 дата записей 3 Байта
//    public static String DIARY_RECORD_COUNT = "c8b61527-a676-4fba-968b-cbccbdd224c9";// R 0-4 294 967 295  получение кол-ва записей в соответствие с выбранным типом или датой 4 Байта
//    public static String DIARY_RECORDS = "c8b61528-a676-4fba-968b-cbccbdd224c9";// R|W 0-65535  ID записи, с которой будет производиться работа 2 Байта
//    public static String DIARY_RECORD_TYPE = "c8b61529-a676-4fba-968b-cbccbdd224c9";// R|W 0-4  тип записи в дневнике(логе) 1 Байт  0-все записи 1-остановка/запуск помпы 2-изменение базала 3-изменение болюса 4-заправка
//    public static String DIARY_DATA = "c8b6152a-a676-4fba-968b-cbccbdd224c9";// R|W    массив целевых данных    До 20 Байт
    public static String SUSPEND_START_STOP = "c8b6152c-a676-4fba-968b-cbccbdd224c9";// R|W 0-1   ‘1’ - включение паузы   1 Байт
//    public static String BASAL_BLOCK_CHANGE = "c8b616ff-a676-4fba-968b-cbccbdd224c9";// R|W 0-1   ‘1’ - разрешение редактирования   1 Байт
//    public static String BASAL_PROFILE = "c8b61601-a676-4fba-968b-cbccbdd224c9";// R|W 0-255   номер базального профиля, с которым работаем   1 Байт
//    public static String BASAL_PROFILE_NUMBER = "c8b61603-a676-4fba-968b-cbccbdd224c9";// R|W 0-24   всего периодов в профиле   1 Байт
//    public static String BASAL_PERIOD_NUMBER = "c8b61604-a676-4fba-968b-cbccbdd224c9";// R|W 0-24   ‘0’ - создание нового периода профиля   1 Байт
//    public static String BASAL_PERIOD_SETTINGS = "c8b61606-a676-4fba-968b-cbccbdd224c9";// R|W   AAAAББББВВВВ   6 Байт

    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    // Sample Commands.
    public static Boolean SHOW_EVERYONE_RECEIVE_BYTE = false;
    public static String READ = "READ";
    public static String WRITE = "WRITE";
    public static String NOTIFY = "NOTIFY";


    static {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put("00001810-0000-1000-8000-00805f9b34fb", "Что-то моё");
        attributes.put("0000fe40-cc7a-482a-984a-7f2ed5b3e58f", "Наша кастомная характеристика");
        // Sample Characteristics.
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");

    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
