
from library.python.testing.pyremock.lib.pyremock import MockResponse
from mail.notsolitesrv.tests.integration.lib.expectation.blackbox.request import make_request
from mail.notsolitesrv.tests.integration.lib.expectation.blackbox.response import (
    make_success_body_response,
    make_server_error_body_response
)
from mail.notsolitesrv.tests.integration.lib.util.user import User


class Blackbox:
    @staticmethod
    def expect_call_success(context, user: User):
        request = make_request(user=user)
        response_body = make_success_body_response(user)
        response = MockResponse(status=200, body=response_body)
        context.pyremock.expect(request=request, response=response)

    @staticmethod
    def expect_call_server_error(context, user: User):
        request = make_request(user=user)
        response_body = make_server_error_body_response()
        response = MockResponse(status=500, body=response_body)
        context.pyremock.expect(request=request, response=response)
