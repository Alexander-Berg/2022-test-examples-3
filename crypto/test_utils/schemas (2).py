adv_schema = [
    {'name': 'yandexuid', 'type': 'uint64'},
    {'name': 'cost', 'type': 'uint64'},
    {'name': 'os', 'type': 'string'},
]

antifraud_daily_schema = [
    {'name': 'uid', 'type': 'string'},
    {'name': 'uid_type', 'type': 'string'},
]

bm_categories_description_schema = [
    {'name': 'YTHash', 'type': 'uint64'},
    {'name': 'BMCategoryID', 'type': 'int64'},
    {'name': 'ParentBMCategoryID', 'type': 'int64'},
    {'name': 'Description', 'type': 'string'},
    {'name': 'Options', 'type': 'string'},
    {'name': 'Shows', 'type': 'int64'},
    {'name': 'UpdateTime', 'type': 'int64'},
    {'name': 'AncestorBMCategoryID', 'type': 'int64'},
    {'name': 'HitsRatio', 'type': 'double'},
]

bs_chevent_log_schema = [
    {'name': 'detaileddevicetype', 'type': 'string'},
    {'name': 'uniqid', 'type': 'uint64'},
    {'name': 'logid', 'type': 'uint64'},
    {'name': 'eventcost', 'type': 'int64'},
    {'name': 'placeid', 'type': 'int64'},
    {'name': 'fraudbits', 'type': 'uint64'},
]

cluster_stats_schema = [
    {'name': 'cluster', 'type': 'string'},
    {'name': 'serp_revenue_sum', 'type': 'double'},
    {'name': 'active_search_users', 'type': 'uint64'},
    {'name': 'CPT_sum', 'type': 'double'},
]

clusters_schema = [
    {'name': 'prism_segment', 'type': 'string'},
    {'name': 'crypta_id', 'type': 'string'},
    {'name': 'weight', 'type': 'double'},
    {'name': 'yandexuid', 'type': 'string'},
    {'name': 'icookie', 'type': 'string'},
    {'name': 'cluster', 'type': 'int64'},
    {'name': 'rank_in_cluster', 'type': 'double'},
]

feature_mapping_schema = [
    {'name': 'feature_index', 'type': 'uint64'},
    {'name': 'description', 'type': 'string'},
    {'name': 'feature', 'type': 'string'},
]

gmv_schema = [
    {'name': 'yandexuid', 'type': 'uint64'},
    {'name': 'cost', 'type': 'double'},
    {'name': 'os', 'type': 'string'},
]

hit_log_schema = [
    {'name': 'UniqID', 'type': 'uint64'},
    {'name': 'TimeStamp', 'type': 'uint32'},
    {'name': 'ProfileDump', 'type': 'string'},
]

indevice_yandexuid_matching = [
    {'name': 'id', 'type': 'string'},
    {'name': 'id_type', 'type': 'string'},
    {'name': 'yandexuid', 'type': 'uint64'},
]

longterm_norm_segment_weights_schema = [
    {'name': 'longterm_segment_2month', 'type': 'double'},
    {'name': 'longterm_segment_6month', 'type': 'double'},
    {'name': 'longterm_segment_month', 'type': 'double'},
    {'name': 'longterm_segment_week', 'type': 'double'},
    {'name': 'longterm_segment_week_m1', 'type': 'double'},
    {'name': 'norm_serp_revenue', 'type': 'double'},
    {'name': 'prism_segment', 'type': 'string'},
]

lookalike_schema = [
    {'name': 'GroupID', 'type': 'string'},
    {'name': 'Yandexuid', 'type': 'string'},
    {'name': 'Score', 'type': 'double'},
]

market_sales_schema = [
    {'name': 'hid', 'type': 'int64'},
    {'name': 'price', 'type': 'double'},
    {'name': 'yandexuid', 'type': 'utf8'},
    {'name': 'os_family', 'type': 'utf8'},
    {'name': 'multiplier', 'type': 'double'},
]

market_takerate_schema = [
    {'name': 'takerate', 'type': 'double'},
    {'name': 'hyper_id', 'type': 'int64'},
]

matching_schema = [
    {'name': 'id', 'type': 'string'},
    {'name': 'target_id', 'type': 'string'},
]

os_description_schema = [
    {'name': 'DetailedDeviceType', 'type': 'int64'},
    {'name': 'Description', 'type': 'string'},
]

prior_key_schema = [
    {'name': 'browser', 'type': 'string'},
    {'name': 'device_name', 'type': 'string'},
    {'name': 'device_vendor', 'type': 'string'},
    {'name': 'os_family', 'type': 'string'},
    {'name': 'screen_info', 'type': 'string'},
]

