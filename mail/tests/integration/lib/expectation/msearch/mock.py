

from library.python.testing.pyremock.lib.pyremock import MockResponse

from mail.notsolitesrv.tests.integration.lib.expectation.msearch.request import make_request
from mail.notsolitesrv.tests.integration.lib.expectation.msearch.response import (
    SubscriptionStatus,
    make_success_body_response,
    make_server_error_body_response
)


class MSearch:
    @staticmethod
    def expect_call_success(context, expected_statuses, opt_in_uids=[]):
        uids = list(set([str(s["uid"]) for s in expected_statuses]))
        emails = list(set([s["email"] for s in expected_statuses]))
        statuses = [
            SubscriptionStatus(
                uid=uid,
                email=email,
                status=next(
                    (s["status"] for s in expected_statuses if str(s["uid"]) == uid and s["email"] == email),
                    "active"
                )
            )
            for uid in uids
            for email in emails
        ]
        request = make_request(uids=uids, emails=emails, opt_in_uids=opt_in_uids)
        response_body = make_success_body_response(status=statuses)
        response = MockResponse(status=200, body=response_body)
        context.pyremock.expect(request=request, response=response)

    @staticmethod
    def expect_call_server_error(context, uid, email, times=1):
        request = make_request(uids=uid, emails=email)
        response_body = make_server_error_body_response()
        response = MockResponse(status=500, body=response_body)
        context.pyremock.expect(request=request, response=response, times=times)
