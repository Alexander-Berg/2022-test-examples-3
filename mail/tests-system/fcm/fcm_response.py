import json

errors = {
    "": (200, [{"message_id": 1}]),
    "MissingRegistration": (200, [{"error": "MissingRegistration"}]),
    "InvalidRegistration": (200, [{"error": "InvalidRegistration"}]),
    "NotRegistered": (200, [{"error": "NotRegistered"}]),
    "InvalidPackageName": (200, [{"error": "InvalidPackageName"}]),
    "MismatchSenderId": (200, [{"error": "MismatchSenderId"}]),
    "MessageTooBig": (200, [{"error": "MessageTooBig"}]),
    "InvalidDataKey": (200, [{"error": "InvalidDataKey"}]),
    "InvalidTtl": (200, [{"error": "InvalidTtl"}]),
    "DeviceMessageRateExceeded": (200, [{"error": "DeviceMessageRateExceeded"}]),
    "TopicsMessageRateExceeded": (200, [{"error": "TopicsMessageRateExceeded"}]),
    "Unavailable": (200, [{"error": "Unavailable"}]),
    "Unavailable5XX": (504, [{"error": "Unavailable"}]),
    "InternalServerError": (200, [{"error": "InternalServerError"}]),
    "InternalServerError5XX": (500, [{"error": "InternalServerError"}]),
}


def from_error(error):
    resp = {
        "multicast_id": 1,
        "success": 0 if len(error) else 1,
        "failure": 1 if len(error) else 0,
        "canonical_ids": 0,
        "results": [err for err in errors[error][1]],
    }
    return (errors[error][0], json.dumps(resp))


def topic_from_error(error):
    resp = errors[error][1][0]
    return (errors[error][0], json.dumps(resp))


success = from_error("")
topic_success = topic_from_error("")

success_but_token_changed = (
    200,
    json.dumps(
        {
            "multicast_id": 1,
            "success": 1,
            "failure": 0,
            "canonical_ids": 1,
            "results": [{"message_id": "1", "registration_id": "other"}],
        }
    ),
)

unauthorized = (
    401,
    "<HTML>\n<HEAD>\n<TITLE>UnauthorizedRegistration</TITLE>\n"
    + '</HEAD>\n<BODY BGCOLOR="#FFFFFF" TEXT="#000000">\n<H1>UnauthorizedRegistration</H1>\n'
    + "<H2>Error 400</H2>\n</BODY>\n</HTML>\n",
)

invalid_json_in_body = (400, "JSON_PARSING_ERROR: Unexpected token END OF FILE at position 27.\n")

invalid_data_field_in_body = (400, 'Field "data" must be a JSON array: smth\n')

check_ok = (
    200,
    json.dumps(
        {
            "applicationVersion": "55814",
            "application": "xiva.mob.send.test",
            "scope": "*",
            "authorizedEntity": "1087931301371",
            "appSigner": "5d224274d9377c35da777ad934c65c8cca6e7a20",
            "platform": "ANDROID",
        }
    ),
)

check_bad = (400, json.dumps({"error": "InvalidToken"}))

check_unexpected = (404, json.dumps({"error": "NotFound"}))
