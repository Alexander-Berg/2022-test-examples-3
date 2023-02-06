#!/usr/bin/env python
# -*- coding: utf-8 -*-

from collections import Counter, namedtuple
import copy
from datetime import datetime, timedelta
import logging
import unittest

import luigi
import numpy as np
from tensorflow.keras.layers import Dense
from tensorflow.keras.models import Sequential
import yt.yson as yson

from crypta.profile.lib.socdem_helpers.train_utils.models import convert_simple_keras_to_numpy
from crypta.profile.runners.export_profiles.lib.export import get_daily_export_and_process_bb_storage
from crypta.profile.tasks.features import process_user_events
from crypta.profile.tasks.features.calculate_id_vectors import MonthlyVectorsReducer
from crypta.profile.tasks.monitoring.consistency.bb_consistency_monitoring import check_update_times, is_equal
from crypta.profile.utils import utils
from crypta.profile.utils.segment_utils import builders

logging.basicConfig(level=logging.INFO)

SANDBOX_RESOURCE = 'https://proxy.sandbox.yandex-team.ru/last/LONG_AB_EXPERIMENTS_CONFIG'

stable_socdem_thresholds = {
    "income_segments": {
        "A": 0.38,
        "B": 0.4,
        "C": 0.3
    },
    'income_5_segments': {
        "A": 0.25,
        "B1": 0.25,
        "B2": 0.25,
        "C1": 0.25,
        "C2": 0.25,
    },
    "gender": {
        "m": 0.58,
        "f": 0.65
    },
    "user_age_6s": {
        "0_17": 0.3,
        "18_24": 0.25,
        "25_34": 0.36,
        "35_44": 0.32,
        "45_54": 0.27,
        "55_99": 0.34
    }
}


def probability_is_ok(p):
    return ~(np.isnan(p) | (p > 1) | (p < 0))


class TestTask(luigi.Task):
    param1 = luigi.Parameter()
    param2 = luigi.Parameter()
    param3 = luigi.Parameter(significant=False)

    def __init__(self, *args, **kwargs):
        super(TestTask, self).__init__(*args, **kwargs)

    def run(self):
        pass

    def requires(self):
        return []

    def output(self):
        return []


class TestLuigiInternals(unittest.TestCase):
    def test_get_param_names(self):
        task = TestTask('a', 'b', 'c')

        significant_param_names = sorted(task.get_param_names())
        self.assertEquals(significant_param_names, ['param1', 'param2'])

    def test_get_param_values(self):
        task = TestTask('a', 'b', 'c')

        significant_param_names = sorted(task.get_param_names())
        significant_param_values = [str(getattr(task, key)) for key in significant_param_names]

        self.assertEquals(significant_param_values, ['a', 'b'])

    def test_get_not_str_param_values(self):
        task = TestTask('a', 3, 'c')

        significant_param_names = sorted(task.get_param_names())
        significant_param_values = [str(getattr(task, key)) for key in significant_param_names]

        self.assertEquals(significant_param_values, ['a', '3'])


