import logging

from yt import yson

from crypta.lib.python.yt import yt_helpers
from crypta.lib.python.yt.dyntables import (
    kv_client,
    kv_schema,
    kv_setup,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    utils,
)


logger = logging.getLogger(__name__)


class ReplicatedDynamicYsonTable(utils.FileSource):
    replica_attrs = [
        "unmerged_row_count",
        "uncompressed_data_size",
    ]

    def __init__(self, file_path, master_client, master_table, replica_client, replica_table, on_read=None):
        self.file_path = file_path
        self.master_client = master_client
        self.master_table = master_table
        self.replica_client = replica_client
        self.replica_table = replica_table
        self.on_read = on_read or tables.OnRead()

    def create_on_local(self, yt_client):
        kv_setup.kv_setup(
            self.master_client,
            [(yt_helpers.get_cluster_name(self.replica_client), self.replica_client)],
            self.master_table,
            self.replica_table,
            kv_schema.get(),
            kv_schema.create_pivot_keys(1),
            sync=True
        )

    def write_to_local(self, _):
        self.create_on_local(None)

        with open(self.file_path, "rb") as input_stream:
            kv = kv_client.make_kv_client(self.master_client.config["proxy"]["url"], self.master_table, token="FAKE")
            rows = yson.load(input_stream, yson_type="list_fragment")
            rows = [self.on_read.row_transformer(row) for row in rows]
            records = {row["key"]: row["value"] for row in rows}
            kv.write_many(records)

    def get_attr_from_local(self, attr_name, _yt_client, attr_type=None):
        if attr_name in self.replica_attrs:
            yt_client = self.replica_client
            cypress_path = self.replica_table
        else:
            yt_client = self.master_client
            cypress_path = self.master_table

        return utils.get_attr_with_log(logger, attr_name, yt_client, cypress_path, attr_type)
