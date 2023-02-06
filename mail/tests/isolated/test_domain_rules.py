import json
from library.python.testing.pyremock.lib.pyremock import (
    MatchRequest,
    MockResponse,
    HttpMethod
)
from hamcrest import is_, contains_string


def expect_call_tupita_mock(context, rules_count, times):
    http_req = MatchRequest(method=is_(HttpMethod.POST), path=contains_string('/api/mail/conditions/convert'))
    queries = ["{{\"query\": \"some query {}\"}}".format(i) for i in range(rules_count)]
    mock_response = MockResponse(status=200, body="""{
            "status": "ok",
            "conditions": [
                """ + ",".join(queries) + """
            ]
    }""")
    context.tupita_rules_mock.expect(http_req, mock_response, times=times)


def expect_call_blackbox(context, return_orgid, times):
    http_req = MatchRequest(method=is_(HttpMethod.GET), path=contains_string('/blackbox'))
    mock_response = MockResponse(status=200, body="""{{"users":[{{"attributes":{{"1031":"{}"}}, "id": "54321"}}]}}""".format(return_orgid))
    context.blackbox_mock.expect(http_req, mock_response, times=times)


def test_domain_rules_set_for_correct_request_should_response_200(context):
    rules_data = {
        "rules": [
            {
                "terminal": True,
                "scope": {"direction": "inbound"},
                "condition": {"$eq": "foobar"},
                "actions": [
                    {
                        "action": "forward",
                        "data": {
                            "email": "foo"
                        }
                    }
                ]
            }
        ]
    }
    expect_call_tupita_mock(context, rules_count=1, times=1)
    expect_call_blackbox(context, return_orgid=25, times=1)
    response = context.furita_api.api_domain_rules_set(25, rules_data)
    assert response.status_code == 200
    context.tupita_rules_mock.assert_expectations()
    context.blackbox_mock.assert_expectations()


def test_domain_rules_set_for_request_with_bad_actions_should_response_500(context):
    rules_data = {
        "rules": [
            {
                "terminal": True,
                "scope": {"direction": "inbound"},
                "condition": {"$eq": "foobar"},
                "actions": "foobar"
            }
        ]
    }
    response = context.furita_api.api_domain_rules_set(25, rules_data)
    assert response.status_code == 500

    context.tupita_rules_mock.assert_expectations()
    context.blackbox_mock.assert_expectations()


def test_domain_rules_get_should_response_200(context):
    rules_data = {
        "rules": [
            {
                "terminal": True,
                "scope": {"direction": "inbound"},
                "condition": {"$eq": "foobar_for_rule_25"},
                "actions": [
                    {
                        "action": "forward",
                        "data": {
                            "email": "foo"
                        }
                    }
                ]
            }
        ]
    }
    expect_call_tupita_mock(context, rules_count=1, times=1)
    expect_call_blackbox(context, return_orgid=42, times=1)
    response = context.furita_api.api_domain_rules_set(42, rules_data)
    assert response.status_code == 200

    response = context.furita_api.api_domain_rules_get(42)
    assert response.status_code == 200
    expected_answer = rules_data
    expected_answer["rules"][0]["condition_query"] = "some query 0"
    expected_answer["revision"] = 1
    assert json.loads(response.text) == expected_answer

    context.tupita_rules_mock.assert_expectations()
    context.blackbox_mock.assert_expectations()


def test_domain_rules_get_for_non_existent_rule_should_response_404(context):
    rules_data = {
        "rules": [
            {
                "terminal": True,
                "scope": {"direction": "inbound"},
                "condition": {"$eq": "foobar"},
                "actions": [
                    {
                        "action": "forward",
                        "data": {
                            "email": "foo"
                        }
                    }
                ]
            }
        ]
    }
    expect_call_tupita_mock(context, rules_count=1, times=1)
    expect_call_blackbox(context, return_orgid=25, times=1)
    response = context.furita_api.api_domain_rules_set(25, rules_data)
    assert response.status_code == 200

    response = context.furita_api.api_domain_rules_get(26)
    assert response.status_code == 404

    context.tupita_rules_mock.assert_expectations()
    context.blackbox_mock.assert_expectations()
