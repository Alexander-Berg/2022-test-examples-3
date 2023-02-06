import mock
import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
import crypta.profile.lib.test_helpers.task_helpers as task_test_helpers
from crypta.profile.tasks.features.calculate_id_vectors import GetCryptaIdVectors
from crypta.profile.utils import utils
from crypta.profile.utils.config import config


def vertices_no_multi_profile_schema():
    return schema_utils.yt_schema_from_dict({
        'id': 'string',
        'id_type': 'string',
        'cryptaId': 'string',
    }, sort_by=['id', 'id_type'])


def test_get_crypta_id_vectors(local_yt, patched_config, date):
    with mock.patch(
        'crypta.profile.tasks.features.calculate_id_vectors.GetMonthlyYandexuidVectors.complete',
        return_value=True,
    ), mock.patch(
        'crypta.profile.tasks.features.get_app_metrica_data.UpdateMonthlyDevidStorage.complete',
        return_value=True,
    ):
        task = GetCryptaIdVectors(date=date)

        return task_test_helpers.run_and_test_task(
            task=task,
            yt=local_yt,
            data_path=yatest.common.test_source_path('data'),
            input_tables=[
                (
                    tables.get_yson_table_with_schema(
                        'yandexuid_vectors.yson',
                        task.input()['yandexuid_vectors'].table,
                        schema=schema_utils.yt_schema_from_dict(utils.monthly_yandexuid_vector_schema),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'devid_vectors.yson',
                        task.input()['devid_vectors'].table,
                        schema=schema_utils.yt_schema_from_dict(utils.monthly_devid_vector_schema),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'vertices_no_multi_profile.yson',
                        config.VERTICES_NO_MULTI_PROFILE,
                        schema=vertices_no_multi_profile_schema(),
                    ),
                    tests.TableIsNotChanged(),
                ),
            ],
            output_tables=[
                (
                    tables.YsonTable(
                        'crypta_id_vectors.yson',
                        task.output()['vectors_with_yuid'].table,
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
                (
                    tables.YsonTable(
                        'all_crypta_id_vectors.yson',
                        task.output()['all_vectors'].table,
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
            ],
            dependencies_are_missing=False,
        )
