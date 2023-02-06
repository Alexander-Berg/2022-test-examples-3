import os
import shutil

from yatest import common
from rtmapreduce.tests.yatest import rtmr_test
import six


def _data_path(*args):
    return common.test_output_path(os.path.join("input_data", *args))


def format_canonical_file(table, formatter=None):
    if formatter:
        table = formatter(table)

    return common.canonical_file(table, local=True)


def run(tmpdir, input_formatters, output_formatters, input_dir, name, manifest, *args, **kwargs):
    rtmr_test.init(tmpdir)

    shutil.copytree(input_dir, _data_path())
    os.chmod(_data_path(), 0o777)

    for table, formatter in input_formatters.items():
        input_formatters[table](_data_path(table))

    kwargs.setdefault("output_format", "lenval")
    kwargs.setdefault("split_files", True)
    kwargs["input_path"] = _data_path()
    kwargs["canonize"] = False

    output = rtmr_test.run(name, manifest, *args, **kwargs)

    for key, table in six.iteritems(output):
        name = table[len(common.test_output_path())+1:]
        output[key] = format_canonical_file(table, output_formatters.get(name))

    return output
