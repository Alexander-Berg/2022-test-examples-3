import json


def stringify_keys(some_struct):
    new_struct = {}
    """Convert a dict's keys to strings if they are not."""
    for key in some_struct.keys():
        new_key = key
        # convert nonstring to string if needed
        if not isinstance(key, str):
            new_key = str(key).replace('$', '')
        # check inner dict
        if isinstance(some_struct[key], dict):
            new_struct[new_key] = stringify_keys(some_struct[key])
        else:
            new_struct[new_key] = some_struct[key]
    return new_struct


def assert_json_equal(a, b):
    a_json = json.dumps(stringify_keys(a))
    b_json = json.dumps(stringify_keys(b))
    assert json.loads(a_json) == json.loads(b_json)
