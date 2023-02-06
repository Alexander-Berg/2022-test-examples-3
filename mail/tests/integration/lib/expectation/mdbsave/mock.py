from hamcrest import anything
from library.python.testing.pyremock.lib.pyremock import MockResponse
from mail.notsolitesrv.tests.integration.lib.expectation.mdbsave.request import make_request
from mail.notsolitesrv.tests.integration.lib.expectation.mdbsave.response import make_success_response_body


class MdbSave:
    @staticmethod
    def expect_call_success(context, users, request_body_matcher=anything()):
        request = make_request(request_body_matcher)
        response_body = make_success_response_body(users)
        response = MockResponse(status=200, body=response_body)
        context.pyremock.expect(request=request, response=response)

    @staticmethod
    def expect_call_server_error(context, users, times=1):
        request = make_request()
        response_body = make_success_response_body(users)
        response = MockResponse(status=500, body=response_body)
        context.pyremock.expect(request=request, response=response, times=times)
