import os

import pytest

import yt.wrapper as yt

from crypta.lab.lib.test.user_data_with_date_pb2 import TUserDataWithDate
from crypta.lab.proto.config_pb2 import TLabConfig
from crypta.lab.proto.sample_pb2 import Sample
from crypta.lab.proto.view_pb2 import TSampleView
from crypta.lib.proto.user_data.user_data_pb2 import TUserData
from crypta.lib.python import (
    crypta_env,
    proto,
    test_utils,
)
import crypta.lib.python.bt.conf.conf as conf
import crypta.lib.python.bt.conf.resource_conf as resource_conf
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import tables


pytest_plugins = [
    'crypta.lib.python.test_utils.user_data_fixture',
    'crypta.lib.python.yql.test_helpers.fixtures',
    'crypta.lookalike.lib.python.test_utils.fixtures',
]


@pytest.fixture(scope="function")
def config(local_yt, local_yt_and_yql_env):
    if conf.CONFIG_SOURCE not in conf.LOCALS:
        conf.use(resource_conf.find('/crypta/lab'))
    proto_config = TLabConfig()
    proto_config.Yt.Token = 'fake'
    proto_config.Yql.Token = 'fake'
    proto_config.Yt.Proxy = local_yt.get_server()

    conf.use_proto(proto_config)

    os.environ[crypta_env.EnvNames.crypta_environment] = 'testing'
    os.environ.update(local_yt_and_yql_env)
    yield conf


class Lab(object):
    @test_utils.api_result(Sample)
    def getSample(self, id):  # noqa
        return {
            'id': id,
            'name': 'Test sample with dates',
            'idName': 'yandexuid',
            'idKey': 'Yandexuid',
            'dateKey': 'Date',
            'state': 'CREATED',
        }

    @test_utils.api_result(TSampleView)
    def getSampleView(self, id, view_id):  # noqa
        if view_id == 'src':
            return {
                'ID': view_id,
                'SampleID': id,
                'Path': yt.ypath_join('//home/crypta/testing/lab/samples', id, view_id),
                'State': 'READY',
                'Type': 'IDENTITY',
                'Error': '',
            }
        return {
            'ID': view_id,
            'SampleID': id,
            'Path': yt.ypath_join('//home/crypta/testing/lab/samples', id, view_id),
            'State': 'READY',
            'Error': '',
            "Options": {
                'Lookalike': {
                    'UseDates': view_id == 'dst_with_dates',
                    'Counts': {
                        'Output': 100,
                    },
                },
            },
            'Type': 'LOOKALIKE',
        }

    @test_utils.api_result(TSampleView)
    def updateSampleViewState(self, id, view_id, state):  # noqa
        if view_id == 'src':
            return {
                'ID': view_id,
                'SampleID': id,
                'Path': yt.ypath_join('//home/crypta/testing/lab/samples', id, view_id),
                'State': state,
                'Type': 'IDENTITY',
                'Error': '',
            }
        return {
            'ID': view_id,
            'SampleID': id,
            'Path': yt.ypath_join('//home/crypta/testing/lab/samples', id, view_id),
            'State': state,
            'Options': {
                'Lookalike': {
                    'UseDates': view_id == 'dst_with_dates',
                    'Counts': {
                        'Output': 100,
                    }
                },
            },
            'Error': '',
            'Type': 'LOOKALIKE',
        }

    @test_utils.api_result(TSampleView)
    def updateSampleState(self, id, state):  # noqa
        return {
            'id': id,
            'name': 'Test sample with dates',
            'idName': 'yandexuid',
            'idKey': 'Yandexuid',
            'dateKey': 'Date',
            'state': state,
        }

    @test_utils.api_result(TSampleView)
    def updateSampleViewError(self, id, view_id, error):  # noqa
        return {
            'ID': view_id,
            'SampleID': id,
            'Path': yt.ypath_join('//home/crypta/testing/lab/samples', id, view_id),
            'State': 'ERROR',
            'Type': 'IDENTITY',
            'Error': error,
        }


class ApiClient(object):
    def __init__(self):
        self.lab = Lab()


@pytest.fixture(scope="function")
def api_client_mock(mocker):
    mocked_api = mocker.patch(
        'crypta.lab.lib.common.WithApi.api',
        new_callable=mocker.PropertyMock,
        return_value=ApiClient(),
    )

    return mocked_api


@pytest.fixture(scope='function')
def dated_user_data_table(clean_local_yt, config):
    def get_user_data_by_date(file_path, id_type='yuid', date=None, addtitional_attributes=None):
        if date is not None:
            proto_type = TUserDataWithDate
            if id_type == 'CryptaID':
                cypress_path = os.path.join(config.paths.lab.data.crypta_id.userdata_daily, date)
            else:
                cypress_path = os.path.join(config.paths.lab.data.userdata_daily, date)
        else:
            proto_type = TUserData
            if id_type == 'CryptaID':
                cypress_path = config.paths.lab.data.crypta_id.userdata
            else:
                cypress_path = config.paths.lab.data.userdata

        attributes = {'schema': schema_utils.get_schema_from_proto(message_type=proto_type, key_columns=[id_type])}
        if addtitional_attributes is not None:
            attributes.update(addtitional_attributes)

        return tables.YsonTable(
            file_path=file_path,
            cypress_path=cypress_path,
            on_write=tables.OnWrite(
                attributes=attributes,
                row_transformer=proto.row_transformer(proto_type),
            ),
        )

    return get_user_data_by_date
