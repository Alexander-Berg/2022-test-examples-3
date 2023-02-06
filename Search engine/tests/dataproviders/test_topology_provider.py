# -*- coding: utf-8 -*-
import os
import sys
import copy
import json
import logging.config
from StringIO import StringIO
from contextlib import contextmanager

import pytest
import requests_mock

from rtcc import settings
from rtcc.core.errors import ResolverException
from rtcc.core.errors import ResolvesToEmpty
from rtcc.core.errors import ResolvesToEmptyMultiresolve
from rtcc.core.session import Session
from rtcc.dataprovider.searchconfigs.base import SearchConfigBuilder
from rtcc.dataprovider.searchconfigs.types.sources import Source
from rtcc.dataprovider.topology.provider import TopologyProvider
from rtcc.dataprovider.topology.provider import get_resolver
from rtcc.dataprovider.topology.base import BaseResolver
from rtcc.dataprovider.topology.user import UserInstanceResolver
from rtcc.dataprovider.topology.runtime import RuntimeInstanceResolver
from rtcc.dataprovider.topology.online import OnlineInstanceResolver
from rtcc.model import raw
from rtcc.utils.contextmanagers import patch_envvar


@pytest.fixture
def clusterstate_response(monkeypatch):
    monkeypatch.setattr('rtcc.settings.RUNTIME_RESOLVER_INTERVAL_BETWEEN_ATTEMPTS', 0.1)

    return {
        'instances': [
            {'host': 'man1-6171.search.yandex.net', 'port': 14350, 'extra': {'shard': 'none'}},
            {'host': 'man1-5720.search.yandex.net', 'port': 14350, 'extra': {'shard': 'none'}}],
        'meta': {
            'timestamp': 1500457761,
            'backend': {
                'host': 'vla1-2450.search.yandex.net', 'port': '24700'},
            'versions': {'TEST': {'version': 101000004}}}
    }


@pytest.fixture
def empty_clusterstate_response(clusterstate_response):
    response = copy.deepcopy(clusterstate_response)
    response['instances'] = []
    return response


@pytest.fixture
def setup_logging():
    def _inner():
        log_settings = settings.LOGGING_SETTINGS
        log_settings['handlers']['resolvers'] = {
            "level": "DEBUG",
            "formatter": "file",
            "class": "logging.StreamHandler",
            "stream": StringIO()
        }
        log_settings['handlers']['console']['stream'] = StringIO()

        logging.config.dictConfig(log_settings)

    return _inner


@pytest.fixture(scope='class')
def mock_external_calls(request):

    @contextmanager
    def _mock_external_calls(_, url, response, method='get', status_code=None):
        with requests_mock.Mocker() as mock:
            kwargs = dict()
            kwargs['text' if callable(response) else 'json'] = response
            if status_code is not None:
                kwargs['status_code'] = status_code
            getattr(mock, method)(url, **kwargs)
            yield

    if request.cls is not None:
        request.cls.mock_external_calls = _mock_external_calls

    return _mock_external_calls


class TestTopologyProvider(object):

    @pytest.fixture(params=[('online', OnlineInstanceResolver),
                            ('user', UserInstanceResolver),
                            ('resolver', RuntimeInstanceResolver)])
    def resolver_to_resolve(self, request):
        return request.param

    @pytest.fixture
    def provider_and_kwargs(self, monkeypatch, tmpdir):
        def raiser(*_, **__):
            raise ResolverException

        monkeypatch.setattr('rtcc.dataprovider.topology.provider.get_resolver',
                            lambda t: type('_U', (object,), {'resolve': raiser})())

        session = Session(path=str(tmpdir))
        provider = TopologyProvider(session=session)
        kwargs = dict(type='user', expression='Foo', args=tuple())
        provider.get(**kwargs)
        return provider, kwargs

    def test_get_resolver(self, resolver_to_resolve):
        key, clz = resolver_to_resolve
        assert isinstance(get_resolver(key), clz)

    def test_not_write_to_session_when_setting_set(self, monkeypatch, provider_and_kwargs):
        provider, kwargs = provider_and_kwargs
        monkeypatch.setattr('rtcc.settings.DO_NOT_STORE_EMPTY_RESOLVINGS_AT_SESSION', True)
        assert not provider._table.has(**kwargs)

    def test_write_to_session_if_var_set(self, monkeypatch, provider_and_kwargs):
        provider, kwargs = provider_and_kwargs
        monkeypatch.setattr('rtcc.settings.DO_NOT_STORE_EMPTY_RESOLVINGS_AT_SESSION', False)
        provider.get(**kwargs)
        assert provider._table.has(**kwargs)


