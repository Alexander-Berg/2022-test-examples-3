import sys
sys.path.append('../python')
import json
import luigi
from lib.luigi import base_luigi_task
import yt.local as yt_local
import yt.wrapper as yt
from utils import utils
from utils import mr_utils as mr
import pytest

from rtcconf import config
from data_imports.import_logs.graph_passport_sensitive import ImportPassportPhoneBindingsDayTask


YT_DIR = 'yt-data'
YT_ID = 'yt-graph-test-import-passport-phone-bindings-log'
PROXY_PORT = 9014


def get_node_config():
    import os
    script_dir, _script_name = os.path.split(__file__)
    return os.path.join(script_dir, 'node_config.yson')


@pytest.mark.usefixtures("ytlocal")
class TestParsePassportSensitivePhones(object):
    def __prepare_cypress(self, date):
        out_date_folder = config.YT_OUTPUT_FOLDER + date + '/'
        mr.mkdir(config.YT_OUTPUT_FOLDER)
        mr.mkdir(out_date_folder)
        mr.mkdir(config.LOG_FOLDERS['passport_sensitive'])

    def __load_data(self, date):
        passport_sensitive_log = config.LOG_FOLDERS['passport_sensitive'] + date
        yt.create_table(passport_sensitive_log)
        data = [{"consumer":"passport","uid":"111111111","entity":"phones.secure","event":"account_modification","new_entity_id":"167777777","new":"+380977589974"},
                {"consumer":"passport","uid":"111111111","attribute":"number","entity":"phone","event":"account_modification","entity_id":"511111111","new":"+380977589974"},
                {"consumer":"passport","uid":"111111111","attribute":"secured","entity":"phone","event":"account_modification","entity_id":"511111111","new":"{0} 23:26:20".format(date)},
                {"consumer":"passport","uid":"222222222","attribute":"number","entity":"phone","event":"account_modification","entity_id":"522222222","new":"+79814638803"},
                {"consumer":"passport","uid":"222222222","attribute":"confirmed","entity":"phone","event":"account_modification","entity_id":"522222222","new":"{0} 23:26:23".format(date)},
                {"consumer":"passport","uid":"222222222","attribute":"bound","entity":"phone","event":"account_modification","entity_id":"522222222","new":"{0} 23:26:23".format(date)},]
        yt.write_table(passport_sensitive_log, [json.dumps(row) for row in data], format='json', raw=True)

    def __assert_task_result(self, date):
        result_table = config.YT_OUTPUT_FOLDER + date + '/yuid_raw/puid_with_' + config.ID_TYPE_PHONE + '_' + config.ID_SOURCE_TYPE_PASSPORT_SENSITIVE
        assert yt.exists(result_table)
        rows = [json.loads(row) for row in list(yt.read_table(result_table, 'json', raw=True))]
        assert len(rows) == 1
        puid_with_phone = rows[0]
        id_prefix, id_hash = utils.prepare_phone_md5('+79814638803')
        assert puid_with_phone['puid'] == '222222222'
        assert puid_with_phone['id_value'] == id_hash
        assert puid_with_phone['id_prefix'] == id_prefix
        assert puid_with_phone['id_type'] == 'phone'
        assert puid_with_phone['source_type'] == 'passport_sensitive'
        assert puid_with_phone['id_date'] == date

    def test_parse_phones(self):
        date = '2016-04-03'
        config.MR_SERVER = yt_local.get_proxy(YT_ID, path=YT_DIR)
        self.__prepare_cypress(date)
        self.__load_data(date)
        luigi.run(['ImportPassportPhoneBindingsDayTask', '--date', date, '--run-date', date, '--workers', '1', '--local-scheduler', '--no-lock'])
        self.__assert_task_result(date)
