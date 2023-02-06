from library.python.testing.pyremock.lib.pyremock import MockResponse

from mail.notsolitesrv.tests.integration.lib.expectation.msettings.request import (
    make_profile_request,
    make_request
)
from mail.notsolitesrv.tests.integration.lib.expectation.msettings.response import (
    make_success_body_response,
    make_server_error_body_response
)


class MSettings:
    @staticmethod
    def expect_call_success(context, uid, expected_settings={}):
        request = make_request(uid=uid)
        response_body = make_success_body_response(settings=expected_settings)
        response = MockResponse(status=200, body=response_body)
        context.pyremock.expect(request=request, response=response)

    @staticmethod
    def expect_profile_call_success(context, uid, expected_settings={}):
        request = make_profile_request(uid=uid)
        response_body = make_success_body_response(settings=expected_settings)
        response = MockResponse(status=200, body=response_body)
        context.pyremock.expect(request=request, response=response)

    @staticmethod
    def expect_call_server_error(context, uid, times=1):
        request = make_request(uid=uid)
        response_body = make_server_error_body_response()
        response = MockResponse(status=500, body=response_body)
        context.pyremock.expect(request=request, response=response, times=times)
