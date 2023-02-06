import logging

import yatest.common

from crypta.lab.lib import tables as lab_tables
from crypta.lab.lib.mixed_userdata import PrepareMixedUserData
from crypta.lib.python.bt import test_helpers
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)

logger = logging.getLogger(__name__)


def test_mix_userdata(clean_local_yt, config, dated_user_data_table):
    return tests.yt_test_func(
        yt_client=clean_local_yt.get_yt_client(),
        func=lambda: test_helpers.execute(PrepareMixedUserData(day='2021-12-07')),
        data_path=yatest.common.test_source_path('data/test_mix_userdata'),
        input_tables=[
            (
                dated_user_data_table(
                    file_path='user_data_by_yandexuid.yson',
                    addtitional_attributes={lab_tables.UserData.Attributes.LAST_UPDATE_DATE: '2021-12-07'},
                ),
                tests.TableIsNotChanged(),
            ),
            (
                dated_user_data_table(
                    file_path='user_data_by_cryptaid.yson',
                    id_type='CryptaID',
                    addtitional_attributes={lab_tables.UserData.Attributes.LAST_UPDATE_DATE: '2021-12-07'},
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    'mixed_userdata.yson',
                    config.paths.lab.data.mixed.userdata,
                    yson_format='pretty',
                ),
                [tests.Diff()],
            ),
        ],
    )
