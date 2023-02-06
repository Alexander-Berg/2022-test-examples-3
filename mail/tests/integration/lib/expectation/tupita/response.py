import json


def make_success_check_response_body(matched_queries):
    result = {
        "result": [{"matched_queries": matched_queries}]
    }
    return json.dumps(result)


def make_success_conditions_convert_response_body(queries):
    result = {
        "status": "ok",
        "conditions": [{"query": q} for q in queries],
    }
    return json.dumps(result)
