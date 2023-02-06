import json
from collections import defaultdict

from ads.bsyeti.libs.log_protos import crypta_profile_pb2
from library.python.protobuf.json import proto2json
from yt import yson
from yt.yson import yson_types

from crypta.profile.runners.export_profiles.lib.export.get_logbroker_export_table import GenerateTablesForExport


def is_experiment(crypta_id):
    if crypta_id == 5886085651527201397:
        return 'EXPERIMENTAL_THRESHOLD'
    return 'DEFAULT'


def value_to_dict(row):
    try:
        row["value"] = json.loads(row["value"])
    except Exception:
        user_segments = crypta_profile_pb2.TCryptaLog()
        user_segments.ParseFromString(row["value"])
        row["value"] = json.loads(proto2json.proto2json(user_segments))
    return row


def get_mapper_output(record, not_exported_segments, trainable_segments_ids, trainable_segments_priority=None):
    segments_not_for_export = not_exported_segments or defaultdict(set)
    generate_table_for_logbroker = GenerateTablesForExport(
        timestamp=1535241600,
        segments_not_for_export=segments_not_for_export,
        trainable_segments_ids=trainable_segments_ids,
        trainable_segments_priority=trainable_segments_priority,
        output_to_logbroker=True
    )
    generate_table_for_logbroker.start()
    return [yson.yson_to_json(x) if isinstance(x, yson_types.YsonEntity) else value_to_dict(x) for x in generate_table_for_logbroker(record)]


def test_generate_table_for_logbroker():
    record = {
        'yandexuid': 5886085651527201397,
        'affinitive_site_ids': {'509741312': 0.22107784612940356572},
        'affinitive_sites': {'tripadvisor.ru': 0.22107784612940356572},
        'age_segments': {
            '18_24': 0.075407356023788452148,
            '25_34': 0.18604175746440887451,
            '45_99': 0.4905697256326675415,
            '35_44': 0.21699705719947814941,
            '0_17': 0.030984057113528251648},
        'crypta_id': None,
        'exact_socdem': {
            'gender': 'f',
            'income_segment': 'C',
            'age_segment': '45_54',
            'income_5_segment': 'C1',
        },
        'gender': {
            'm': 0.25574332475662231445,
            'f': 0.74425673484802246094,
        },
        'heuristic_common': [1023],
        'heuristic_internal': None,
        'heuristic_private': None,
        'heuristic_segments': None,
        'icookie': 5886085651527201397,
        'income_5_segments': {
            'A': 0.065630093216896057129,
            'C2': 0.058677759021520614624,
            'C1': 0.49043577909469604492,
            'B1': 0.21185553073883056641,
            'B2': 0.17340087890625,
        },
        'income_segments': {
            'A': 0.065630093216896057129,
            'C': 0.54911353811621665955,
            'B': 0.38525640964508056641,
        },
        'interests_composite': None,
        'lal_common': None,
        'lal_internal': None,
        'lal_private': None,
        'longterm_interests': [128],
        'marketing_segments': {'122': 1.0, '124': 1.0, '265': 1.0},
        'packed_vector': 'somebinarystuff',
        'probabilistic_segments': {
            '570': {'0': 0.48096042871475219727},
            '564': {'0': 0.81513166427612304688},
            '316': {'0': 0.61366866528987884521},
            '460': {'0': 0.62409712374210357666},
            '120': {'0': 0.36075001955032348633},
            '378': {'0': 0.6490888446569442749},
            '272': {'0': 0.090535521507263183594},
            '89': {'0': 0.64524665474891662598},
            '400': {'0': 0.62535262107849121094},
            '8': {'1': 0.0, '0': 1.0},
            '269': {'0': 0.38867357373237609863},
            '167': {'0': 0.11665051430463790894},
            '161': {'0': 0.28584769368171691895},
            '377': {'0': 0.66534456610679626465}},
        'shortterm_interests': None,
        'top_common_site_ids': [509741312, 47800576, 1065944576, 539138304, 180431117, 3513580800],
        'top_common_sites': [
            'tripadvisor.ru',
            'booking.com',
            'ostrovok.ru',
            'lentainform.com',
            'tez-tour.com',
            'drivemusic.me',
        ],
        'update_time': 1535241600,
        'user_age_6s': {
            '25_34': 0.18604175746440887451,
            '0_17': 0.030984057113528251648,
            '18_24': 0.075407356023788452148,
            '55_99': 0.21650798618793487549,
            '45_54': 0.27406173944473266602,
            '35_44': 0.21699705719947814941,
        },
        'yandex_loyalty': 0.0,
        'fields_to_delete': ['heuristic_segments'],
    }

    return get_mapper_output(record, not_exported_segments=None, trainable_segments_ids=None)


