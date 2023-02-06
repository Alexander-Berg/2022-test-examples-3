from collections import (
    namedtuple,
    OrderedDict,
)
import functools

import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.runners.segments.lib.coded_segments.custom_classification.abstract_model_application import AbstractModelApplication


ModelParams = namedtuple('ModelParams', ['ordered_thresholds'])


class ThresholdModelTrainHelper:
    @property
    def model_params(self):
        return ModelParams(
            ordered_thresholds={
                'A': 0.19,
                'B': 0.39,
                'C': 0.59,
            }
        )


class EmptyModelApplication(AbstractModelApplication):
    def requires(self):
        return None

    @property
    def name_segment_dict(self):
        return None

    @property
    def probability_classes(self):
        return None

    @property
    def slice_to_segment_name_dict(self):
        return None

    @property
    def percentiles(self):
        return None

    @property
    def train_helper(self):
        return None

    @property
    def positive_name(self):
        return None


class ThresholdModelApplication(EmptyModelApplication):
    keyword = 546

    name_segment_dict = {
        'A': 1,
        'B': 2,
        'C': 3,
    }

    @property
    def train_helper(self):
        return ThresholdModelTrainHelper()


class ProbabilityModelApplication(EmptyModelApplication):
    keyword = 546

    name_segment_dict = {
        'percentile_0_30': 1,
        'percentile_30_70': 2,
        'percentile_70_100': 3,
    }

    @property
    def percentiles(self):
        return OrderedDict([
            ('percentile_70_100', (0.0, 0.3)),
            ('percentile_20_70', (0.3, 0.8)),
            ('percentile_0_20', (0.8, 1.0)),
        ])

    @property
    def slice_to_segment_name_dict(self):
        return {
            0: 'percentile_70_100',
            1: 'percentile_20_70',
            2: 'percentile_0_20',
        }

    @property
    def probability_classes(self):
        return [
            'negative',
            'positive',
        ]

    @property
    def positive_name(self):
        return 'positive'


class IntegralScoreModelApplication(EmptyModelApplication):
    keyword = 546

    name_segment_dict = {
        'percentile_0_30': 1,
        'percentile_30_70': 2,
        'percentile_70_100': 3,
    }

    @property
    def percentiles(self):
        return OrderedDict([
            ('percentile_70_100', (0.0, 0.3)),
            ('percentile_20_70', (0.3, 0.8)),
            ('percentile_0_20', (0.8, 1.0)),
        ])

    @property
    def slice_to_segment_name_dict(self):
        return {
            0: 'percentile_70_100',
            1: 'percentile_20_70',
            2: 'percentile_0_20',
        }

    @property
    def probability_classes(self):
        return ['A', 'B', 'C']


def make_map_prediction_test(
    local_yt,
    model_application,
    input_table_file,
    input_table_yt_path,
    output_table_file,
    output_table_yt_path,
):
    input_table = tables.YsonTable(
        input_table_file,
        input_table_yt_path,
        yson_format='pretty',
    )
    output_table = tables.YsonTable(
        output_table_file,
        output_table_yt_path,
        yson_format='pretty',
    )

    return tests.yt_test_func(
        yt_client=local_yt.get_yt_client(),
        func=functools.partial(
            model_application.map_predictions_to_segments,
            model_predictions=input_table.cypress_path,
            output_path=output_table.cypress_path,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (input_table, tests.TableIsNotChanged()),
        ],
        output_tables=[
            (output_table, tests.Diff()),
        ],
    )


def test_map_predictions_to_segments_via_threshold(
    local_yt,
    patched_config,
    date,
):
    patched_config.environment = 'local_testing'

    return make_map_prediction_test(
        local_yt=local_yt,
        model_application=ThresholdModelApplication(date=date),
        input_table_file='model_predictions_multiclass.yson',
        input_table_yt_path='//home/crypta/custom_classification/threshold_input',
        output_table_file='threshold_output.yson',
        output_table_yt_path='//home/crypta/custom_classification/threshold_output',
    )


def test_map_predictions_to_segments_via_probability_percentile(
    local_yt,
    patched_config,
    date,
):
    patched_config.environment = 'local_testing'

    return make_map_prediction_test(
        local_yt=local_yt,
        model_application=ProbabilityModelApplication(date=date),
        input_table_file='model_predictions_binary.yson',
        input_table_yt_path='//home/crypta/custom_classification/probability_percentile_input',
        output_table_file='probability_percentile_output.yson',
        output_table_yt_path='//home/crypta/custom_classification/probability_percentile_output',
    )


def test_map_predictions_to_segments_via_integral_score(
    local_yt,
    patched_config,
    date,
):
    patched_config.environment = 'local_testing'

    return make_map_prediction_test(
        local_yt=local_yt,
        model_application=IntegralScoreModelApplication(date=date),
        input_table_file='model_predictions_multiclass.yson',
        input_table_yt_path='//home/crypta/custom_classification/integral_score_input',
        output_table_file='integral_score_output.yson',
        output_table_yt_path='//home/crypta/custom_classification/integral_score_output',
    )
