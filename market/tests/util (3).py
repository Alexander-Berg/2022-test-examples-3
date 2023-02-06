# coding: utf-8

import base64
import six
import yt.yson


def convert_yt_str(data):
    return six.ensure_binary(
        base64.b64encode(
            data if six.PY2 else yt.yson.get_bytes(data)
        )
    )
