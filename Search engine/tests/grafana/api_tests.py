"""
    Tests for Grafana API
"""
import requests_mock
import inject
from urllib import parse as urlparse
from . import EXAMPLE_DASHBOARD_RESPONSE, EXAMPLE_SNAPSHOT_RESPONSE
from search.mon.wabbajack.libs.config.config_util import ConfigUtilInterface
from search.mon.wabbajack.libs.config.helpers import ConnectionString

from search.mon.wabbajack.libs.modlib.api_wrappers.grafana import (
    GrafanaDashboardRenderer,
    GrafanaSnapshotRenderer,
    CONFIG_ITEM
)

from search.mon.wabbajack.libs.utils.mds import CONFIG_ITEM as MDS_CONFIG_ITEM


class TestCfg(object):
    def __init__(self, cfg):
        self.cfg = cfg

    @property
    def modules(self):
        return self.cfg


class TestGrafanaDashboardRenderer:
    """
        test suite for iss api wrapper
    """

    def setup_class(self):
        self.renderer = None
        self.oauth = 'oauth_token'
        self.dashboard_id = '000016640'
        self.slug = 'strm-rtmp-stream-quality'
        self.orgid = 2
        self.__GRAFANA_URL__ = str(ConnectionString('http://grafana.yandex-team.ru')).rstrip('/')

        def configure_injector(binder):
            # binder.bind_to_constructor(Config, Config(config))
            binder.bind(ConfigUtilInterface, TestCfg({
                CONFIG_ITEM: self.__GRAFANA_URL__,
                MDS_CONFIG_ITEM: {
                    'conn_str': str(ConnectionString('http://avatars-int.mdst.yandex.net:13000/put-searchmon')),
                    'read_url': str(ConnectionString('http://avatars.mdst.yandex.net:80/')),
                    'retry_count': 3
                }
            }))

        inject.clear_and_configure(configure_injector)

        with requests_mock.Mocker() as m:
            endpoint = f'{self.__GRAFANA_URL__}/{GrafanaDashboardRenderer.__main_path__}'
            m.register_uri(
                'GET',
                f'{endpoint}/{self.dashboard_id}',
                json=EXAMPLE_DASHBOARD_RESPONSE,
                status_code=200
            )
            self.renderer = GrafanaDashboardRenderer(
                dashboard_id=self.dashboard_id,
                orgid=self.orgid,
                oauth=self.oauth,
                variables={'test_var1': 'test_val1', 'test_var2': 'test_val2'}
            )

    def test_init(self):
        assert self.renderer._slug == EXAMPLE_DASHBOARD_RESPONSE['meta']['slug']
        assert self.renderer._conf == EXAMPLE_DASHBOARD_RESPONSE

    def test_read_panels(self):
        params = {
            'time_from': 0,
            'time_to': 1,
            'width': 100,
            'height': 200,
            'tz': 'Europe/Moscow',
        }
        panels_list = self.renderer.render_panels(**params)
        params['tz'] = 'Europe%2FMoscow'
        assert panels_list == [
            {2: ''.join([
                f'{self.__GRAFANA_URL__}/render/d-solo/{self.dashboard_id}/{self.slug}/?',
                f'orgId={self.orgid}&from={params["time_from"]}&to={params["time_to"]}',
                f'&width={params["width"]}&height={params["height"]}&tz={params["tz"]}',
                f'&var-test_var1=test_val1&var-test_var2=test_val2',
                f'&panelId=2'
            ])},
            {43: ''.join([
                f'{self.__GRAFANA_URL__}/render/d-solo/{self.dashboard_id}/{self.slug}/?',
                f'orgId={self.orgid}&from={params["time_from"]}&to={params["time_to"]}',
                f'&width={params["width"]}&height={params["height"]}&tz={params["tz"]}',
                f'&var-test_var1=test_val1&var-test_var2=test_val2',
                f'&panelId=43'
            ])}
        ]


class TestGrafanaSnapshotRenderer:
    """
        test suite for iss api wrapper
    """

    def setup_class(self):
        self.renderer = None
        self.oauth = 'oauth_token'
        self.snapshot_id = 'KTjBYfmBrb0ojuk2uOq5PQ6cOJKLXLvP'
        self.orgid = 2
        self.__GRAFANA_URL__ = str(ConnectionString('http://grafana.yandex-team.ru')).rstrip('/')

        def configure_injector(binder):
            # binder.bind_to_constructor(Config, Config(config))
            binder.bind(ConfigUtilInterface, TestCfg({
                CONFIG_ITEM: self.__GRAFANA_URL__,
                MDS_CONFIG_ITEM: {
                    'conn_str': str(ConnectionString('http://avatars-int.mdst.yandex.net:13000/put-searchmon')),
                    'read_url': str(ConnectionString('http://avatars.mdst.yandex.net:80/')),
                    'retry_count': 3
                }}))

        inject.clear_and_configure(configure_injector)

        with requests_mock.Mocker() as m:
            endpoint = f'{self.__GRAFANA_URL__}/{GrafanaSnapshotRenderer.__main_path__}'
            m.register_uri(
                'GET',
                f'{endpoint}/{self.snapshot_id}',
                json=EXAMPLE_SNAPSHOT_RESPONSE,
                status_code=200
            )
            self.renderer = GrafanaSnapshotRenderer(snapshot_id=self.snapshot_id, orgid=self.orgid, oauth=self.oauth)

    def test_init(self):
        assert self.renderer._variables == {
            'channel_a': 'raztv_supres_source',
            'channel_b': 'raztv_supres',
            'environment': 'stable',
            'publisher_a': 'src-rtmp-mskm903_strm_yandex_net',
            'publisher_b': 'nnmetr-src-rtmp-mskm903_strm_yandex_net'
        }

        assert self.renderer._times == {'from': 'now-24h', 'to': 'now'}
        assert self.renderer._conf == EXAMPLE_SNAPSHOT_RESPONSE

    def test_read_panels(self):
        params = {
            'time_from': 0,
            'time_to': 1,
            'width': 100,
            'height': 200,
            'tz': 'Europe/Moscow',
        }
        panels_list = self.renderer.render_panels(**params)
        params['tz'] = 'Europe%2FMoscow'
        assert panels_list == [
            {2: ''.join([
                f'{self.__GRAFANA_URL__}/render/dashboard-solo/snapshot/{self.snapshot_id}/?',
                f'orgId={self.orgid}&from={params["time_from"]}&to={params["time_to"]}',
                f'&width={params["width"]}&height={params["height"]}&tz={params["tz"]}&',
                urlparse.urlencode(self.renderer._vars_to_get_params),
                f'&panelId=2'
            ])},
            {43: ''.join([
                f'{self.__GRAFANA_URL__}/render/dashboard-solo/snapshot/{self.snapshot_id}/?',
                f'orgId={self.orgid}&from={params["time_from"]}&to={params["time_to"]}',
                f'&width={params["width"]}&height={params["height"]}&tz={params["tz"]}&',
                urlparse.urlencode(self.renderer._vars_to_get_params),
                f'&panelId=43'
            ])}
        ]