class TestFormatSegmentReducer(unittest.TestCase):

    key = {
        'id': '00000000000c481f8d2a42623d37753a',
        'id_type': 'uuid'
    }

    def test_non_matching_row(self):
        name_segment_dict = {
            'Name1': (547, 1265),
            'Name2': (547, 1287),
            'Name3': (547, 1288),
            'Name4': (557, 1342)
        }

        rows = []

        reducer = builders.FormatSegmentReducer(name_segment_dict)
        for result in reducer(self.key, rows):
            self.assertEqual(result, None)

    def test_single_keyword_with_single_export(self):
        name_segment_dict = {
            'Name1': (547, 1265)
        }

        rows = [{'segment_name': 'Name1'}]

        correct_output = {
            'id': '00000000000c481f8d2a42623d37753a',
            'id_type': 'uuid',
            'heuristic_common': [yson.YsonUint64('1265')]
        }

        reducer = builders.FormatSegmentReducer(name_segment_dict)

        for result in reducer(self.key, rows):
            for key in ['id', 'id_type']:
                self.assertEqual(result[key], correct_output[key])
            self.assertEqual(set(result['heuristic_common']), set(correct_output['heuristic_common']))

    def test_single_keyword_with_multiple_export(self):
        name_segment_dict = {
            'Name1': (547, 1265),
            'Name2': (547, 1287),
            'Name3': (547, 1288)
        }

        rows = [{'segment_name': 'Name1'}, {'segment_name': 'Name2'}]

        correct_output = {
            'id': '00000000000c481f8d2a42623d37753a',
            'id_type': 'uuid',
            'heuristic_common': [yson.YsonUint64('1265'), yson.YsonUint64('1287')]
        }

        reducer = builders.FormatSegmentReducer(name_segment_dict)

        for result in reducer(self.key, rows):
            for key in ['id', 'id_type']:
                self.assertEqual(result[key], correct_output[key])
            self.assertEqual(set(result['heuristic_common']), set(correct_output['heuristic_common']))

    def test_multiple_keywords_with_single_export(self):
        name_segment_dict = {
            'Name1': (547, 1265),
            'Name4': (557, 1342)
        }

        rows = [{'segment_name': 'Name1'}, {'segment_name': 'Name4'}]

        correct_output = {
            'id': '00000000000c481f8d2a42623d37753a',
            'id_type': 'uuid',
            'heuristic_common': [yson.YsonUint64('1265')],
            'audience_segments': [yson.YsonUint64('1342')]
        }

        reducer = builders.FormatSegmentReducer(name_segment_dict)

        for result in reducer(self.key, rows):
            for key in ['id', 'id_type']:
                self.assertEqual(result[key], correct_output[key])
            self.assertEqual(set(result['heuristic_common']), set(correct_output['heuristic_common']))
            self.assertEqual(set(result['audience_segments']), set(correct_output['audience_segments']))

    def test_multiple_keywords_with_multiple_exports(self):
        name_segment_dict = {
            'Name1': (547, 1265),
            'Name2': (547, 1287),
            'Name3': (547, 1288),
            'Name4': (557, 1342)
        }

        rows = [{'segment_name': 'Name1'}, {'segment_name': 'Name2'}, {'segment_name': 'Name4'}]

        correct_output = {
            'id': '00000000000c481f8d2a42623d37753a',
            'id_type': 'uuid',
            'heuristic_common': [yson.YsonUint64('1265'), yson.YsonUint64('1287')],
            'audience_segments': [yson.YsonUint64('1342')]
        }

        reducer = builders.FormatSegmentReducer(name_segment_dict)

        for result in reducer(self.key, rows):
            for key in ['id', 'id_type']:
                self.assertEqual(result[key], correct_output[key])
            self.assertEqual(set(result['heuristic_common']), set(correct_output['heuristic_common']))
            self.assertEqual(set(result['audience_segments']), set(correct_output['audience_segments']))


class TestHostnameProcessing(unittest.TestCase):
    def test_is_valid_hostname(self):
        self.assertTrue(
            process_user_events.is_valid_hostname('yandex.ru'))
        self.assertTrue(
            process_user_events.is_valid_hostname('www.yandex.ru'))
        self.assertTrue(
            process_user_events.is_valid_hostname('192.168.0.1'))

        self.assertFalse(
            process_user_events.is_valid_hostname('яндекс.ru'))
        self.assertFalse(
            process_user_events.is_valid_hostname('jkfd@jkfdkd.com'))
        self.assertFalse(
            process_user_events.is_valid_hostname('!jkfd.com'))
        self.assertFalse(process_user_events.is_valid_hostname(''))


class TestKeras(unittest.TestCase):
    def test_keras_to_numpy_transformation(self):
        nn = Sequential([Dense(3, input_dim=10, activation='softmax')])
        nn.compile('adam', loss='mse')
        numpy_nn = convert_simple_keras_to_numpy(nn)
        data = np.random.normal(size=(30, 10))
        self.assertTrue(np.allclose(nn.predict(data), numpy_nn.predict(data)))

    def test_keras_model_training(self):
        nn = Sequential([
            Dense(50, input_dim=10, activation='relu'),
            Dense(1)
        ])
        nn.compile('adam', loss='mse')
        X = np.random.normal(size=(100, 10))
        y = np.random.normal(size=(100,))
        nn.fit(X, y, verbose=0, validation_split=0.3)
        nn.predict(X)


class TestUtils(unittest.TestCase):
    def test_normalize_weights(self):
        self.assertTrue(np.allclose(
            utils.normalize_weights(np.array([0.09, 0.01])),
            np.array([1.8, 0.2])
        ))

    def test_is_valid_uint64(self):
        self.assertTrue(utils.is_valid_uint64(0))
        self.assertTrue(utils.is_valid_uint64('0'))
        self.assertTrue(utils.is_valid_uint64(18431482921371144894))
        self.assertTrue(utils.is_valid_uint64('18431482921371144894'))

        self.assertFalse(utils.is_valid_uint64(-13))
        self.assertFalse(utils.is_valid_uint64('18531482921371144894'))
        self.assertFalse(utils.is_valid_uint64('3.14'))
        self.assertFalse(utils.is_valid_uint64('pi'))
        self.assertFalse(utils.is_valid_uint64(''))
        self.assertFalse(utils.is_valid_uint64(None))


