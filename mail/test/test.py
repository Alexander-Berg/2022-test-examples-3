from json_value import TJsonValue, JSON_UNDEFINED, \
    JSON_NULL, JSON_BOOLEAN, JSON_INTEGER, JSON_DOUBLE, \
    JSON_STRING, JSON_MAP, JSON_ARRAY, JSON_UINTEGER, \
    make_json_value, read_json_value_from_string
from ymod_python_sys import deadline_timer, terminate_application, log, severity_level
import sys


def string_to_json_value(s):
    log(severity_level.info, "in python string_to_json_value: " + s)
    print("in python string_to_json_value: " + s)
    sys.stdout.flush()
    result = TJsonValue()
    result.as_dict = {
        "string" : s
    }
    return result


def json_value_to_string(json):
    log(severity_level.info, "in python json_value_to_string: " + str(json))
    print("in python json_value_to_string: " + str(json))
    sys.stdout.flush()
    return str(json)


#exit after 5 seconds
deadline_timer(None, 5000, lambda x: terminate_application())
