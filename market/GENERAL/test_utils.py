import json


def make_json_diff_msg(left, right, msg=''):
    import difflib
    left = json.dumps(left, indent=4, sort_keys=True)
    right = json.dumps(right, indent=4, sort_keys=True)
    lines = difflib.context_diff(left.splitlines(keepends=True), right.splitlines(keepends=True), n=5)
    lines = ''.join(lines)
    return msg + '\n' + lines