class TestBaseResolver(object):

    @pytest.fixture
    def base_descendant(self):

        def raise_if_100(_, arg, exc_type):
            if arg == 100:
                raise exc_type
            else:
                return [arg]

        return type('_Resolver',
                    (BaseResolver,),
                    {
                        '_prepare_single_resolve_arguments': lambda *args, **_: args[1:],
                        '_resolve_single': raise_if_100
                    })

    def test_stderr_write(self, base_descendant, monkeypatch, setup_logging):

        def assert_exception_and_flush(stream):
            assert ("Exception occured during resolving" in stream.getvalue())
            stream.flush()

        monkeypatch.setattr('sys.stderr', StringIO())
        setup_logging()
        with patch_envvar(settings.WRITE_RESOLVER_ERROR_ENVVAR, '1'):
            resolver = base_descendant()

            with pytest.raises(ResolvesToEmpty):
                resolver.resolve(100)
            assert_exception_and_flush(sys.stderr)

            with pytest.raises(ResolvesToEmptyMultiresolve):
                resolver.resolve(100, 200)
            assert_exception_and_flush(sys.stderr)


class TestOnlineResolver(object):

    @pytest.fixture
    def online_resolver(self, monkeypatch):
        def mock_resolve(this, expression):
            if this.raise_unconditionally:
                raise ResolverException(expression)
            return [raw.Instance(expression, '8080')]

        fake_resolver = type('FakeResolver', (RuntimeInstanceResolver,), {
            'resolve': mock_resolve,
            'raise_unconditionally': False
        })

        monkeypatch.setattr('rtcc.dataprovider.topology.online.RuntimeInstanceResolver', fake_resolver)
        return OnlineInstanceResolver()

    def test_arguments_preparing(self, online_resolver):
        args = ('a', 'b', 'c')
        assert tuple(online_resolver._prepare_single_resolve_arguments(*args)) == args

    def test_successful_resolve(self, online_resolver):
        args = ('_ga', '_gb', '_gc')
        instances = online_resolver.resolve(*args)
        for instance in instances:
            assert isinstance(instance, raw.Instance)
            assert instance.host.startswith('C@ONLINE')
            assert any(map(lambda a: a in instance.host, args))

    def test_failed_resolve_single_group(self, online_resolver):
        online_resolver.resolver.raise_unconditionally = True
        with pytest.raises(ResolvesToEmpty):
            online_resolver.resolve('a')

    def test_failed_resolve_multi_group(self, online_resolver):
        online_resolver.resolver.raise_unconditionally = True
        with pytest.raises(ResolvesToEmptyMultiresolve):
            online_resolver.resolve('a', 'b')


@pytest.mark.usefixtures('mock_external_calls')
class TestRuntimeResolver(object):

    @pytest.fixture
    def runtime_resolver(self):
        return RuntimeInstanceResolver()

    @pytest.fixture
    def clusterstate_response(self, monkeypatch):
        monkeypatch.setattr('rtcc.settings.RUNTIME_RESOLVER_INTERVAL_BETWEEN_ATTEMPTS', 0.1)

        return {
            'instances': [
                {'host': 'man1-6171.search.yandex.net', 'port': 14350, 'extra': {'shard': 'none'}},
                {'host': 'man1-5720.search.yandex.net', 'port': 14350, 'extra': {'shard': 'none'}}],
            'meta': {
                'timestamp': 1500457761,
                'backend': {
                    'host': 'vla1-2450.search.yandex.net', 'port': '24700'},
                'versions': {'TEST': {'version': 101000004}}}
        }

    @pytest.fixture
    def empty_clusterstate_response(self, clusterstate_response):
        clusterstate_response['instances'] = []
        return clusterstate_response

    @pytest.fixture
    def except_penultimate_500(self, clusterstate_response):
        counter = {'cnt': settings.RUNTIME_RESOLVER_MAX_ATTEMPTS}

        def _resp_creator(_, context, counter=counter):
            counter['cnt'] -= 1
            if counter['cnt'] == 0:
                context.status_code = 200
                return json.dumps(clusterstate_response)
            else:
                context.status_code = 500
                return ''

        return _resp_creator

    def test_successful_resolve(self, runtime_resolver, clusterstate_response,
                                except_penultimate_500, monkeypatch):
        with self.mock_external_calls(settings.RUNTIME_RESOLVER_URL, clusterstate_response):
            instances = runtime_resolver.resolve('TEST')
            assert len(instances) == 2
            assert all(map(lambda i: isinstance(i, raw.Instance), instances))

        with self.mock_external_calls(settings.RUNTIME_RESOLVER_URL, except_penultimate_500):
            log_out = []
            monkeypatch.setattr('rtcc.dataprovider.topology.runtime.RuntimeInstanceResolver._logger',
                                type('_logger', (object,), {
                                    'info': staticmethod(lambda *info: log_out.append(info))
                                }))
            instances = runtime_resolver.resolve('TEST_2')
            assert (
                len(filter(lambda info: info[0].startswith('Clusterstate resolving for'), log_out)) ==
                settings.RUNTIME_RESOLVER_MAX_ATTEMPTS - 1
            )
            assert len(instances) == 2
            assert all(map(lambda i: isinstance(i, raw.Instance), instances))

    def test_failed_resolve(self, runtime_resolver, empty_clusterstate_response):
        with self.mock_external_calls(settings.RUNTIME_RESOLVER_URL, {}, status_code=500):
            with pytest.raises(ResolverException):
                runtime_resolver.resolve('TEST_3')

        with self.mock_external_calls(settings.RUNTIME_RESOLVER_URL, empty_clusterstate_response):
            with pytest.raises(ResolverException):
                runtime_resolver.resolve('TEST_4')


