import pytest

from mail.payments.payments.api.middlewares import tvm_check_func


@pytest.mark.usefixtures('loop')
class TestTvmCheckFunc:
    @pytest.fixture(autouse=True)
    def setup(self, payments_settings, src):
        payments_settings.TVM_CHECK_SERVICE_TICKET = True
        payments_settings.TVM_ALLOWED_CLIENTS = (src,)

    @pytest.fixture
    def route_name(self, rands):
        return rands()

    @pytest.fixture(params=(
        pytest.param(True, id='open'),
        pytest.param(False, id='noopen'),
    ), autouse=True)
    def route_is_open(self, payments_settings, route_name, request):
        payments_settings.TVM_OPEN_PATHS = (route_name,) if request.param else ()
        return request.param

    @pytest.fixture
    def acl_name(self, rands):
        return rands()

    @pytest.fixture
    def src(self, randn):
        return randn()

    @pytest.fixture(params=(True, False))
    def valid(self, request):
        return request.param

    @pytest.fixture
    def web_request(self, mocker, noop, route_name):
        web_request = mocker.Mock()
        web_request.match_info.route.name = route_name
        web_request.headers = {}
        web_request.__setitem__ = noop
        return web_request

    @pytest.fixture
    def check_result(self, mocker, src, valid):
        check_result = mocker.Mock()
        check_result.src = src
        check_result.valid = valid

        return check_result

    @pytest.fixture
    def returned_func(self, web_request, check_result):
        def _inner():
            return tvm_check_func(web_request, check_result)

        return _inner

    @pytest.fixture
    def returned(self, returned_func):
        return returned_func()

    def test_valid(self, returned, route_is_open, valid):
        assert returned == (valid or route_is_open)

    def test_absent_in_allowed(self, route_is_open, payments_settings, returned_func):
        payments_settings.TVM_ALLOWED_CLIENTS = ()
        assert returned_func() is route_is_open

    def test_disable_check(self, payments_settings, route_is_open, route_name, returned_func):
        payments_settings.TVM_CHECK_SERVICE_TICKET = False
        assert returned_func() is True

    def test_deny_route_acl(self, payments_settings, route_is_open, route_name, acl_name, returned_func):
        payments_settings.TVM_ROUTES_ACLS[route_name] = [acl_name]
        assert returned_func() is route_is_open

    def test_allow_route_acl_default(self, route_is_open, payments_settings, valid, route_name, acl_name,
                                     returned_func):
        payments_settings.TVM_ROUTES_ACLS[route_name] = ['common']
        assert returned_func() is (valid or route_is_open)

    def test_allow_route_acl(self, src, valid, route_is_open, payments_settings, acl_name, route_name, returned_func):
        payments_settings.TVM_ALLOWED_CLIENTS = ((src, {'acl': [acl_name]}),)
        payments_settings.TVM_ROUTES_ACLS[route_name] = [acl_name]
        assert returned_func() is (valid or route_is_open)
