# coding: utf-8

from components_app.api.yasm.api import YasmApi
from components_app.configs.base import yasm as yasm_config
from components_app.tests.base import BaseApiTestCase


def get_attr_values(obj):
    return set((v for k, v in obj.__dict__.items() if not k.startswith('__') and not k.endswith('__')))


class TestYasmApi(BaseApiTestCase):
    def __init__(self, methodName='runTest'):
        super(TestYasmApi, self).__init__(methodName=methodName)
        self.api = YasmApi()
        self.api.load_config(yasm_config)

    def test_alert2(self):
        alerts_config = self.api.conf.alerts2()
        self.assertNotEmptyDict(alerts_config)

    def test_meta_alert(self):
        metric = 'gen.ASEARCH.mmeta_prod_web-main_vla_yandsearch.or_hperc_unistat-all_dhhh_0_300_loadlogw_evlog-all-300-ms_perc_'
        info = self.api.meta_alert()
        self.assertNotEmptyDict(info)
        info = self.api.meta_alert(ASEARCH=[metric])
        self.assertNotEmptyDict(info)
        self.assertIn('state', info['ASEARCH'][metric])
        self.assertIn('value', info['ASEARCH'][metric])
        self.assertIn('tags', info['ASEARCH'][metric])
        self.assertEqual(len(info['ASEARCH']), 1)

