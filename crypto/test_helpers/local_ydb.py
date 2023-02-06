import logging
import os

import ydb
import retry

from crypta.lib.python.ydb.ydb_client import YdbClient


logger = logging.getLogger(__name__)


class LocalYdb(object):
    def __init__(self, endpoint, database):
        self.client = YdbClient(endpoint, database)

    @property
    def endpoint(self):
        return self.client.endpoint

    @property
    def database(self):
        return self.client.database

    @retry.retry(exceptions=ydb.NotFound, tries=10, delay=1, logger=logger)
    def upload_data(self, path, data):
        columns = self.client.describe_table(path).columns

        template = """
            DECLARE $data AS "List<Struct<
                {}
            >>";

            INSERT INTO `{}`
            SELECT {}
            FROM AS_TABLE($data);
        """
        query = template.format(
            ", ".join("{}: {}".format(column.name, str(column.type)) for column in columns),
            path,
            ", ".join(column.name for column in columns),
        )

        session = self.client.create_session()
        session.transaction(ydb.SerializableReadWrite()).execute(
            session.prepare(query),
            commit_tx=True,
            parameters={
                "$data": data
            }
        )

    def dump_table(self, path):
        path = self.client.get_full_path(path)
        it = self.client.create_session().read_table(path, ordered=True)
        return sum([x.rows for x in it], [])

    def dump_dir(self, path):
        result = {}
        for item in self.client.list_directory(path):
            assert item.is_table()
            result[item.name] = self.dump_table(os.path.join(path, item.name))
        return result

    @retry.retry(exceptions=ydb.Overloaded, tries=10, delay=1, logger=logger)
    def remove_all(self):
        logger.info("Try to remove all from local ydb...")
        for item in self.client.list_directory():
            if item.is_table():
                self.client.drop_table(item.name)
            if item.is_directory():
                self.client.remove_directory(item.name, recursive=True)
