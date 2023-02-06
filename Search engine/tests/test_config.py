# -*- coding: utf-8 -*-

import pytest
from argparse import Namespace  # convenient for on-fly class with some attributes creation
from contextlib import contextmanager

from rtcc.core.config import Config
from rtcc.core.errors import EmptyBackendsList
from rtcc.model.endpoint import Endpoint

from extensions.configurations.noapache import NoapacheConfig
from extensions.configurations.sfront import RequestConfig


@pytest.fixture(scope='function')
def sources():
    return [
        Namespace(name='foo', endpoint=Namespace(grouping='none', service=Namespace(expression='ALL_FOO'))),
        Namespace(name='bar', endpoint=Namespace(grouping='none', service=Namespace(expression='ALL_BAR'))),
        Namespace(name='baz', endpoint=Namespace(grouping='none', service=Namespace(expression='ALL_BAZ')))
    ]


class BaseConfigTest(object):
    @staticmethod
    def check_raises_emptybackends(expr):
        with pytest.raises(EmptyBackendsList) as e:
            expr()
        errors = e.value.empty_backends_errors
        assert len(errors) == 2
        assert 'source bar has no backends (expression: ALL_BAR)' in errors


class TestBasicConfig(BaseConfigTest):

    @pytest.fixture
    def config(self):
        return Config

    def test_detecting_and_reporting_no_errors(self, config, sources):
        Config.test_and_report_source_errors(sources, lambda _: True)

    def test_detecting_and_reporting_with_errors(self, config, sources):
        self.check_raises_emptybackends(
            lambda: Config.test_and_report_source_errors(sources, lambda s: s.name in ['foo']))


class TestExtensionsConfigs(BaseConfigTest):

    @pytest.fixture
    def patched_sfront(self, monkeypatch, sources):
        def mock_instances(me):
            return [1, 2, 3] if me._Endpoint__raw.service.expression == 'ALL_FOO' else []

        new_type_dict = {
            'instances': property(mock_instances)
        }
        monkeypatch.setattr(
            'extensions.configurations.sfront.Endpoint',
            type('_Endpoint', (Endpoint,), new_type_dict)
        )
        old_confdata = RequestConfig.get_confdata
        RequestConfig.get_confdata = lambda _: Namespace(sources=sources)
        yield RequestConfig('some_id')
        RequestConfig.get_confdata = old_confdata

    @pytest.fixture
    def patched_noapache(self, sources):
        @contextmanager
        def inner(raise_for_all=False):
            patched_noapache = NoapacheConfig
            old_cgiprefix = NoapacheConfig.cgisearchprefix
            old_get_confdata = NoapacheConfig.get_confdata
            patched_noapache.cgisearchprefix = Namespace(get=lambda endpoint=None: not raise_for_all)
            patched_noapache.get_confdata = lambda _: Namespace(collection=Namespace(sources=sources[:2]))
            yield patched_noapache('some_id')
            NoapacheConfig.cgisearchprefix = old_cgiprefix
            NoapacheConfig.get_confdata = old_get_confdata
        return inner

    def test_sfront_config_no_errors(self, sources, patched_sfront):
        for source in sources:
            source.endpoint.service.expression = 'ALL_FOO'
        patched_sfront.test_sources(patched_sfront)

    def test_sfront_config_with_errors(self, patched_sfront):
        self.check_raises_emptybackends(lambda: patched_sfront.test_sources(patched_sfront))

    def test_noapache_config_no_errors(self, patched_noapache):
        with patched_noapache() as config:
            config.test_sources(config)

    def test_noapache_config_with_errors(self, patched_noapache):
        with patched_noapache(raise_for_all=True) as config:
            self.check_raises_emptybackends(lambda: config.test_sources(config))
