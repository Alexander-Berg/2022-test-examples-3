import json

FORWARD_TO_EMAIL = "local@domain.net"


def make_success_blackwhitelist_response_body(whitelist=[], blacklist=[]):
    return json.dumps({
        "whitelist": ", ".join(whitelist) if len(whitelist) else [],
        "blacklist": ", ".join(blacklist) if len(whitelist) else [],
        "session": "Jlbe9cUPPiE1"
    })


def make_server_error_blackwhitelist_response_body():
    return json.dumps({
        "report": "exception",
        "status": "error",
        "session": "Jlbe9cUPPiE1"
    })


def make_success_get_response_body(org_id):
    result = {
        "rules": [{
            "terminal": False,
            "scope": {"direction": "inbound"},
            "condition_query": "ConditionQuery",
            "actions": [{
                "data": {}
            }]
        }],
        "revision": 1
    }
    if org_id == "0":
        result["rules"][0]["actions"][0]["action"] = "drop"
    elif org_id == "1":
        result["rules"][0]["actions"][0]["action"] = "forward"
        result["rules"][0]["actions"][0]["data"]["email"] = FORWARD_TO_EMAIL
    return json.dumps(result)


def make_success_list_response_body():
    result = {
        "rules": [{
            "id": "id",
            "priority": 1,
            "query": "query",
            "enabled": True,
            "stop": False,
            "actions": [{
                "verified": True,
                "parameter": FORWARD_TO_EMAIL,
                "type": "forwardwithstore"
            }]
        }]
    }
    return json.dumps(result)
