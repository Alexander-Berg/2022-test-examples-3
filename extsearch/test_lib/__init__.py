from nile.api.v1.local import YsonFormat
from nile.api.v1 import Record
from library.python import resource
import six
import os
import sys


def resource_records(path, as_dict=False):
    assert resource.find(path) is not None
    res = YsonFormat(bytes_decode_mode='strict').deserialize(six.BytesIO(resource.find(path)))
    if as_dict:
        res = map(lambda x: x.to_dict(), res)
    return res


def dump_result_records(output, output_name, is_dict=False):
    print('output file path:', file=sys.stderr)
    print(os.path.abspath(output_name), file=sys.stderr)
    s = open(output_name, 'wb')
    if is_dict:
        output = map(Record, output)
    YsonFormat(format='pretty').serialize(output, s)


def cmp_tables(expected, output, cmp_order=False):
    for i, r in enumerate(expected):
        assert r in output, 'line {} not found in {}'.format(i, output)

    assert len(output) == len(expected), 'expected is smaller then result'

    if cmp_order:
        for i, r in enumerate(expected):
            assert expected[i] == output[i], 'expected and result orders are not same'
