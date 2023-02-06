"""
    Testing yasm api wrapper
"""

import pytest
import requests
import inject
import requests_mock

from search.mon.wabbajack.libs.modlib.modules import yasm
from search.mon.wabbajack.libs.modlib.api_wrappers.yasm.yasm_porto import (
    compile_all_tags,
    compile_service_tags,
    metric_urls_all,
    metric_urls_service,
)
from search.mon.wabbajack.libs.config.config_util import ConfigUtilInterface
from search.mon.wabbajack.libs.config.helpers import ConnectionString
from search.mon.wabbajack.libs.utils.mds import CONFIG_ITEM as MDS_CONFIG_ITEM


class TestCfg(object):
    def __init__(self, cfg):
        self.cfg = cfg

    @property
    def modules(self):
        return self.cfg


class TestValidateUrl:
    def setup_class(self):
        self.url = 'https://yasm.yandex-team.ru/panel/thatguy.L7-main-services/4cfbbe82-404b-0bc7-c824-fb125b2aad3b/'

        def configure_injector(binder):
            # binder.bind_to_constructor(Config, Config(config))
            binder.bind(ConfigUtilInterface, TestCfg({
                MDS_CONFIG_ITEM: {
                    'conn_str': str(ConnectionString('http://avatars-int.mdst.yandex.net:13000/put-searchmon')),
                    'read_url': str(ConnectionString('http://avatars.mdst.yandex.net:80/')),
                    'retry_count': 3
                }
            }))

        inject.clear_and_configure(configure_injector)
        self.yasm_api = yasm.YasmScreenShoterApi(self.url)

    def test_return_type(self):
        return_val = self.yasm_api.validate_url(self.url)
        assert isinstance(return_val, bool)
        return_val = self.yasm_api.validate_url('')
        assert isinstance(return_val, bool)

    def test_return_value(self):
        return_val = self.yasm_api.validate_url(self.url)
        assert return_val is True
        return_val = self.yasm_api.validate_url('')
        assert return_val is False

    def tear_down(self):
        pass


class TestRequest:
    def setup_class(self):
        self.response = None
        self.url = 'http://yasm.yandex-team.ru/panel/thatguy.L7-main-services/4cfbbe82-404b-0bc7-c824-fb125b2aad3b/'
        self.url_refub = 'https://s.yasm.yandex-team.ru/panel/thatguy.L7-main-services/4cfbbe82-404b-0bc7-c824-fb125b2aad3b/?width=1920&height=1080&static=true'

        def configure_injector(binder):
            # binder.bind_to_constructor(Config, Config(config))
            binder.bind(ConfigUtilInterface, TestCfg({
                MDS_CONFIG_ITEM: {
                    'conn_str': str(ConnectionString('http://avatars-int.mdst.yandex.net:13000/put-searchmon')),
                    'read_url': str(ConnectionString('http://avatars.mdst.yandex.net:80/')),
                    'retry_count': 3
                }
            }))

        inject.clear_and_configure(configure_injector)
        self.yasm_api = yasm.YasmScreenShoterApi(self.url, storage=yasm.ScreenshotStorageType.YASM)
        with requests_mock.Mocker() as m:
            m.register_uri('GET', self.url_refub, status_code=302)
            self.test_response = self.yasm_api.get_screenshot()

    def test_return_type(self):
        assert isinstance(self.test_response, list)

    def test_return_value(self):
        assert self.test_response == [{'image': 'Failed to get image from yasm', 'name': '', 'url': 'http://yasm.yandex-team.ru/panel/thatguy.L7-main-services/4cfbbe82-404b-0bc7-c824-fb125b2aad3b/'}]

    def tear_down(self):
        pass


# TODO when iss api moves to arc.
class TestCompileServiceTags:
    def setup_class(self):
        pass

    def test_return_type(self):
        pass

    def test_return_value(self):
        pass

    def tear_down(self):
        pass


class TestCompileAllTags:
    def setup_class(self):
        self.emptyprops = {}
        self.props = {'first': {
            'instanceData': {
                'properties/tags': 'firstctype_ctype_hello asd_prj_firstprj',
                },
            },
            'second': {'instanceData': {
                'properties/tags': 'secondctype_ctype_hello asd_prj_secondprj',
                },
                }
            }

    def test_return_type(self):
        return_val = compile_all_tags(self.emptyprops)
        assert return_val is None
        return_val = compile_all_tags(self.props)
        assert isinstance(return_val, dict)

    def test_return_value(self):
        return_val = compile_all_tags(self.emptyprops)
        assert return_val is None
        return_val = compile_all_tags(self.props)
        assert return_val == {'first': {'prj': 'firstprj'}, 'second': {'prj': 'secondprj'}}

    def tear_down(self):
        pass


class TestMetricsUrlsAll:
    def setup_class(self):
        self.tags = {'service#config': {'ctype': str, 'itype': str, 'prj': str}}
        self.host = 'man1-5214@search.yandex.net'

    def test_return_type(self):
        return_val = metric_urls_all(self.tags, self.host)
        assert isinstance(return_val, dict)

    def test_raise_exception(self):
        with pytest.raises(ValueError) as e:
            metric_urls_all({}, '')
            assert isinstance(e, ValueError)

    def test_return_value(self):
        return_val = metric_urls_all(self.tags, self.host)
        assert return_val == {'service#config': "https://yasm.yandex-team.ru/template/panel/Porto-container/hosts=man1-5214@search.yandex.net;itype=<class 'str'>;ctype=<class 'str'>;prj=<class 'str'>"}

    def tear_down(self):
        pass


class TestMetricsUrlsService:
    def setup_class(self):
        self.tags = {'ctype': 'testctype', 'itype': 'testitype', 'prj': 'testprj'}
        self.host = 'man1-5214@search.yandex.net'

    def test_return_type(self):
        return_val = metric_urls_service(self.tags, self.host)
        assert isinstance(return_val, str)

    def test_return_value(self):
        return_val = metric_urls_service(self.tags, self.host)
        assert return_val == 'https://yasm.yandex-team.ru/template/panel/Porto-container/hosts=man1-5214@search.yandex.net;itype=testitype;ctype=testctype;prj=testprj'

        return_val = metric_urls_service({}, '')
        assert return_val is None

    def tear_down(self):
        pass
