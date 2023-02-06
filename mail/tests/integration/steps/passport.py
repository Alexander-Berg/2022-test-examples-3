from tests_common.pytest_bdd import given, then

from library.python.testing.pyremock.lib.pyremock import MatchRequest, MockResponse, HttpMethod

from hamcrest import (
    equal_to,
)


@given('passport will respond without errors')
def step_given_passport_will_respond_without_errors(context):
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.POST),
        path=equal_to('/2/account/{uid}/options/'.format(uid=context.params['uid'])),
    ), response=MockResponse(status=200, body='{"status":"ok"}'))


@given('passport will respond with 500 {times:d} times')
def step_given_passport_will_respond_with_500(context, times):
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.POST),
        path=equal_to('/2/account/{uid}/options/'.format(uid=context.params['uid'])),
    ), response=MockResponse(status=500, body=''), times=times)


@given('passport will respond with 400 {times:d} times')
def step_given_passport_will_respond_with_400(context, times):
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.POST),
        path=equal_to('/2/account/{uid}/options/'.format(uid=context.params['uid'])),
    ), response=MockResponse(status=400, body=''), times=times)


@given('passport will respond with retriable errors {times:d} times')
def step_given_passport_will_respond_with_retriable_errors(context, times):
    pyremock = context.coordinator.pyremock
    body = '{"status":"error", "errors":["backend.blackbox_failed","backend.yasms_failed"]}'
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.POST),
        path=equal_to('/2/account/{uid}/options/'.format(uid=context.params['uid'])),
    ), response=MockResponse(status=200, body=body), times=times)


@given('passport will respond with nonretriable errors {times:d} times')
def step_given_passport_will_respond_with_nonretriable_errors(context, times):
    pyremock = context.coordinator.pyremock
    body = '{"status":"error", "errors:["some error"]"}'
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.POST),
        path=equal_to('/2/account/{uid}/options/'.format(uid=context.params['uid'])),
    ), response=MockResponse(status=200, body=body), times=times)


@given('passport will respond with illformed response {times:d} times')
def step_given_passport_will_respond_with_illwormed_response(context, times):
    pyremock = context.coordinator.pyremock
    body = '<xml>'
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.POST),
        path=equal_to('/2/account/{uid}/options/'.format(uid=context.params['uid'])),
    ), response=MockResponse(status=200, body=body), times=times)


@then('there are no unexpected requests to passport')
def step_then_there_are_no_unexpected_requests_to_passport(context):
    context.coordinator.pyremock.assert_expectations()
