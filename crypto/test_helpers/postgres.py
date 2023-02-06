import contextlib
import typing

import psycopg2


class Postgres(typing.NamedTuple):
    host: str
    port: int
    dbname: str
    user: str

    @contextlib.contextmanager
    def connect(self):
        with psycopg2.connect(host=self.host, port=self.port, dbname=self.dbname, user=self.user) as connection:
            yield connection
