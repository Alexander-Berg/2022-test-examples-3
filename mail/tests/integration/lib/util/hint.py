from base64 import b64encode


def encode_hint(items):
    items = ((k, isinstance(v, list) and v or [v]) for k, v in items.items())
    return b64encode(
        ("\n".join(k + "=" + str(v) for k, vs in items for v in vs)).encode()).decode()


def make_hint_value(**kwargs):
    return encode_hint(kwargs)
