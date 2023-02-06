import base64
import os

import yatest

from crypta.lib.python.rtmr.log_serializers.bs_watch_log_serializer import BsWatchLogSerializer


def test_bs_watch_log_serializer():
    json_input = yatest.common.test_source_path("data/bs_watch_log.json")
    json_symlink = yatest.common.test_output_path("input.json")
    os.symlink(json_input, json_symlink)

    output_file = BsWatchLogSerializer()(json_symlink)

    encoded_output_file = yatest.common.test_output_path("encoded_output.lenval")
    with open(output_file, "rb") as output, open(encoded_output_file, "wb") as encoded_output:
        encoded_output.write(base64.b64encode(output.read()))

    return yatest.common.canonical_file(encoded_output_file, local=True)
