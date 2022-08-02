package ua.cn.stu.navigation.contract

interface ConstantManager {
    companion object {
        var READ_REGISTER = byteArrayOf(0x00)
        var FAKE_DATA_REGISTER = byteArrayOf(0x13.toByte(), 0x87.toByte())

        var DATE_REGISTER = byteArrayOf(0x10, 0x00)
        var TIME_WORK_PUMP_REGISTER = byteArrayOf(0x10, 0x01)


        var NUM_BASAL_PROFILES_REGISTER = byteArrayOf(0x16, 0x00)
        var NUM_MODIFIED_BASAL_PROFILES_REGISTER = byteArrayOf(0x16, 0x01)
        var NUM_ACTIVE_BASAL_PROFILES_REGISTER = byteArrayOf(0x16, 0x02)
        var NUM_PERIODS_MODIFIED_BASAL_PROFILE_REGISTER = byteArrayOf(0x16, 0x03)
        var NUM_MODIFIED_PERIOD_MODIFIED_BASAL_PROFILE_REGISTER = byteArrayOf(0x16, 0x04)
        var PERIOD_BASAL_PROFILE_DATA_REGISTER = byteArrayOf(0x16, 0x06)
        var NAME_BASAL_PROFILE_REGISTER = byteArrayOf(0x16, 0x07)
        var ACTIVATE_BASAL_PROFILE_REGISTER = byteArrayOf(0x16, 0x08)
        var DELETE_BASAL_PROFILE_REGISTER = byteArrayOf(0x16, 0x09)
        var BASAL_LOCK_CONTROL_REGISTER = byteArrayOf(0x16, 0xff.toByte())

        var BASAL_SPEED_REGISTER = byteArrayOf(0x16, 0x0a)

        var BASAL_TEMPORARY_PERFORMANCE_REGISTER = byteArrayOf(0x17, 0x00)
        var BASAL_TEMPORARY_TYPE_ADJUSTMENT_REGISTER = byteArrayOf(0x17, 0x01)
        var BASAL_TEMPORARY_VALUE_ADJUSTMENT_REGISTER = byteArrayOf(0x17, 0x02)
        var BASAL_TEMPORARY_TIME_REGISTER = byteArrayOf(0x17, 0x03)

        var INIT_REFUELLING_REGISTER = byteArrayOf(0x19, 0x00)

        var SUPPLIES_RSOURCE_REGISTER = byteArrayOf(0x1d, 0x00)
        var IOB_REGISTER = byteArrayOf(0x1c, 0x00)
        var BATTERY_PERCENT_REGISTER = byteArrayOf(0x1c, 0x02)
        var AKB_PERCENT_REGISTER = byteArrayOf(0x1c, 0x03)
        var BALANCE_DRUG_REGISTER = byteArrayOf(0x1c, 0x04)

        var BOLUS_ACTIVATE_REGISTER = byteArrayOf(0x1e, 0x00)
        var BOLUS_TYPE_REGISTER = byteArrayOf(0x1e, 0x01)
        var BOLUS_AMOUNT_REGISTER = byteArrayOf(0x1e, 0x02)
//        var BOLUS_AMOUNT_REGISTER = byteArrayOf(0x1e, 0x03)
        var SUPER_BOLUS_RESTRICTION_FLAG_REGISTER = byteArrayOf(0x1e, 0x05)
        var EXTENEDED_AND_DUAL_PATTERN_BOLUS_RESTRICTION_FLAG_REGISTER = byteArrayOf(0x1e, 0x06)
        var SIMPLE_BOLUS_RESTRICTION_FLAG_REGISTER = byteArrayOf(0x1e, 0x08)
        var SUPER_BOLUS_TIME_REGISTER = byteArrayOf(0x1e, 0x09)
        var SUPER_BOLUS_BASL_VOLIUM_REGISTER = byteArrayOf(0x1e, 0x0a)
        var EXTENDED_AND_DUAL_PATTERN_BOLUS_TIME_REGISTER = byteArrayOf(0x1e, 0x0b)
        var BOLUS_STRECHED_AMOUNT_REGISTER = byteArrayOf(0x1e, 0x0c)
        var BOLUS_DELETE_REGISTER = byteArrayOf(0x1e, 0x0d)
        var BOLUS_DELETE_CONFIRM_REGISTER = byteArrayOf(0x1e, 0x0e)


        var DATE = "1000"
        var TIME_WORK_PUMP = "1001"

        var NUM_BASAL_PROFILES = "1600"
        var NUM_MODIFIED_BASAL_PROFILES = "1601"
        var NUM_ACTIVE_BASAL_PROFILE = "1602"
        var NUM_PERIODS_MODIFIED_BASAL_PROFILE = "1603"
        var NUM_MODIFIED_PERIOD_MODIFIED_BASAL_PROFILE = "1604"
        var PERIOD_BASAL_PROFILE_DATA = "1606"
        var NAME_BASAL_PROFILE = "1607"
        var ACTIVATE_BASAL_PROFILE = "1608"
        var DELETE_BASAL_PROFILE = "1609"
        var BASAL_LOCK_CONTROL = "16ff"

        var BASAL_SPEED = "160a"

        var BASAL_TEMPORARY_PERFORMANCE = "1700" // R|W 0-1 Включение или отключение корректировки профиля
        var BASAL_TEMPORARY_TYPE_ADJUSTMENT = "1701" // R|W 0-1 Тип корректировки: (1 - Ед/ч) (0 - %)
        var BASAL_TEMPORARY_VALUE_ADJUSTMENT = "1702" // R|W -32k-32k Значение в 0,01 Ед или %
        var BASAL_TEMPORARY_TIME = "1703" // R|W 0-65k Время действия в минутах

        var INIT_REFUELLING = "1900" // R|W 0-1 Начало процедуры заправки

        var SUPPLIES_RSOURCE = "1d00" // R|W 0-50.00 Ед Insulin On Board 2 Байта
        var IOB = "1c00" // R|W 0-50.00 Ед Insulin On Board 2 Байта
        var BATTERY_PERCENT = "1c02"
        var AKB_PERCENT = "1c03"
        var BALANCE_DRUG = "1c04"

        var BOLUS_ACTIVATE = "1e00" // R|W 0-1 Запуск ввода болюса
        var BOLUS_TYPE = "1e01" // R|W 0-15 Выбор типа болюсного ввода
        var BOLUS_AMOUNT = "1e02" // R|W 0-32к Количество Ед
        var SUPER_BOLUS_RESTRICTION_FLAG = "1e05"
        var EXTENEDED_AND_DUAL_PATTERN_BOLUS_RESTRICTION_FLAG = "1e06"
        var SIMPLE_BOLUS_RESTRICTION_FLAG = "1e08" // R|W 0-1 Флаг разрешения обычного болюса.
        var SUPER_BOLUS_TIME = "1e09"
        var SUPER_BOLUS_BASL_VOLIUM = "1e0a"
        var EXTENDED_BOLUS_TIME = "1e0b"
        var BOLUS_STRECHED_AMOUNT = "1e0c"
        var BOLUS_DELETE = "1e0d"
        var BOLUS_DELETE_CONFIRM = "1e0e"


        val LOG_UPDATE_DEPTH: Int get() = 7 //7 лог синхронизируется на 7 дней в глубину
        val RESERVOIR_VOLUME: Int get() = 300 //300 единиц
        val CANNULE_RESOURCE: Int get() = 4320  //72 часа в минутах
        val MAX_COUNT_PROFILES: Int get() = 7  //7 базальных профилей

        const val REQUEST_ENABLE_BT = 1
        const val MY_PERMISSIONS_REQUEST_LOCATION = 99
        var RECONNECT_BLE_PERIOD = 1000

    }
}