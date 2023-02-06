#!/usr/bin/env python
# -*- coding: utf-8 -*-

from os.path import join


LUIGI_SCHEDULER_URL = 'https://profile-luigi-test.crypta.yandex.net/'
REACTOR_URL = 'https://test.reactor.yandex-team.ru'
CRYPTA_YT_WORKING_DIR = '//home/crypta/testing'
YT_ACCOUNT = 'crypta-testing'

# production thresholds are too strict for testing
SOCDEM_LABELING_ABSOLUTE_THRESHOLDS = {
    'strong': {
        'age': 2.0,
        'gender': 2.0,
        'income': 0.95,
    },
    'weak': {
        'age': 1.7,
        'gender': 1.7,
        'income': 0.95,
    },
}
RELATIVE_THRESHOLD = 0.85

CRYPTA_PUBLIC_IDS_STORAGE_DIR = '//home/crypta/public/ids_storage'
YUID_WITH_ALL_BY_YANDEXUID_TABLE = join(CRYPTA_YT_WORKING_DIR, 'profiles/sample/yuid_with_all_light_by_yandexuid')
VERTICES_NO_MULTI_PROFILE = join(CRYPTA_YT_WORKING_DIR, 'profiles/matching/vertices_no_multi_profile')
VERTICES_NO_MULTI_PROFILE_BY_CRYPTA_ID = join(CRYPTA_YT_WORKING_DIR, 'profiles/matching/vertices_no_multi_profile_by_crypta_id')
VERTICES_NO_MULTI_PROFILE_BY_ID_TYPE = join(CRYPTA_YT_WORKING_DIR, 'profiles/matching/vertices_no_multi_profile_by_id_type')
INDEVICE_YANDEXUID_BY_ID_TYPE = join(CRYPTA_YT_WORKING_DIR, 'profiles/matching/indevice_yandexuid_by_id_type_and_id')
INDEVICE_YANDEXUID = join(CRYPTA_YT_WORKING_DIR, 'profiles/matching/indevice_yandexuid')

LOGBROKER_OFFLINE_CRYPTA_IDENT = 'offline-crypta@test'
LOGBROKER_PROFILES_JSON_LOG_PARTITIONS_NUMBER = 10
LOGBROKER_TESTING_SAMPLING_RATE = 0.2

DEFAULT_POOL = 'crypta_profile_default'
SEGMENTS_POOL = 'crypta_profile_default'
TRAINABLE_SEGMENTS_POOL = 'crypta_profile_default'
MONITORING_POOL = 'crypta_profile_default'
EXPORT_PROFILES_POOL = 'crypta_profile_default'
LOG_PARSER_POOL = 'crypta_profile_default'
INTERESTS_POOL = 'crypta_profile_default'

N_DAYS_TO_AGGREGATE_APP_DATA = 3

SOLOMON_CLUSTER = 'profile_test'

CRYPTA_PROFILE_TVM_ID = 2021516

CRYPTA_PROFILES_TOPIC_NAME = '/offline-crypta/test/crypta-profiles-json-log'

CRYPTA_PROFILE_JUGGLER_HOST = 'crypta.profile.testing'
CRYPTA_ML_JUGGLER_HOST = 'crypta.ml.testing'

LOG_PARSING_SAMPLING = 'TABLESAMPLE SYSTEM(1.0)'
NUMBER_OF_DAYS_TO_CALCULATE_RULES = 3

INTERESTS_PROCESSED_FOLDER_TTL_DAYS = 3

NUMBER_OF_DAYS_TO_CALCULATE_LONGTERM_INTERESTS = 3
MIN_INTEREST_DURATION_IN_DAYS = 1