priors_schema = prior_key_schema + [
    {'name': 'count', 'type': 'uint64'},
    {'name': 'prior_cluster', 'type': 'int32'},
    {'name': 'prior_segment', 'type': 'int32'},
    {'name': 'prior_weight', 'type': 'double'},
    {'name': 'region', 'type': 'int32'},
]

prior_features_and_cluster_by_user_schema = prior_key_schema + [
    {'name': 'cluster', 'type': 'string'},
    {'name': 'device_type', 'type': 'string'},
    {'name': 'icookie', 'type': 'string'},
    {'name': 'region', 'type': 'int64'},
    {'name': 'date', 'type': 'string'},
]

prism_cluster_mapping_schema = [
    {'name': 'cluster', 'type': 'string'},
    {'name': 'norm_serp_revenue', 'type': 'double'},
    {'name': 'prism_segment', 'type': 'string'},
]

prism_segment_weights_schema = [
    {'name': 'cluster', 'type': 'string'},
    {'name': 'norm_serp_revenue', 'type': 'double'},
    {'name': 'prism_segment', 'type': 'string'},
]

roc_auc_schema = [
    {'name': 'fielddate', 'type': 'string'},
    {'name': 'roc_auc', 'type': 'double'},
]

statbox_event_money_schema = [
    {'name': 'revenue', 'type': 'double'},
    {'name': 'icookie', 'type': 'uint64'},
    {'name': 'service', 'type': 'string'},
    {'name': 'abc_id', 'type': 'int64'},
    {'name': 'shows', 'type': 'int64'},
    {'name': 'clicks', 'type': 'int64'},
    {'name': 'fraudbits', 'type': 'uint64'},
    {'name': 'placeid', 'type': 'int64'},
]

taxi_user_profile_schema = [
    {'name': 'tariff_class_stat', 'type': 'any'},
    {'name': 'order_cnt', 'type': 'int64'},
    {'name': 'passport_uid', 'type': 'string'},
]

traffic_v3_schema = [
    {'name': 'user_id', 'type': 'string'},
    {'name': 'search_engine_id', 'type': 'int64'},
    {'name': 'geo_id', 'type': 'uint32'},
    {'name': 'os_family', 'type': 'string'},
]

tx_lavka_schema = [
    {'name': 'CryptaId', 'type': 'string'},
    {'name': 'Status', 'type': 'string'},
    {'name': 'Timestamp', 'type': 'uint32'},
]

user_sessions_nano_schema = [
    {'name': 'key', 'type': 'string'},
]

user_sessions_full_schema = [
    {'name': 'key', 'type': 'string'},
    {'name': 'UserAgentRaw', 'type': 'string'},
    {'name': 'UserRegion', 'type': 'int64'},
    {'name': 'Ip', 'type': 'string'},
    {'name': 'Rearr', 'type_v3': {'type_name': 'struct', 'members': [
        {'name': 'SzmScreen', 'type': 'string'},
        {'name': 'SzmRatio', 'type': 'double'},
    ]}},
]

user_weights_schema = [
    {'name': 'cluster', 'type': 'string'},
    {'name': 'crypta_id', 'type': 'string'},
    {'name': 'longterm_metrics', 'type': 'any'},
    {'name': 'prism_segment', 'type': 'string'},
    {'name': 'yandexuid', 'type': 'string'},
    {'name': 'rank_in_cluster', 'type': 'double'},
]

yandex_google_visits_schema = [
    {'name': 'yandexuid', 'type': 'uint64'},
    {'name': 'yandex_visits', 'type': 'uint64'},
    {'name': 'google_visits', 'type': 'uint64'},
    {'name': 'os', 'type': 'string'},
]

yandexuid_profile_export_schema = [
    {'name': 'yandexuid', 'type': 'uint64'},
    {'name': 'icookie', 'type': 'uint64'},
    {'name': 'update_time', 'type': 'uint64'},
    {'name': 'income_5_segments', 'type': 'any'},
]

yandexuid_export_profiles_14_days_schema = [
    {'name': 'yandexuid', 'type': 'uint64'},
    {'name': 'audience_segments', 'type': 'any'},
]

yuid_with_all_info_schema = [
    {'name': 'id', 'type': 'string'},
    {'name': 'dates', 'type_v3': {'type_name': 'list', 'item': 'string'}},
]


def get_features_and_cluster_by_user_schema(with_date=True):
    schema = prior_key_schema
    schema.extend([
        {'name': 'cluster', 'type': 'string'},
        {'name': 'device_type', 'type': 'string'},
        {'name': 'icookie', 'type': 'string'},
        {'name': 'region', 'type': 'int64'},
    ])
    if with_date:
        schema.append({'name': 'date', 'type': 'string'})
    return schema
