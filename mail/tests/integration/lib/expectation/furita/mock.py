from library.python.testing.pyremock.lib.pyremock import MockResponse
from mail.notsolitesrv.tests.integration.lib.expectation.furita.request import (
    make_blackwhitelist_request,
    make_get_request,
    make_list_request
)
from mail.notsolitesrv.tests.integration.lib.expectation.furita.response import (
    make_success_blackwhitelist_response_body,
    make_server_error_blackwhitelist_response_body,
    make_success_get_response_body,
    make_success_list_response_body
)
from mail.notsolitesrv.tests.integration.lib.util.user import User


class Furita:
    @staticmethod
    def expect_blackwhitelist_call_success(context, user: User):
        request = make_blackwhitelist_request(uid=user.uid)
        response_body = make_success_blackwhitelist_response_body()
        response = MockResponse(status=200, body=response_body)
        context.pyremock.expect(request=request, response=response)

    @staticmethod
    def expect_blackwhitelist_call_server_error(context, user: User):
        request = make_blackwhitelist_request(uid=user.uid)
        response_body = make_server_error_blackwhitelist_response_body()
        response = MockResponse(status=500, body=response_body)
        context.pyremock.expect(request=request, response=response)

    @staticmethod
    def expect_get_call_success(context, org_id):
        request = make_get_request(org_id=org_id)
        response_body = make_success_get_response_body(org_id=org_id)
        response = MockResponse(status=200, body=response_body)
        context.pyremock.expect(request=request, response=response)

    @staticmethod
    def expect_get_call_server_error(context, org_id, times=1):
        request = make_get_request(org_id=org_id)
        response_body = make_success_get_response_body(org_id=org_id)
        response = MockResponse(status=500, body=response_body)
        context.pyremock.expect(request=request, response=response, times=times)

    @staticmethod
    def expect_list_call_success(context, uid):
        request = make_list_request(uid=uid)
        response_body = make_success_list_response_body()
        response = MockResponse(status=200, body=response_body)
        context.pyremock.expect(request=request, response=response)

    @staticmethod
    def expect_list_call_server_error(context, uid, times=1):
        request = make_list_request(uid=uid)
        response_body = make_success_list_response_body()
        response = MockResponse(status=500, body=response_body)
        context.pyremock.expect(request=request, response=response, times=times)