class TestBbStorageUploadOptimizer(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.up_to_date_timestamp = 1492982298
        cls.yesterday_timestamp = 1492982298 - 86400
        cls.outdated_entry_threshold = cls.up_to_date_timestamp - \
                                       (14 * 24 * 60 * 60)
        cls.outdated_shortterm_interests_threshold = cls.up_to_date_timestamp - \
                                                     (3 * 24 * 60 * 60)
        cls.outdated_timestamp = cls.outdated_entry_threshold - 1
        Context = namedtuple('Context', 'table_index')
        cls.context = Context
        cls.maxDiff = 65536

    def _convert_input_record_generator(self, records):
        for record in records:
            self.context.table_index = record['@table_index']
            yield record

    def _convert_output_record_generator(self, generator):
        for record in generator:
            if isinstance(record, yson.yson_types.YsonEntity):
                table_index = record.__dict__['attributes']['table_index']
                record = next(generator)
                record['@table_index'] = table_index
            yield record

    def test_process_bb_storage(self):
        daily_profiles_record = {
            'yandexuid': 7051307558878,
            'update_time': self.up_to_date_timestamp,
            '@table_index': 0,
            'yandex_loyalty': 0,
            'user_age_6s': {
                '0_17': 0.020853,
                '18_24': 0.112856,
                '25_34': 0.519635,
                '35_44': 0.219918,
                '45_54': 0.093622,
                '55_99': 0.033114
            },
            'age_segments': {
                '0_17': 0.020853,
                '18_24': 0.112856,
                '25_34': 0.519635,
                '35_44': 0.219918,
                '45_99': 0.126736
            },
            'probabilistic_segments': {
                '101': {
                    '0': 0
                },
                '102': {
                    '0': 0
                },
                '122': {
                    '0': 0.225408
                },
                '188': {
                    '0': 0.215815
                },
            }
        }
        storage_record_1 = copy.deepcopy(daily_profiles_record)
        storage_record_1['@table_index'] = 1

        storage_record_2 = {
            'yandexuid': 7051307558878,
            'update_time': self.up_to_date_timestamp,
            '@table_index': 1,
            'yandex_loyalty': 0,
            'user_age_6s': {
                '0_17': 0.030853,
                '18_24': 0.102856,
                '25_34': 0.519635,
                '35_44': 0.219918,
                '45_54': 0.093622,
                '55_99': 0.033114
            },
            'age_segments': {
                '0_17': 0.310853,
                '18_24': 0.122856,
                '25_34': 0.219635,
                '35_44': 0.219918,
                '45_99': 0.126736
            },
            'probabilistic_segments': {
                '102': {
                    '0': 0
                },
                '122': {
                    '0': 0.225408
                },
                '188': {
                    '0': 0.215815
                },
            }
        }

        logging.info('bb_storage_upload_optimizer')
        self.maxDiff = 65536
        result = {}

        bb_storage_processor = get_daily_export_and_process_bb_storage.BbStorageProcessor(
            self.outdated_entry_threshold, self.outdated_shortterm_interests_threshold, )

        output_generator = bb_storage_processor(
            key=None, records=self._convert_input_record_generator(
                (copy.deepcopy(daily_profiles_record), storage_record_1)), context=self.context)

        for record in self._convert_output_record_generator(output_generator):
            if record['@table_index'] == 0:
                result['daily_export'] = record
            elif record['@table_index'] == 1:
                result['updated_storage'] = record

        self.assertEqual(
            result['daily_export'],
            daily_profiles_record,
        )

        self.assertEqual(
            result['updated_storage'],
            {
                'yandexuid': 7051307558878,
                'update_time': self.up_to_date_timestamp,
                '@table_index': 1,
                'yandex_loyalty': 0,
                'user_age_6s': {
                    '0_17': 0.020853,
                    '18_24': 0.112856,
                    '25_34': 0.519635,
                    '35_44': 0.219918,
                    '45_54': 0.093622,
                    '55_99': 0.033114
                },
                'age_segments': {
                    '0_17': 0.020853,
                    '18_24': 0.112856,
                    '25_34': 0.519635,
                    '35_44': 0.219918,
                    '45_99': 0.126736
                },
                'probabilistic_segments': {
                    '101': {
                        '0': 0
                    },
                    '102': {
                        '0': 0
                    },
                    '122': {
                        '0': 0.225408
                    },
                    '188': {
                        '0': 0.215815
                    },
                },
            }
        )

        result2 = {}
        bb_storage_processor = get_daily_export_and_process_bb_storage.BbStorageProcessor(
            self.outdated_entry_threshold, self.outdated_shortterm_interests_threshold, )

        output_generator = bb_storage_processor(
            key=None, records=self._convert_input_record_generator(
                (daily_profiles_record, storage_record_2)), context=self.context)

        for record in self._convert_output_record_generator(output_generator):
            if record['@table_index'] == 0:
                result2['daily_export'] = record
            elif record['@table_index'] == 1:
                result2['updated_storage'] = record

        self.assertEqual(
            result2['daily_export'],
            daily_profiles_record,
        )

        new_storage_record = daily_profiles_record.copy()
        new_storage_record['@table_index'] = 1

        self.assertEqual(
            result2['updated_storage'],
            new_storage_record,
        )

    def test_none_storage(self):
        daily_profiles_record = {
            '@table_index': 0,
            'update_time': self.up_to_date_timestamp,
            'yandex_loyalty': 0.31337,
        }
        storage_record_record = {
            '@table_index': 1,
            'update_time': self.yesterday_timestamp,
            'yandex_loyalty': None
        }
        result = {}

        bb_storage_processor = get_daily_export_and_process_bb_storage.BbStorageProcessor(
            self.outdated_entry_threshold, self.outdated_shortterm_interests_threshold, )

        output_generator = bb_storage_processor(
            key=None, records=self._convert_input_record_generator(
                (daily_profiles_record, storage_record_record)), context=self.context)

        for record in self._convert_output_record_generator(output_generator):
            if record['@table_index'] == 0:
                result['daily_export'] = record
            elif record['@table_index'] == 1:
                result['updated_storage'] = record
        self.assertEqual(
            result['daily_export'],
            {
                '@table_index': 0,
                'update_time': self.up_to_date_timestamp,
                'yandex_loyalty': 0.31337,
            }
        )
        self.assertEqual(
            result['updated_storage'],
            {
                '@table_index': 1,
                'update_time': self.up_to_date_timestamp,
                'yandex_loyalty': 0.31337,
            },
        )

    def test_none_profile_outdated_storage(self):
        get_daily_export_and_process_bb_storage.outdated_entry_threshold = 100500
        storage_record = {
            '@table_index': 1,
            'update_time': self.outdated_timestamp,
            'yandex_loyalty': 0.31337
        }
        result = {}
        bb_storage_processor = get_daily_export_and_process_bb_storage.BbStorageProcessor(
            self.outdated_entry_threshold, self.outdated_shortterm_interests_threshold, )

        output_generator = bb_storage_processor(
            key=None,
            records=self._convert_input_record_generator((storage_record,)),
            context=self.context
        )

        for record in self._convert_output_record_generator(output_generator):
            if record['@table_index'] == 0:
                result['daily_export'] = record
            elif record['@table_index'] == 1:
                result['updated_storage'] = record
        self.assertEqual(result, {})

    def test_none_profile_not_outdated_storage(self):
        storage_record = {
            '@table_index': 1,
            'update_time': self.yesterday_timestamp,
            'yandex_loyalty': 0.31337,
            'gender': {'m': 0.31337, 'f': 0.68663},
            'user_age_6s': {
                '0_17': 0.030853,
                '18_24': 0.102856,
                '25_34': 0.519635,
                '35_44': 0.219918,
                '45_54': 0.093622,
                '55_99': 0.033114
            },
            'age_segments': {
                '18_24': 0.112856,
                '25_34': 0.519635,
                '45_99': 0.126736,
                '35_44': 0.219918,
                '0_17': 0.020853,
            },
            'income_segments': {'A': 0.4, 'B': 0.4, 'C': 0.2},
            'income_5_segments': {
                'A': 0.4,
                'B1': 0.2,
                'B2': 0.2,
                'C1': 0.1,
                'C2': 0.1,
            },
        }

        result = {}
        bb_storage_processor = get_daily_export_and_process_bb_storage.BbStorageProcessor(
            self.outdated_entry_threshold, self.outdated_shortterm_interests_threshold, )

        output_generator = bb_storage_processor(
            key=None,
            records=self._convert_input_record_generator((storage_record,)),
            context=self.context
        )

        for record in self._convert_output_record_generator(output_generator):
            if record['@table_index'] == 0:
                result['daily_export'] = record
            elif record['@table_index'] == 1:
                result['updated_storage'] = record
        self.assertEqual(
            result['updated_storage'],
            {
                '@table_index': 1,
                'update_time': self.yesterday_timestamp,
                'yandex_loyalty': 0.31337,
                'gender': {'m': 0.31337, 'f': 0.68663},
                'user_age_6s': {
                    '0_17': 0.030853,
                    '18_24': 0.102856,
                    '25_34': 0.519635,
                    '35_44': 0.219918,
                    '45_54': 0.093622,
                    '55_99': 0.033114,
                },
                'income_segments': {'A': 0.4, 'B': 0.4, 'C': 0.2},
                'income_5_segments': {
                    'A': 0.4,
                    'B1': 0.2,
                    'B2': 0.2,
                    'C1': 0.1,
                    'C2': 0.1,
                },
                'age_segments': {
                    '18_24': 0.112856,
                    '25_34': 0.519635,
                    '45_99': 0.126736,
                    '35_44': 0.219918,
                    '0_17': 0.020853
                },
            },
        )
        self.assertEqual(len(result), 1)

    def test_none_profile_field_not_outdated_storage(self):
        daily_profiles_record = {
            '@table_index': 0,
            'update_time': self.up_to_date_timestamp,
            'gender': {'m': 0.3, 'f': 0.7},
            'yandex_loyalty': None,
        }
        storage_record = {
            '@table_index': 1,
            'update_time': self.yesterday_timestamp,
            'yandex_loyalty': 0.31337,
            'gender': {'m': 0.31337, 'f': 0.68663},
        }
        result = {}
        bb_storage_processor = get_daily_export_and_process_bb_storage.BbStorageProcessor(
            self.outdated_entry_threshold, self.outdated_shortterm_interests_threshold, )

        output_generator = bb_storage_processor(
            key=None, records=self._convert_input_record_generator(
                (storage_record, daily_profiles_record)), context=self.context)

        for record in self._convert_output_record_generator(output_generator):
            if record['@table_index'] == 0:
                result['daily_export'] = record
            elif record['@table_index'] == 1:
                result['updated_storage'] = record
        self.assertEqual(
            result['updated_storage'],
            {
                '@table_index': 1,
                'update_time': self.up_to_date_timestamp,
                'gender': {'m': 0.3, 'f': 0.7},
                'yandex_loyalty': None,
            },
        )
        self.assertEqual(
            result['daily_export'],
            {
                '@table_index': 0,
                'update_time': self.up_to_date_timestamp,
                'yandex_loyalty': None,
                'gender': {'m': 0.3, 'f': 0.7},
                'fields_to_delete': ['yandex_loyalty'],
            }
        )

    def test_both_none(self):
        daily_profiles_record = {
            '@table_index': 0,
            'update_time': self.up_to_date_timestamp,
            'yandex_loyalty': None,
        }
        storage_record_record = {
            '@table_index': 1,
            'update_time': self.yesterday_timestamp,
            'yandex_loyalty': None,
        }
        result = {}
        bb_storage_processor = get_daily_export_and_process_bb_storage.BbStorageProcessor(
            self.outdated_entry_threshold, self.outdated_shortterm_interests_threshold, )

        output_generator = bb_storage_processor(
            key=None, records=self._convert_input_record_generator(
                (daily_profiles_record, storage_record_record)), context=self.context)

        for record in self._convert_output_record_generator(output_generator):
            if record['@table_index'] == 0:
                result['daily_export'] = record
            elif record['@table_index'] == 1:
                result['updated_storage'] = record
        self.assertEqual(
            result['daily_export'],
            {
                '@table_index': 0,
                'update_time': self.up_to_date_timestamp,
                'yandex_loyalty': None,
            },
        )
        self.assertEqual(
            result['updated_storage'],
            {
                '@table_index': 1,
                'update_time': self.up_to_date_timestamp,
                'yandex_loyalty': None,
            }
        )

    def test_shortterm_interests_refresh(self):
        daily_profiles_record = {
            '@table_index': 0,
            'update_time': self.up_to_date_timestamp,
            'shortterm_interests': {
                '1': self.up_to_date_timestamp,
                '4': self.up_to_date_timestamp,
            }
        }
        storage_record = {
            '@table_index': 1,
            'update_time': self.yesterday_timestamp,
            'shortterm_interests': {
                '1': self.yesterday_timestamp,
                '2': self.outdated_shortterm_interests_threshold - 1,
                '3': self.yesterday_timestamp,
            }
        }
        result = {}
        bb_storage_processor = get_daily_export_and_process_bb_storage.BbStorageProcessor(
            self.outdated_entry_threshold, self.outdated_shortterm_interests_threshold, )

        output_generator = bb_storage_processor(
            key=None, records=self._convert_input_record_generator(
                (daily_profiles_record, storage_record)), context=self.context)

        for record in self._convert_output_record_generator(output_generator):
            if record['@table_index'] == 0:
                result['daily_export'] = record
            elif record['@table_index'] == 1:
                result['updated_storage'] = record
        self.assertEqual(
            result['daily_export'],
            {
                '@table_index': 0,
                'update_time': self.up_to_date_timestamp,
            },
        )
        self.assertEqual(
            result['updated_storage'],
            {
                '@table_index': 1,
                'update_time': self.up_to_date_timestamp,
                'shortterm_interests': {
                    '1': self.up_to_date_timestamp,
                    '3': self.yesterday_timestamp,
                    '4': self.up_to_date_timestamp,
                }
            }
        )

    def test_shortterm_interests_addition(self):
        daily_profiles_record = {
            '@table_index': 0,
            'update_time': self.up_to_date_timestamp,
            'shortterm_interests': {
                '1': self.up_to_date_timestamp,
            }
        }
        result = {}
        bb_storage_processor = get_daily_export_and_process_bb_storage.BbStorageProcessor(
            self.outdated_entry_threshold, self.outdated_shortterm_interests_threshold, )

        output_generator = bb_storage_processor(
            key=None, records=self._convert_input_record_generator(
                (daily_profiles_record,)), context=self.context)

        for record in self._convert_output_record_generator(output_generator):
            if record['@table_index'] == 0:
                result['daily_export'] = record
            elif record['@table_index'] == 1:
                result['updated_storage'] = record
        self.assertEqual(
            result['daily_export'],
            {
                '@table_index': 0,
                'update_time': self.up_to_date_timestamp,
            }
        )
        self.assertEqual(
            result['updated_storage'],
            {
                '@table_index': 1,
                'update_time': self.up_to_date_timestamp,
                'shortterm_interests': {
                    '1': self.up_to_date_timestamp,
                }
            }
        )

    def test_shortterm_interests_deletion(self):
        storage_record = {
            '@table_index': 1,
            'update_time': self.yesterday_timestamp,
            'gender': {'m': 0.31337, 'f': 0.68663},
            'user_age_6s': {
                '0_17': 0.030853,
                '18_24': 0.102856,
                '25_34': 0.519635,
                '35_44': 0.219918,
                '45_54': 0.093622,
                '55_99': 0.033114
            },
            'yandex_loyalty': 0.31337,
            'age_segments': {
                '18_24': 0.112856,
                '25_34': 0.519635,
                '45_99': 0.126736,
                '35_44': 0.219918,
                '0_17': 0.020853,
            },
            'income_segments': {'A': 0.4, 'B': 0.4, 'C': 0.2},
            'income_5_segments': {
                'A': 0.4,
                'B1': 0.2,
                'B2': 0.2,
                'C1': 0.1,
                'C2': 0.1,
            },
            'shortterm_interests': {
                '2': self.outdated_shortterm_interests_threshold - 1,
            }
        }
        result = {}
        bb_storage_processor = get_daily_export_and_process_bb_storage.BbStorageProcessor(
            self.outdated_entry_threshold, self.outdated_shortterm_interests_threshold, )

        output_generator = bb_storage_processor(
            key=None,
            records=self._convert_input_record_generator((storage_record,)),
            context=self.context
        )

        for record in self._convert_output_record_generator(output_generator):
            if record['@table_index'] == 0:
                result['daily_export'] = record
            elif record['@table_index'] == 1:
                result['updated_storage'] = record

        self.assertFalse('daily_export' in result)
        self.assertEqual(
            result['updated_storage'],
            {
                '@table_index': 1,
                'update_time': self.yesterday_timestamp,
                'gender': {'m': 0.31337, 'f': 0.68663},
                'user_age_6s': {
                    '0_17': 0.030853,
                    '18_24': 0.102856,
                    '25_34': 0.519635,
                    '35_44': 0.219918,
                    '45_54': 0.093622,
                    '55_99': 0.033114
                },
                'yandex_loyalty': 0.31337,
                'age_segments': {
                    '18_24': 0.112856,
                    '25_34': 0.519635,
                    '45_99': 0.126736,
                    '35_44': 0.219918,
                    '0_17': 0.020853,
                },
                'income_segments': {'A': 0.4, 'B': 0.4, 'C': 0.2},
                'income_5_segments': {
                    'A': 0.4,
                    'B1': 0.2,
                    'B2': 0.2,
                    'C1': 0.1,
                    'C2': 0.1,
                },
                'shortterm_interests': None,
            },
        )


class TestMonitorBBConsistency(unittest.TestCase):
    def test_check_update_times(self):
        good_update_time = 1567425600

        update_times = {
            175: Counter({good_update_time: 5}),
            198: Counter({good_update_time: 1}),
            216: Counter({good_update_time: 1}),
            217: Counter({good_update_time: 4}),
            220: Counter({good_update_time: 1}),
            281: Counter({good_update_time: 2}),
            547: Counter({good_update_time: 1}),
            549: Counter({good_update_time: 1}),
            595: Counter({good_update_time: 1}),
            601: Counter({good_update_time + 4: 1}),

            877: Counter({good_update_time: 1, good_update_time + 20: 1}),
            878: Counter({good_update_time: 6}),
            879: Counter({good_update_time: 3}),
            880: Counter({good_update_time: 5}),

            885: Counter({good_update_time: 1}),
            886: Counter({good_update_time: 1}),
            887: Counter({good_update_time: 1}),
            888: Counter({good_update_time: 1}),

            174: Counter({good_update_time: 2}),
            176: Counter({good_update_time: 3}),
            543: Counter({good_update_time + 20: 6}),
            614: Counter({good_update_time: 5}),

            569: Counter({good_update_time + 30: 1}),
        }

        exact_socdem = {
            'gender': 'm',
            'income_segment': 'B',
            'income_5_segment': 'B1',
        }

        error_messages = check_update_times(update_times, good_update_time, exact_socdem)
        self.assertEquals(len(error_messages), 1)
        self.assertTrue('877' in error_messages[0])

    def test_is_equal(self):
        positive_cases = [
            (0.112312, 0.11235),
            ([123, 456], [123, 456]),
            (
                {
                    "0_17": 0.039373,
                    "18_24": 0.16629,
                    "25_34": 0.432156,
                    "35_44": 0.17686,
                    "45_54": 0.131393,
                    "55_99": 0.053924,
                },
                {
                    "0_17": 0.039372,
                    "18_24": 0.16629,
                    "25_34": 0.432156,
                    "35_44": 0.17686,
                    "45_54": 0.131394,
                    "55_99": 0.053924,
                },
            ),
            (
                {
                    "11": 1,
                    "160": 1,
                    "165": 1,
                    "22": 1
                },
                {
                    "11": 1,
                    "160": 1,
                    "165": 1,
                    "22": 1
                }
            ),
            (
                {
                    "101": {
                        "0": 0.632098
                    },
                    "109": {
                        "0": 0.139339
                    }
                },
                {
                    "101": {
                        "0": 0.632099
                    },
                    "109": {
                        "0": 0.139338
                    }
                }
            ),
            (
                {
                    '199': {'0': 0.224586},
                    '316': {'0': 0.157321},
                    '315': {'0': 0.150799},
                    '314': {'0': 0.483434},
                    '89': {'0': 0.405778},
                    '275': {'0': 0.51639},
                    '399': {'0': 0.902645},
                    '398': {'0': 0.838188},
                    '429': {'0': 0.237196},
                    '8': {'1': 0.95, '0': 0.05},
                },
                {
                    '199': {'0': 0.224586},
                    '316': {'0': 0.157321},
                    '315': {'0': 0.150799},
                    '314': {'0': 0.483434},
                    '89': {'0': 0.405778},
                    '275': {'0': 0.51639},
                    '399': {'0': 0.902645},
                    '398': {'0': 0.838188},
                    '429': {'0': 0.237196},
                    '8': {'1': 0.95, '0': 0.05},
                },
            ),
            ([123, 456], [123], ["456", "457"]),
            (
                {
                    "101": {
                        "0": 0.632098
                    },
                    "109": {
                        "0": 0.139339
                    }
                },
                {
                    "101": {
                        "0": 0.632099
                    },
                },
                ["108", "109"]
            ),
        ]
        negative_cases = [
            (1, None),
            (0.112312, 0.21235),
            ([123, 456], [123, 456, 13]),
            (
                {
                    "0_17": 0.019373,
                    "18_24": 0.16629,
                    "25_34": 0.432156,
                    "35_44": 0.17686,
                    "45_54": 0.131393,
                    "55_99": 0.053924,
                },
                {
                    "0_17": 0.059372,
                    "18_24": 0.16629,
                    "25_34": 0.432156,
                    "35_44": 0.17686,
                    "45_54": 0.131394,
                    "55_99": 0.053924,
                }
            ),
            (
                {
                    "11": 1,
                    "160": 1,
                    "165": 1,
                    "22": 1,
                },
                {
                    "11": 1,
                    "160": 1,
                    "22": 1,
                },
            ),
            (
                {
                    "101": {
                        "0": 0.232098
                    },
                    "109": {
                        "0": 0.139339
                    }
                },
                {
                    "101": {
                        "0": 0.632099
                    },
                    "109": {
                        "0": 0.139338
                    }
                },
            ),
            ([123, 456], [123], ["457", "458"]),
            ([123, 456], [123, 457], ["456", "457"]),
            (
                {
                    "101": {
                        "0": 0.632098
                    },
                    "109": {
                        "0": 0.139339
                    }
                },
                {
                    "101": {
                        "0": 0.632099
                    },
                },
                ["108"]
            ),
        ]

        for positive_case in positive_cases:
            equal, reason = is_equal(*positive_case)
            self.assertTrue(equal, reason)

        for negative_case in negative_cases:
            equal, reason = is_equal(*negative_case)
            self.assertFalse(equal, reason)


class TestGetExactSocdemString(unittest.TestCase):
    def test_get_exact_socdem_dict(self):
        record = {
            'gender': {
                "f": 0.45616565942764282,
                "m": 0.5438343405723572
            },
            'user_age_6s': {
                "0_17": 0.05643466114997864,
                "18_24": 0.26713749915361404,
                "25_34": 0.2849578559398651,
                "35_44": 0.21707038283348083,
                "45_54": 0.09360539317131042,
                "55_99": 0.08079417049884796
            },
            'income_segments': {
                "A": 0.39,
                "B": 0.41,
                "C": 0.2
            },
            'income_5_segments': {
                'A': 0.39,
                'B1': 0.409,
                'B2': 0.001,
                'C1': 0.1,
                'C2': 0.1,
            },
        }
        correct = {
            'age_segment': '18_24',
            'income_segment': 'A',
            'income_5_segment': 'B1',
        }
        self.assertEqual(
            utils.get_exact_socdem_dict(
                record,
                stable_socdem_thresholds,
            ),
            correct,
        )


class TestGetYandexuidVectors(unittest.TestCase):
    @classmethod
    def setUpClass(self):
        Context = namedtuple('Context', 'table_index')
        self.context = Context

    def _convert_input_record_generator(self, records):
        for record in records:
            self.context.table_index = record['@table_index']
            yield record

    def _convert_output_record_generator(self, generator):
        for record in generator:
            if isinstance(record, yson.yson_types.YsonEntity):
                table_index = record.__dict__['attributes']['table_index']
                record = next(generator)
                record['@table_index'] = table_index
            yield record

    def test_merger(self):
        base_date_string = '2017-09-14'
        number_of_days_to_keep_vectors = 35

        base_date = datetime.strptime(base_date_string, '%Y-%m-%d').date()
        date_list = list(sorted([str(base_date - timedelta(days=x))
                                 for x in range(0, number_of_days_to_keep_vectors)]))

        merger = MonthlyVectorsReducer(base_date_string, number_of_days_to_keep_vectors)

        daily_record = {
            '@table_index': 0,
            'vector': 'daily vector',
        }
        monthly_record = {
            '@table_index': 1,
            'vector': 'monthly vector',
            'days_active': date_list,
        }
        correct_output = [{
            '@table_index': 0,
            'vector': 'daily vector',
            'days_active': [
                '2017-08-11',
                '2017-08-12',
                '2017-08-13',
                '2017-08-14',
                '2017-08-15',
                '2017-08-16',
                '2017-08-17',
                '2017-08-18',
                '2017-08-19',
                '2017-08-20',
                '2017-08-21',
                '2017-08-22',
                '2017-08-23',
                '2017-08-24',
                '2017-08-25',
                '2017-08-26',
                '2017-08-27',
                '2017-08-28',
                '2017-08-29',
                '2017-08-30',
                '2017-08-31',
                '2017-09-01',
                '2017-09-02',
                '2017-09-03',
                '2017-09-04',
                '2017-09-05',
                '2017-09-06',
                '2017-09-07',
                '2017-09-08',
                '2017-09-09',
                '2017-09-10',
                '2017-09-11',
                '2017-09-12',
                '2017-09-13',
                '2017-09-14',
            ]
        }]

        output_records = []
        output_generator = merger(
            key=None,
            records=self._convert_input_record_generator((daily_record, monthly_record)),
            context=self.context,
        )
        for record in self._convert_output_record_generator(output_generator):
            output_records.append(record)

        self.assertEqual(output_records, correct_output)


if __name__ == '__main__':
    unittest.main()
