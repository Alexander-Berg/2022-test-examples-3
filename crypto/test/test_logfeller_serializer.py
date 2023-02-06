import base64
import os
import six

import yatest.common

from crypta.lib.python.rtmr.log_serializers.logfeller_serializer import LogfellerSerializer


def test_logfeller_serializer():
    json_input = yatest.common.test_source_path("data/logfeller_input.json")
    json_symlink = yatest.common.test_output_path("input.json")
    os.symlink(json_input, json_symlink)

    output_file = LogfellerSerializer(item_serializer=six.ensure_binary)(json_symlink)

    encoded_output_file = yatest.common.test_output_path("encoded_output.json")
    with open(output_file, "rb") as output, open(encoded_output_file, "wb") as encoded_output:
        encoded_output.write(base64.b64encode(output.read()))

    return yatest.common.canonical_file(encoded_output_file, local=True)
