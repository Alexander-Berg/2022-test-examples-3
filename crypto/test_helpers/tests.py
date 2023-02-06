import datetime
import operator
import os

import pytest
import yatest.common
import yt.wrapper as yt
from yt import yson

from crypta.lib.python.yt import (
    path_utils,
    schema_utils,
    yt_helpers,
)
from crypta.lib.python.yt.test_helpers import (
    cypress,
    files,
    replicated_tables,
    tables,
    utils
)


def assert_is_table(thing):
    assert isinstance(thing, tables.YtTable), "{} is not instance of YtTable".format(thing)


def assert_is_table_or_replicated_table(thing):
    assert isinstance(thing, (tables.YtTable, replicated_tables.ReplicatedDynamicYsonTable)), "{} is not instance of YtTable or ReplicatedDynamicYsonTable".format(thing)


def assert_is_cypress_node(thing):
    assert isinstance(thing, cypress.CypressNode), "{} is not instance of CypressNode".format(thing)


def assert_is_yt_entity(thing):
    assert isinstance(thing, (cypress.CypressNode, replicated_tables.ReplicatedDynamicYsonTable)), "{} is not an instance of CypressNode or ReplicatedDynamicYsonTable".format(thing)


class YtTest(object):
    def setup(self, thing, yt_client):
        pass

    def teardown(self, thing, yt_client):
        pass


class DiffWithoutAttrs(YtTest):
    def __init__(self, local=True, diff_tool_timeout=None, yt_client=None):
        self.local = local
        self.diff_tool_timeout = diff_tool_timeout
        self.yt_client = yt_client

    def teardown(self, thing, yt_client):
        if self.yt_client is not None:
            yt_client = self.yt_client

        path = None
        diff_tool_cmd = None

        if isinstance(thing, tables.YtTable):
            thing.read_from_local(yt_client=yt_client)
            path = thing.file_path
            diff_tool_cmd = [utils.get_crypta_diff_tool_path(), "--format", str(thing.format)]
        elif isinstance(thing, files.YtFile):
            thing.read_from_local(yt_client=yt_client)
            path = thing.file_path
        elif isinstance(thing, str):
            path = thing

        assert path is not None
        canonical_file = yatest.common.canonical_file(path, diff_tool=diff_tool_cmd, local=self.local, diff_tool_timeout=self.diff_tool_timeout)
        return [canonical_file]


class FloatToStr:
    def __init__(self, precision):
        self.precision = precision

    def __call__(self, value):
        if isinstance(value, float):
            return "{0:.{1}g}".format(value, self.precision)
        elif isinstance(value, dict):
            return {key: self(value[key]) for key in value}
        elif isinstance(value, list):
            return [self(x) for x in value]
        else:
            return value


float_to_str = FloatToStr(precision=6)


class DiffUserAttrs(YtTest):
    def __init__(self, transformer=None, ignore_yql=True, yt_client=None):
        self.transformer = transformer or (lambda x: x)
        self.ignore_yql = ignore_yql
        self.yt_client = yt_client

    def teardown(self, thing, yt_client):
        if self.yt_client is not None:
            yt_client = self.yt_client

        keys = thing.get_attr_from_local("user_attribute_keys", yt_client)

        return [float_to_str({
            "path": thing.cypress_path,
            "user_attrs": self.transformer({
                key: yson.yson_to_json(thing.get_attr_from_local(key, yt_client))
                for key in keys
                if not self.ignore_yql or not key.startswith("_yql")
            })
        })]


class Diff(YtTest):
    def __init__(self, local=True, diff_tool_timeout=None, transformer=None, ignore_yql=True, yt_client=None):
        self.diff = DiffWithoutAttrs(local, diff_tool_timeout, yt_client)
        self.diff_user_attrs = DiffUserAttrs(transformer, ignore_yql, yt_client)

    def teardown(self, thing, yt_client):
        diff_result = self.diff.teardown(thing, yt_client)
        diff_user_attrs_result = self.diff_user_attrs.teardown(thing, yt_client)
        return [{
            "file": diff_result[0],
            "user_attrs": diff_user_attrs_result[0]["user_attrs"],
        }]


class TableIsNotChanged(YtTest):
    """
    Compares uncompressed_data_size and row_count attributes before and after test
    """
    def __init__(self):
        self.data_size = None
        self.row_count = None

    def get_row_count(self, thing, yt_client):
        return thing.get_attr_from_local("unmerged_row_count" if isinstance(thing, (tables.DynamicYsonTable, replicated_tables.ReplicatedDynamicYsonTable)) else "row_count", yt_client, attr_type=int)

    def setup(self, thing, yt_client):
        assert_is_table_or_replicated_table(thing)

        self.data_size = thing.get_attr_from_local("uncompressed_data_size", yt_client, attr_type=int)
        self.row_count = self.get_row_count(thing, yt_client)

    def teardown(self, thing, yt_client):
        data_size = thing.get_attr_from_local("uncompressed_data_size", yt_client, attr_type=int)
        row_count = self.get_row_count(thing, yt_client)

        assert self.row_count == row_count, "Row count of {} is changed from {} to {} after test".format(thing.cypress_path, self.row_count, row_count)
        assert self.data_size == data_size, "Uncompressed data size of {} is changed from {} to {} after test".format(thing.cypress_path, self.data_size, data_size)


