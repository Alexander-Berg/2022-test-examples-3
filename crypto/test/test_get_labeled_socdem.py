import mock
import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.runners.matching.lib.get_labeled_socdem import GetLabeledSocdem


def test_get_labeled_socdem(local_yt, patched_config, date):
    task = GetLabeledSocdem(date=date)

    join_socdem_storage = tables.YsonTable(
        file_path='join_socdem_storage.yson',
        cypress_path=task.input().table,
        on_write=tables.OnWrite(
            attributes={
                'schema': [
                    {'name': 'gender_sources', 'type': 'any'},
                    {'name': 'age_sources', 'type': 'any'},
                    {'name': 'income_sources', 'type': 'any'},
                    {'name': 'gender_scores', 'type': 'any'},
                    {'name': 'age_scores', 'type': 'any'},
                    {'name': 'income_scores', 'type': 'any'},
                    {'name': 'yandexuid', 'type': 'uint64'},
                ],
            },
            sort_by='yandexuid',
        ),
    )

    yuid_with_all_light = tables.YsonTable(
        file_path='yuid_with_all_light.yson',
        cypress_path=patched_config.YUID_WITH_ALL_BY_YANDEXUID_TABLE,
        on_write=tables.OnWrite(
            attributes={
                'schema': [
                    {'name': 'yandexuid', 'type': 'uint64'},
                    {'name': 'ip_activity_type', 'type': 'string'},
                    {'name': 'main_region', 'type': 'uint64'},
                    {'name': 'main_region_country', 'type': 'uint64'},
                ],
            },
            sort_by='yandexuid',
        ),
    )

    socdem_labels_for_learning = tables.YsonTable(
        file_path='socdem_labels_for_learning.yson',
        cypress_path=task.output()['socdem_labels_for_learning'].table,
        yson_format='pretty',
    )

    socdem_labels = tables.YsonTable(
        file_path='socdem_labels.yson',
        cypress_path=task.output()['socdem_labels'].table,
        yson_format='pretty',
    )

    with mock.patch('crypta.profile.utils.loggers.send_to_graphite', return_value=lambda *args, **kwargs: None):
        return tests.yt_test_func(
            yt_client=local_yt.get_yt_client(),
            func=task.run,
            data_path=yatest.common.test_source_path('data'),
            input_tables=[
                (join_socdem_storage, tests.TableIsNotChanged()),
                (yuid_with_all_light, tests.TableIsNotChanged()),
            ],
            output_tables=[
                (socdem_labels_for_learning, tests.Diff()),
                (socdem_labels, tests.Diff()),
            ],
        )
