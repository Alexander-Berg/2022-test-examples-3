"""
    Test for wabbajack NANNY MODULES
"""
import pytest

from search.mon.wabbajack.libs.modlib.modules import oops
import requests

from . import UNIT_TEST_ARGS, UNIT_TEST_ARGS_2, REAL_TEST_ARGS


def raise_exc(exc=None):
    def _func():
        if exc is not None:
            raise exc()
    return _func


def make_mocked_post(json, status_code, raising_error=None):
    def mocked_post(session, url, data, *args, **kwargs):
        """A method replacing Requests.get
        Returns either a mocked response object (with json method)
        or the default response object if the url doesn't match
        one of those that have been supplied.
        """
        # create a mocked requests object
        mock = type('MockedReq', (), {})()
        # assign mocked json to requests.json
        mock.json = lambda: json
        mock.status_code = 200
        mock.raise_for_status = raise_exc(raising_error)
        # assign obj to mock
        obj = mock
        return obj
    return mocked_post


@pytest.fixture(scope='function')
def success_resps(monkeypatch):
    # finally, patch requests.Session with patched version
    monkeypatch.setattr(requests.Session, 'post', make_mocked_post({}, 200, None))


@pytest.fixture(scope='function')
def failed_resps(monkeypatch):
    # finally, patch requests.Session with patched version
    monkeypatch.setattr(requests.Session, 'post', make_mocked_post({}, 500, requests.exceptions.HTTPError))


@pytest.fixture(scope='function')
def bad_resps(monkeypatch):
    # finally, patch requests.Session with patched version
    monkeypatch.setattr(requests.Session, 'post', make_mocked_post({}, 200, None))


@pytest.mark.parametrize('test_args,expected', UNIT_TEST_ARGS)
def test_unit_oops_hosts_attrs(success_resps, test_args, expected):
    function_result = oops.hosts_attrs(**test_args)
    assert isinstance(function_result, (list, dict))
    assert function_result == expected


@pytest.mark.parametrize('func_name', oops.OopsApiWrapper.known_attrs)
@pytest.mark.parametrize('test_args,expected', UNIT_TEST_ARGS_2)
def test_unit_other_attrs(success_resps, test_args, expected, func_name):
    _func = getattr(oops, func_name)
    function_result = _func(**test_args)
    assert isinstance(function_result, (list, dict))
    assert function_result == expected


def test_unit_oops_hosts_attrs_failures():
    with pytest.raises(TypeError):
        oops.hosts_attrs()
    with pytest.raises(TypeError):
        oops.hosts_attrs('test')
    with pytest.raises(TypeError):
        oops.hosts_attrs('host', 'attr')
    with pytest.raises(TypeError):
        oops.hosts_attrs('host', 'attr', fqdns='test', attr_names='test')
    with pytest.raises(TypeError):
        oops.hosts_attrs(fqdns=1, attr_names=0)
    with pytest.raises(TypeError):
        oops.hosts_attrs(fqdns=['1'], attr_names=[0])
    with pytest.raises(TypeError):
        oops.hosts_attrs(fqdns=[1], attr_names=['0'])
    with pytest.raises(TypeError):
        oops.hosts_attrs(fqdns='', attr_names='')


def test_unit_oops_hosts_attrs_failed_resps(failed_resps):
    with pytest.raises(requests.exceptions.HTTPError):
        oops.hosts_attrs(fqdns='a', attr_names='b')


@pytest.mark.parametrize('func_name', oops.OopsApiWrapper.known_attrs)
def test_unit_oops_other_failures(func_name):
    _func = getattr(oops, func_name)
    with pytest.raises(TypeError):
        _func()
    with pytest.raises(TypeError):
        _func('test')
    with pytest.raises(TypeError):
        _func('host', 'attr', fqdns='test', attr_names='test')
    with pytest.raises(TypeError):
        _func(fqdns=1)
    with pytest.raises(TypeError):
        _func(fqdns=[1])
    with pytest.raises(TypeError):
        _func(fqdns='')
    with pytest.raises(TypeError):
        _func(fqdns=',,,')


@pytest.mark.parametrize('func_name', oops.OopsApiWrapper.known_attrs)
def test_unit_oops_other_failed_resps(failed_resps, func_name):
    _func = getattr(oops, func_name)
    with pytest.raises(requests.exceptions.HTTPError):
        _func(fqdns='a')


@pytest.mark.parametrize('kwargs', REAL_TEST_ARGS)
def test_oops_hosts_attrs(success_resps, kwargs):
    function_result = oops.hosts_attrs(**kwargs)
    assert isinstance(function_result, (list, dict))