class Exists(YtTest):
    def __init__(self, yt_client=None):
        self.yt_client = yt_client

    def teardown(self, thing, yt_client):
        if self.yt_client is not None:
            yt_client = self.yt_client

        assert_is_cypress_node(thing)
        if isinstance(thing, tables.YtTable):
            thing.unfold_cypress_path(yt_client)
        assert thing.exists_on_local(yt_client), "Cypress path {} doesn't exist".format(thing.cypress_path)


class IsAbsent(YtTest):
    def teardown(self, thing, yt_client):
        assert_is_cypress_node(thing)
        assert not thing.exists_on_local(yt_client), "Cypress path {} exists".format(thing.cypress_path)


class AttrEquals(YtTest):
    def __init__(self, attr, value, comparator=operator.eq, deduct_attr_type=True, yt_client=None):
        self.attr = attr
        self.value = value
        self.comparator = comparator
        self.deduct_attr_type = deduct_attr_type
        self.yt_client = yt_client

    def teardown(self, thing, yt_client):
        yt_client = self.yt_client or yt_client

        assert_is_cypress_node(thing)
        if isinstance(thing, tables.YtTable):
            thing.unfold_cypress_path(yt_client)
        actual_value = thing.get_attr_from_local(self.attr, yt_client, attr_type=type(self.value) if self.deduct_attr_type else None)
        assert self.comparator(self.value, actual_value), "{} of {} is {} != {}".format(self.attr, thing.cypress_path, actual_value, self.value)


class AttrsEquals(YtTest):
    def __init__(self, attrs):
        assert isinstance(attrs, dict)
        self.checks = [AttrEquals(attr, value) for attr, value in attrs.items()]

    def teardown(self, thing, yt_client):
        for check in self.checks:
            check.teardown(thing, yt_client)


class RowCount(AttrEquals):
    def __init__(self, row_count):
        super(RowCount, self).__init__("row_count", row_count)

    def teardown(self, thing, yt_client):
        assert_is_table(thing)
        super(RowCount, self).teardown(thing, yt_client)


class SchemaEquals(AttrEquals):
    def __init__(self, schema, yt_client=None):
        super(SchemaEquals, self).__init__("schema", schema, comparator=schema_utils.are_schemas_equal, deduct_attr_type=False)

    def teardown(self, thing, yt_client):
        assert_is_table(thing)
        super(SchemaEquals, self).teardown(thing, yt_client)


class UncompressedDataSize(AttrEquals):
    def __init__(self, uncompressed_data_size):
        super(UncompressedDataSize, self).__init__("uncompressed_data_size", uncompressed_data_size)


def from_isoformat(date_str):
    return datetime.datetime.strptime(date_str.strip('"'), "%Y-%m-%dT%H:%M:%S.%fZ")


def get_expected_expiration_time_from_creation_time(thing, ttl, yt_client):
    creation_time = thing.get_attr_from_local("creation_time", yt_client, attr_type=from_isoformat)
    return creation_time + ttl


class ExpirationTime(YtTest):
    def __init__(
        self,
        ttl,
        expected_expiration_time_func=get_expected_expiration_time_from_creation_time,
        yt_client=None,
    ):
        self.ttl = ttl
        self.yt_client = yt_client
        self.expected_expiration_time_func = expected_expiration_time_func

    def teardown(self, thing, yt_client):
        assert_is_cypress_node(thing)

        if self.yt_client is not None:
            yt_client = self.yt_client

        expected_expiration_time = self.expected_expiration_time_func(thing, self.ttl, yt_client)
        expiration_time = thing.get_attr_from_local("expiration_time", yt_client, attr_type=from_isoformat)
        assert expected_expiration_time == expiration_time, "Expected: {}, Actual: {}".format(
            expected_expiration_time, expiration_time)


class ExpirationTimeByTableName(ExpirationTime):
    def __init__(self, ttl, name_format="%Y-%m-%d", yt_client=None):
        def get_expected_expiration_time_from_table_name(thing, ttl, yt_client):
            name = path_utils.get_basename(thing.cypress_path)
            return datetime.datetime.strptime(name, name_format) + ttl

        super(ExpirationTimeByTableName, self).__init__(ttl, get_expected_expiration_time_from_table_name, yt_client)


