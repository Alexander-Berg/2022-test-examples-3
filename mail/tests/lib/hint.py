from base64 import b64encode, b64decode
from itertools import chain


def encode_hint(items):
    items = ((k, isinstance(v, list) and v or [v]) for k, v in items.items())
    return b64encode(
        ("\n".join(k + "=" + str(v) for k, vs in items for v in vs)).encode()).decode()


def make_hint(**kwargs):
    return encode_hint(kwargs)


def get_hint_values(msg):
    raw_headers = [b64decode(value).decode("utf-8").split("\n")
                   for header, value in msg.mime.items() if header == "X-Yandex-Hint"]
    hint_values = [value.split("=") for value in chain(*raw_headers) if "=" in value]

    result = dict()
    for key, value in hint_values:
        if (key in result):
            result[key].append(value)
        else:
            result[key] = [value]

    return result