class TestUserResolver(object):

    @pytest.fixture
    def user_resolver(self):
        return UserInstanceResolver()

    def test_arguments_preparing(self, user_resolver):
        args = ('yandex.ru', 80, 'google.com', 80)
        args = list(user_resolver._prepare_single_resolve_arguments(*args))
        assert args == [('yandex.ru', 80), ('google.com', 80)]

    def test_successful_resolve(self, user_resolver):
        res = user_resolver.resolve('yandex.ru', 80)[0]
        assert isinstance(res, raw.Instance) and res.host == 'yandex.ru' and res.port == 80


@pytest.mark.usefixtures('mock_external_calls')
class TestResolveSeveralGroupsForOneSource(object):

    GROUP_SHOULD_FAIL = 'ALL_ERROR'

    @pytest.fixture
    def external_resolvers(self):
        return OnlineInstanceResolver, RuntimeInstanceResolver

    @pytest.fixture
    def respond_to_resolve_request(self, clusterstate_response, empty_clusterstate_response):

        def _response_maker(request, _):
            group = request.query.split('=')[-1]
            if group.endswith(self.GROUP_SHOULD_FAIL.lower()):
                return json.dumps(empty_clusterstate_response)
            return json.dumps(clusterstate_response)

        return _response_maker

    @pytest.fixture
    def args_from_raw_noapache_dat(self):
        raw = open(os.path.join(os.path.dirname(__file__), 'test_data', 'noapache_data.dat')).readlines()
        raw = [ln.split('\t')[1] for ln in raw]
        parsed_raw = Source.read_from_lines(raw, 'LINE')[0]
        sources = map(SearchConfigBuilder.build_source, parsed_raw)
        return filter(lambda source: source.name == 'NEWSP_REALTIME', sources)

    def test_correct_data_parsing(self, args_from_raw_noapache_dat):
        assert args_from_raw_noapache_dat
        for source in args_from_raw_noapache_dat:
            expression = source.endpoint.service.expression,
            args = tuple(source.endpoint.service.args)
            assert len(expression) and len(args)

    def with_fail_on_every(self, resolver, resp_maker):
        with self.mock_external_calls(settings.RUNTIME_RESOLVER_URL, resp_maker):
            with pytest.raises(ResolvesToEmptyMultiresolve):
                resolver().resolve('ALL_FOO', self.GROUP_SHOULD_FAIL)

    def total_success(self, resolver, resp_maker):
        with self.mock_external_calls(settings.RUNTIME_RESOLVER_URL, resp_maker):
            result = resolver().resolve('ALL_FOO', 'ALL_BAR', 'ALL_BAZ')
            assert len(result) == 6

    def test_external_resolvers(self, external_resolvers, respond_to_resolve_request):
        for resolver in external_resolvers:
            self.with_fail_on_every(resolver, respond_to_resolve_request)
            self.total_success(resolver, respond_to_resolve_request)