class TestNodesInMapNode(YtTest):
    def __init__(self, tests_getter, tag, on_absence=lambda: None, on_read=None, filename_func=None):
        self.tests_getter = tests_getter if callable(tests_getter) else (lambda _: tests_getter)
        self.tag = tag
        self.on_absence = on_absence
        self.on_read = on_read
        self.filename_func = filename_func or (lambda yt_client, dir_path, name: name)

    def teardown(self, thing, yt_client):
        assert_is_cypress_node(thing)

        if not thing.exists_on_local(yt_client):
            self.on_absence()
            return

        results = []

        for name in yt_client.list(thing.cypress_path, absolute=False):
            cypress_path = yt.ypath_join(thing.cypress_path, name)
            node_type = yt_helpers.get_attribute(cypress_path, "type", yt_client)
            filename = "{}_{}".format(self.tag, self.filename_func(yt_client, thing.cypress_path, name))
            file_path = yatest.common.test_output_path(filename)

            if node_type == "table":
                node = tables.YsonTable(file_path, cypress_path, yson_format="pretty", on_read=self.on_read)
            elif node_type == "file":
                node = files.YtFile(file_path, cypress_path)
            else:
                node = cypress.CypressNode(cypress_path)

            results += teardown_tests(self.tests_getter(node), node, yt_client)

        return results


class TestNodesInMapNodeChildren(YtTest):
    def __init__(self, tests_getter, tag, on_absence=lambda: None, on_read=None):
        self.tests_getter = tests_getter if callable(tests_getter) else (lambda _: tests_getter)
        self.tag = tag
        self.on_absence = on_absence
        self.on_read = on_read

    def teardown(self, thing, yt_client):
        assert_is_cypress_node(thing)

        if not thing.exists_on_local(yt_client):
            self.on_absence()
            return

        results = []

        for name in yt_client.list(thing.cypress_path, absolute=False):
            tag = "{}_{}".format(self.tag, name)
            node = cypress.CypressNode(yt.ypath_join(thing.cypress_path, name))
            results += TestNodesInMapNode(self.tests_getter, tag, on_read=self.on_read).teardown(node, yt_client)

        return results


class IfExists(YtTest):
    def __init__(self, tests):
        self.tests = tests

    def teardown(self, thing, yt_client):
        assert_is_cypress_node(thing)

        if thing.exists_on_local(yt_client):
            return teardown_tests(self.tests, thing, yt_client)


def teardown_tests(tests, thing, yt_client):
    results = []

    for test in tests:
        result = test.teardown(thing, yt_client)
        if result:
            results += result

    return results


def normalize(cypress_node, tests):
    if isinstance(tests, YtTest):
        tests = [tests]

    tests = tests or []

    assert_is_yt_entity(cypress_node)
    assert all([isinstance(test, YtTest) for test in tests])

    return cypress_node, tests


def yt_test(yt_client,
            binary,
            args,
            data_path="",
            input_tables=None,
            output_tables=None,
            stdout_fname="",
            stdin_fname="",
            stdout_test=None,
            env=None,
            must_be_execution_error=False):
    stdout_files = [(stdout_fname, [stdout_test])] if (stdout_fname and stdout_test) else []

    return yt_test_func(
        yt_client,
        lambda: test_binary(binary, args, stdout_fname, stdin_fname, env, must_be_execution_error),
        data_path,
        input_tables,
        output_tables,
        stdout_files,
    )


def test_binary(binary,
                args,
                stdout_fname,
                stdin_fname,
                env,
                must_be_execution_error):
    env = dict(os.environ, **(env or {}))

    env["YT_PREFIX"] = "//"
    env.setdefault("YT_TOKEN", "FAKE")
    env["Y_NO_AVX_IN_DOT_PRODUCT"] = '1'

    stdout = open(stdout_fname, "wb") if stdout_fname else None
    stdin = open(stdin_fname, "rb") if stdin_fname else None

    if must_be_execution_error:
        with pytest.raises(yatest.common.ExecutionError):
            yatest.common.execute([binary] + args, stdout=stdout, stdin=stdin, env=env)
    else:
        yatest.common.execute([binary] + args, stdout=stdout, stdin=stdin, env=env)


def yt_test_func(yt_client,
                 func,
                 data_path="",
                 input_tables=None,
                 output_tables=None,
                 additional_tests=None,
                 return_result=False,
                 ):
    input_tables = input_tables or []
    input_tables = [normalize(input_table, tests) for input_table, tests in input_tables]
    output_tables = output_tables or []
    output_tables = [normalize(output_table, tests) for output_table, tests in output_tables]
    additional_tests = additional_tests or []
    all_tests = input_tables + output_tables + additional_tests
    work_dir = os.getcwd()

    for input_table, _ in input_tables:
        input_table.to_abs_file_path(data_path)
        input_table.write_to_local(yt_client)

    for output_table, _ in output_tables:
        if isinstance(output_table, (tables.YtTable, files.YtFile)) and output_table.file_path:
            output_table.to_abs_file_path(work_dir)

    for thing, tests in all_tests:
        for test in tests:
            test.setup(thing, yt_client)

    result = func()

    results = [result] if return_result else []

    for thing, tests in all_tests:
        results += teardown_tests(tests, thing, yt_client)

    return results