def test_exact_socdem_for_logbroker():
    record = {
        'yandexuid': 5886085651527201397,
        'exact_socdem': {
            'gender': 'f',
            'income_segment': 'C',
            'age_segment': '45_54',
            'income_5_segment': 'C1',
        },
    }

    return get_mapper_output(record, not_exported_segments=None, trainable_segments_ids=None)


def test_delete_exact_socdem_for_logbroker():
    record = {
        'yandexuid': 5886085651527201397,
        'top_common_site_ids': [509741312, 47800576, 1065944576, 539138304, 180431117, 3513580800],
        'top_common_sites': [
            'tripadvisor.ru',
            'booking.com',
            'ostrovok.ru',
            'lentainform.com',
            'tez-tour.com',
            'drivemusic.me',
        ],
        'exact_socdem': None,
        'update_time': 1535241600,
        'fields_to_delete': ['exact_socdem'],
    }

    return get_mapper_output(record, not_exported_segments=None, trainable_segments_ids=None)


def test_exact_socdem_no_key_for_logbroker():
    record = {
        'yandexuid': 5886085651527201397,
        'exact_socdem': {
            'gender': 'f',
            'income_segment': 'C',
            'income_5_segment': 'C1',
        },
    }

    return get_mapper_output(record, not_exported_segments=None, trainable_segments_ids=None)


def test_not_export_segments():
    record = {
        'yandexuid': 5886085651527201397,
        'heuristic_common': [1023, 1, 2, 3],
        'heuristic_internal': [1, 2, 3],
        'heuristic_private': [1, 2, 3],
        'heuristic_segments': {
            '1': 1,
            '2': 3
        },
        'icookie': 5886085651527201397,
        'lal_common': {
            '1': 0.1,
            '2': 0.2,
            '3': 0.3
        },
        'lal_internal': {
            '1': 0.1,
            '2': 0.2,
            '3': 0.3
        },
        'lal_private': {
            '1': 0.1,
            '2': 0.2,
            '3': 0.3
        },
        'longterm_interests': [128, 1, 2, 3],
        'shortterm_interests': [1, 2, 3],
    }

    not_exported_segments = defaultdict(
        set,
        {
            544: {2},
            545: {2},
            546: {2},
            547: {2},
            548: {2},
            549: {2},
            601: {2},
            602: {2}
        }
    )
    trainable_segments_ids = {'1', '3'}
    trainable_segments_priority = {'1': 1, '3': 0}

    return get_mapper_output(record, not_exported_segments, trainable_segments_ids, trainable_segments_priority)


def test_empty_keyword():
    record = {
        'yandexuid': 5886085651527201397,
        'heuristic_common': [1023, 1, 2, 3],
        'icookie': 5886085651527201397,
        'lal_common': {
            '1': 0.1,
            '3': 0.2,
        },
        'lal_internal': {
            '1': 0.1,
            '3': 0.3
        },
    }

    not_exported_segments = defaultdict(
        set,
        {
            546: {1, 3},
        }
    )
    trainable_segments_ids = {'1', '3'}
    trainable_segments_priority = {'1': 1, '3': 0}

    return get_mapper_output(record, not_exported_segments, trainable_segments_ids, trainable_segments_priority)
