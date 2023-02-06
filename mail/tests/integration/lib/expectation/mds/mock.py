
from library.python.testing.pyremock.lib.pyremock import MockResponse
from mail.notsolitesrv.tests.integration.lib.expectation.mds.request import make_get_request
from mail.notsolitesrv.tests.integration.lib.expectation.mds.request import make_put_request
from mail.notsolitesrv.tests.integration.lib.expectation.mds.response import (
    make_success_get_response_body,
    make_success_put_response_body,
    make_server_error_put_response_body
)
from mail.notsolitesrv.tests.integration.lib.util.message import make_stid_prefix, make_stid
from mail.notsolitesrv.tests.integration.lib.util.user import User


class Mds:
    @staticmethod
    def expect_get_call_success(context, stid, message):
        request = make_get_request(stid=stid)
        response_body = make_success_get_response_body(message=message)
        response = MockResponse(status=200, body=response_body)
        context.pyremock.expect(request=request, response=response)

    @staticmethod
    def expect_put_call_success(
            context,
            user: User,
            unit_type="ham",
            ns=b"mail",
            stid_prefix=None,
            expected_existing_headers: list = None,
            expected_not_existing_headers: list = None,
            expected_equal_headers: dict = None
    ):
        if stid_prefix is None:
            stid_prefix = make_stid_prefix(user.uid, user.is_shared_stid)
        request = make_put_request(
            stid_prefix=stid_prefix,
            unit_type=unit_type,
            ns=ns,
            expected_existing_headers=expected_existing_headers,
            expected_not_existing_headers=expected_not_existing_headers,
            expected_equal_headers=expected_equal_headers)
        user.stid = make_stid(stid_prefix)
        response_body = make_success_put_response_body(stid=user.stid)
        response = MockResponse(status=200, body=response_body)
        context.pyremock.expect(request=request, response=response)

    @staticmethod
    def expect_put_call_server_error(context, user: User):
        stid_prefix = make_stid_prefix(user.uid, user.is_shared_stid)
        request = make_put_request(stid_prefix=stid_prefix)
        response_body = make_server_error_put_response_body()
        response = MockResponse(status=500, body=response_body)
        context.pyremock.expect(request=request, response=response)
