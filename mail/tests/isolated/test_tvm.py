import pytest
import datetime

import furita_helpers as helpers

from hamcrest import all_of, equal_to_ignoring_case, has_entry, has_item, has_key, is_
from library.python.testing.pyremock.lib.pyremock import HttpMethod, MatchRequest, MockHttpServer, MockResponse

X_YA_SERVICE_TICKET = b'x-ya-service-ticket'
X_YA_USER_TICKET = b'x-ya-user-ticket'

# Generated using the command 'ya tool tvmknife unittest user -d 100500 -e test' (expires in 200 millions of years)
VALID_USER_TICKET = b'3:user:CA0Q__________9_GhIKBAiUkQYQlJEGINKF2MwEKAE:LIgMlFMORwxqhnitUVZrI8stra027' \
                    b'x1xr74LxSpys8eFbG_FBN_3qc9HMCxR97_1mrar4bVqudZhJTjT7JSnIpavWCcD3FRDnoqU8bUEaJfeu' \
                    b'pj0Y90g_QDMFJ2yZHWdB56OfqAcSHi9usIHzHOGBe2fhGf-JkX4ywrBMKiV04I'


def set_up_msearch_mock(context, port, request, mock_response):
    context.msearch = MockHttpServer(port)
    context.msearch.start()
    context.msearch.expect(request, mock_response)


def set_up_mops_mock(context, port, request, mock_response):
    context.mops = MockHttpServer(port)
    context.mops.start()
    context.mops.expect(request, mock_response)


def test_service_ticket_is_passed_during_external_requests(context):
    """ Фурита должна передавать сервисный тикет при походе в другие сервисы (например, в mops и msearch) """
    uid = context.get_uid('SomeUser')

    request = MatchRequest(method=is_(HttpMethod.GET),
                           path=is_('/api/async/mail/furita'),
                           headers=has_key(
                               equal_to_ignoring_case(X_YA_SERVICE_TICKET.decode())))
    mock_response = MockResponse(status=200, body=b'{"envelopes":[]}')
    set_up_msearch_mock(context, context.furita.search_port, request,
                        mock_response)

    rule_id = helpers.create_named_rule(context, uid, 'some rule')
    response = context.furita_api.api_apply(uid, rule_id)

    context.msearch.wait_for(max_timeout=datetime.timedelta(seconds=10))
    context.msearch.assert_expectations()

    assert response.status_code == 200


def test_user_ticket_is_forwarded_during_msearch_requests(context):
    """ Фурита должна пробрасывать полученный пользовательский тикет (если он валиден) при походе в другие сервисы """
    uid = context.get_uid('SomeUser')

    request = MatchRequest(
        method=is_(HttpMethod.GET),
        path=is_('/api/async/mail/furita'),
        headers=all_of(
            has_key(equal_to_ignoring_case(X_YA_SERVICE_TICKET.decode())),
            has_entry(equal_to_ignoring_case(X_YA_USER_TICKET.decode()),
                      has_item(is_(VALID_USER_TICKET.decode())))))
    mock_response = MockResponse(status=200, body=b'{"envelopes":[]}')
    set_up_msearch_mock(context, context.furita.search_port, request,
                        mock_response)

    rule_id = helpers.create_named_rule(context, uid, 'some rule')
    response = context.furita_api.api_apply(uid, rule_id, headers={X_YA_USER_TICKET: VALID_USER_TICKET})

    context.msearch.wait_for(max_timeout=datetime.timedelta(seconds=10))
    context.msearch.assert_expectations()

    assert response.status_code == 200


def test_user_ticket_is_forwarded_during_mops_requests(context):
    """ Фурита должна пробрасывать полученный пользовательский тикет (если он валиден) при походе в другие сервисы """
    uid = context.get_uid('SomeUser')

    msrch_request = MatchRequest(
        method=is_(HttpMethod.GET),
        path=is_('/api/async/mail/furita'),
        headers=all_of(
            has_key(equal_to_ignoring_case(X_YA_SERVICE_TICKET.decode())),
            has_entry(equal_to_ignoring_case(X_YA_USER_TICKET.decode()),
                      has_item(is_(VALID_USER_TICKET.decode())))))
    msrch_response = MockResponse(status=200,
                                  body=b'{"envelopes":["111","222","333"]}')
    set_up_msearch_mock(context, context.furita.search_port, msrch_request,
                        msrch_response)

    mops_request = MatchRequest(
        method=is_(HttpMethod.POST),
        path=is_('/remove'),
        headers=all_of(
            has_key(equal_to_ignoring_case(X_YA_SERVICE_TICKET.decode())),
            has_entry(equal_to_ignoring_case(X_YA_USER_TICKET.decode()),
                      has_item(is_(VALID_USER_TICKET.decode())))))
    mops_response = MockResponse(status=200, body=b'{}')
    set_up_mops_mock(context, context.furita.mops_port, mops_request,
                     mops_response)

    rule_id = helpers.create_named_rule(context, uid, 'some rule')
    response = context.furita_api.api_apply(
        uid, rule_id, headers={X_YA_USER_TICKET: VALID_USER_TICKET})

    context.msearch.wait_for(max_timeout=datetime.timedelta(seconds=10))
    context.msearch.assert_expectations()

    context.mops.wait_for(max_timeout=datetime.timedelta(seconds=10))
    context.mops.assert_expectations()

    assert response.status_code == 200


def test_request_is_rejected_if_user_header_is_invalid(context):
    """ Фурита должна реджектить запрос, если в нем был передан невалидный пользовательский тикет """
    uid = context.get_uid('SomeUser')

    response = context.furita_api.api_list(
        uid, headers={X_YA_USER_TICKET: b'fignya'})
    assert response.status_code == 401


# HELPERS


@pytest.fixture(scope='module', autouse=True)
def permodule_setup(context):
    context.create_user('SomeUser')


@pytest.fixture(scope='function', autouse=True)
def pertest_setup(request, context):
    context.mops = None
    context.msearch = None

    def mocks_teardown():
        if context.mops:
            context.mops.stop()
        if context.msearch:
            context.msearch.stop()

    request.addfinalizer(mocks_teardown)
