from library.python.testing.pyremock.lib.pyremock import MockResponse
from mail.notsolitesrv.tests.integration.lib.expectation.tupita.request import (
    make_check_request,
    make_conditions_convert_request,
)
from mail.notsolitesrv.tests.integration.lib.expectation.tupita.response import (
    make_success_check_response_body,
    make_success_conditions_convert_response_body,
)


class Tupita:
    @staticmethod
    def expect_check_call_success(context, uid, matched_queries=[]):
        request = make_check_request(uid)
        response_body = make_success_check_response_body(matched_queries)
        response = MockResponse(status=200, body=response_body)
        context.pyremock.expect(request=request, response=response)

    @staticmethod
    def expect_conditions_convert_call_success(context, org_id, queries):
        request = make_conditions_convert_request(org_id)
        response_body = make_success_conditions_convert_response_body(queries)
        response = MockResponse(status=200, body=response_body)
        context.pyremock.expect(request=request, response=response)
