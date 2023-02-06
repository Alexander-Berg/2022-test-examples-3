import logging

import yatest.common
import yt.wrapper as yt

from crypta.lib.python.yt import yt_helpers
from crypta.lib.python.yt.dyntables.convert_to_kv import convert_to_kv
from crypta.lib.python.yt.test_helpers import (
    cypress,
    row_transformers,
)


logger = logging.getLogger(__name__)


class OnRead(object):
    def __init__(self, row_transformer=None, sort_by=None):
        self.row_transformer = row_transformer or (lambda x: x)
        self.sort_by = sort_by


class OnWrite(object):
    def __init__(self, attributes=None, compressed=False, sort_by=None, row_transformer=None):
        self.attributes = attributes or {}
        self.compressed = compressed
        self.sort_by = sort_by
        self.stream_transformer = row_transformers.stream_transformer_from_row_transformer(row_transformer) if row_transformer else (lambda x: x)


class YtTable(cypress.CypressNode):
    def __init__(self, file_path, cypress_path, format, on_read=None, on_write=None):
        super(YtTable, self).__init__(cypress_path)
        self.file_path = file_path
        self.format = format
        self.on_read = on_read or OnRead()
        self.on_write = on_write or OnWrite()

    def __str__(self):
        return "{format} yt table {cypress_path} corresponding to local file {file_path}".format(
            format=self.format.name(), cypress_path=self.cypress_path, file_path=self.file_path)

    def sort_on_local(self, yt_client, sort_by):
        logger.info("Sort %s by %s", self, sort_by)
        yt_client.run_sort(source_table=self.cypress_path, sort_by=sort_by)

    def create_on_local(self, yt_client):
        logger.info("Create %s with attributes %s", self, self.on_write.attributes)
        yt_client.create("table", path=self.cypress_path, recursive=True, attributes=self.on_write.attributes)

    def write_to_local(self, yt_client):
        self.create_on_local(yt_client)

        logger.info("Write %s", self)
        with open(self.file_path, "rb") as input_stream:
            yt_client.write_table(self.cypress_path, self.on_write.stream_transformer(input_stream), format=self.format, is_stream_compressed=self.on_write.compressed, raw=True)

        if self.on_write.sort_by:
            self.sort_on_local(yt_client, self.on_write.sort_by)

    def unfold_cypress_path(self, yt_client):
        if self.cypress_path.endswith("*"):
            tables = list(yt_client.search(self.cypress_dirname, node_type="table", depth_bound=1))
            assert len(tables) == 1
            self.cypress_path = tables[0]

        if self.cypress_path.endswith("+"):
            tables = list(yt_client.search(self.cypress_dirname, node_type="table", depth_bound=1))
            assert tables

            self.cypress_path = yt_client.create_temp_table("//tmp")

            yt_client.run_merge(tables, self.cypress_path, mode="unordered", spec=dict(combine_chunks=True, force_transform=True))

    def read_from_local(self, yt_client):
        self.unfold_cypress_path(yt_client)

        if self.on_read.sort_by:
            self.sort_on_local(yt_client, self.on_read.sort_by)

        logger.info("Read %s", self)
        with open(self.file_path, "wb") as f:
            for row in yt_client.read_table(self.cypress_path, format=self.format, raw=False):
                self.format.dump_row(self.on_read.row_transformer(row), f)


class YamrTable(YtTable):
    def __init__(self, file_path, cypress_path, on_read=None, on_write=None, has_subkey=True, enable_escaping=True, escape_carriage_return=False):
        fmt = yt.format.YamrFormat(has_subkey=has_subkey,
                                   attributes=dict(enable_escaping=enable_escaping, escape_carriage_return=escape_carriage_return))
        super(YamrTable, self).__init__(file_path, cypress_path, fmt, on_read, on_write)


class YamredDsvTable(YtTable):
    def __init__(self, file_path, cypress_path, on_read=None, on_write=None,
                 has_subkey=True, key_column_names=None, subkey_column_names=None, enable_escaping=True, escape_carriage_return=True):
        key_column_names = key_column_names or []
        subkey_column_names = subkey_column_names or []
        fmt = yt.format.YamredDsvFormat(has_subkey=has_subkey, key_column_names=key_column_names, subkey_column_names=subkey_column_names,
                                        attributes=dict(enable_escaping=enable_escaping, escape_carriage_return=escape_carriage_return))
        on_write = on_write or OnWrite()
        on_write.attributes["_format"] = fmt.to_yson_type()

        super(YamredDsvTable, self).__init__(file_path, cypress_path, fmt, on_read, on_write)


class YsonTable(YtTable):
    def __init__(self, file_path, cypress_path, on_read=None, on_write=None, yson_format="text"):
        fmt = yt.format.YsonFormat(format=yson_format)
        super(YsonTable, self).__init__(file_path, cypress_path, fmt, on_read, on_write)


class YsonTableWithExpressions(YsonTable):
    def write_to_local(self, yt_client):
        attributes_wo_expressions = dict(self.on_write.attributes)
        attributes_wo_expressions["schema"] = [x for x in attributes_wo_expressions["schema"] if x.get("expression") is None]

        logger.info("Create %s without fields with expressions", self)
        yt_client.create("table", path=self.cypress_path, recursive=True, attributes=attributes_wo_expressions)

        logger.info("Write %s", self)
        with open(self.file_path, "rb") as input_stream:
            yt_client.write_table(self.cypress_path, self.on_write.stream_transformer(input_stream), format=self.format, is_stream_compressed=self.on_write.compressed, raw=True)

        convert_to_kv(yt_client, self.cypress_path, self.cypress_path, self.on_write.attributes["schema"], self.on_write.attributes)


class DynamicYsonTable(YsonTable):
    def write_to_local(self, yt_client):
        super(DynamicYsonTable, self).write_to_local(yt_client)

        yt_client.alter_table(self.cypress_path, dynamic=True)
        yt_client.mount_table(self.cypress_path, sync=True)
        yt_helpers.wait_for_mounted(yt_client, self.cypress_path)

    def read_from_local(self, yt_client):
        yt_client.freeze_table(self.cypress_path)
        yt_helpers.wait_for_frozen(yt_client, self.cypress_path)

        super(DynamicYsonTable, self).read_from_local(yt_client)

        yt_client.unfreeze_table(self.cypress_path)
        yt_helpers.wait_for_mounted(yt_client, self.cypress_path)


def get_yson_table_with_schema(file_path, cypress_path, schema, dynamic=False):
    cls = DynamicYsonTable if dynamic else YsonTable
    return cls(file_path, cypress_path, on_write=OnWrite(attributes={"schema": schema}))


def get_empty_table_with_schema(cypress_path, schema, dynamic=False):
    return get_yson_table_with_schema("/dev/null", cypress_path, schema, dynamic)


def get_enumerated_sorted_tables(yt_client, yt_dir, local_file_prefix, on_read=None):
    for i, table_path in enumerate(yt_client.list(yt_dir, sort=True, absolute=True)):
        file_path = yatest.common.test_output_path("{}{}".format(local_file_prefix, i))
        yield YsonTable(file_path, table_path, yson_format="pretty", on_read=on_read)
