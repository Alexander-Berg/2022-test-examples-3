import logging
from schema.schema import Schema, Or, Optional, Regex


log = logging.getLogger(__name__)


values_target_samples = Schema({
    Optional(Regex(r"^graphite_selector\d+$")): str,
    "rps_selector": str,
    "rtime_selector": str,
    "errors_selector": str,
    Optional("id"): str,
    Optional("power_selector"): str,
    Optional("count_selector"): str,
    Optional(Regex(r"ext_.*_selector$")): str,  # ext_sensor1_selector,ext_sensor2_selector
    Optional("child_service"): str,
    Optional("group_name"): str,
    Optional("url"): str,
    Optional("url_response"): str,
    Optional("cpu_utilization_selector"): str,
    Optional("cpu_child_utilization_selector"): str
})


group_meta = Schema({
    "id": str,
    "group_name": str,
    Optional("child_service"): str,
    Optional("rps_selector"): str,
    Optional("rps_child_selector"): str,  # rps дочернего контура
})


values_defaults = Schema({
    "globals": {
        "id": str,
        "itype": str,
        "log_id": str
    },
    "graphite": {
        Optional("rps_template"): str,  # графики количество запросов
        Optional(Regex(r"^rtime_.*_template$")): str,  # графики таймингов
        Optional("rps_limit_template"): str,
        Optional("errors_template"): str,  # графики ошибок
        Optional("power_template"): str,  # график мощностей
        Optional("count_template"): str,  # график количества инстансов
        Optional(Regex(r"^ext_.*_template$")): str  # графики разных сенсоров
    },
    "kraken": {
        Optional("agg_days_to_calc"): int,
        Regex(r"^agg_type_.*$"): str,
        Regex(r"^analyzer_.*$"): int,
        "decrease_percent": float,  # на какой процент уменьшить rps на цели за итерацию
        "increase_percent": float,  # на какой процент увеличить rps на цели за итерацию
        "rps_max": int,  # максимальный RPS для всех машин
        Optional("rps_min"): int,  # depricated
        Optional("rps_safe"): int,  # depricated
        "monitoring_api": str,  # откуда принимать значения по живости сервисов
        "rps_min_per_host": int,  # минимальный RPS для всех машин
        "no_data_crit_time": int,  # через сколько дней считать отсутствие данных критикалом
        Optional("rps_precision"): int,  # максимальная разница между rps_limit - rps*target_percent для которой снапшот считать валидным
        Optional("rps_target_percent"): float,  # процент RPS на target машине используемого при валидации снапшота
        "rps_safe_per_host": int,  # минимальный RPS на один инстанс, который можно выполнить сброс
        Optional("rps_sample_percent"): float,  # процент RPS на sample машинах необходимого для валидации снапшота
        "sla_max_errors_rate": float,  # sla проверка по проценту ошибок
        Optional("sla_max_rtime_0_99"): Or(int, float),  # sla порог по 99 таймингу
        Optional(Regex(r"^sla_ext_.*$")): Or(int, float),  # sla проверка по sensor1, example: sla_ext_sensor1
        Optional("sla_max_rtime_0_95"): Or(int, float),
        "snapshots_to_read": int,  # количество снапшотов, на основе которых принимается решение
        "snapshots_required": int,  # количество снапшотов, необходимых для принятия решения анализатора
        "snapshots_to_write": int,  # Количество снапшотов, которые должны обновляться
        "time_from": str,
        "time_to": str,
        Optional("analyzer_decision_warn_time"): int,
        "snapshots_window_update_minute": int,
        "snapshots_max_age_minute": int,
        "capacity_time_from": str,
        "capacity_time_to": str,
        Optional("no_data_revert_time"): int
    },
    "solomon": {
        "rps_template": str,
        "rps_limit_template": str,
        "rtime_0_80_template": str,
        "rtime_0_90_template": str,
        "rtime_0_95_template": str,
        "rtime_0_99_template": str,
        "errors_template": str,
        Optional("alived_template"): str,
        Optional("count_template"): str,
        Optional("power_template"): str,
        Optional("cpu_utilization_template"): str,
        Optional("cpu_child_utilization_template"): str,
        Optional(Regex(r"^ext_.*_template$")): str  # графики разных сенсоров
    },
    "mproxy": {
        "id": Or(int, str),  # уникальный ID который указан в mproxy. По нему выставляется rps_limit
        "url": str,  # по какому URL получать информацию об rps на target
        Optional("rps_limit_selector"): str,  # depricated
        Optional("rps_selector"): str  # depricated
    },
    "total": values_target_samples,
    "samples": [values_target_samples],  # блоки машин, с которыми идёт сравнение. Тут указывается мощность и параметры для выборок
    "target": values_target_samples,  # машина цель. Так же указывается мощность CPU + другие параметры для выборок
    "service_containers": [group_meta]
})


# блок для указания схемы(нужно для тестов)
config_schema = values_defaults
