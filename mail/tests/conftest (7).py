import pytest
import os
import psycopg2

from mail.devpack.lib.components.sharpei import Mdb
from mail.devpack.tests.helpers.fixtures import coordinator_context

from mail.mdbsave.devpack.components.root import MdbSaveService
from mail.mdbsave.devpack.components.mdbsave import MdbSaveComponent

from mdbsave_api import MdbSaveApi


@pytest.fixture(scope="session", autouse=True)
def env():
    with coordinator_context(MdbSaveService, devpack_root=os.environ.get('MDBSAVE_DEVPACK_ROOT')) as coord:
        coord.mdbsave = coord.components[MdbSaveComponent]
        coord.mdbsave_api = MdbSaveApi(
            location='http://localhost:{port}'.format(port=coord.mdbsave.webserver_port())
        )

        mdb = coord.components[Mdb]
        connstr = 'host=localhost port={port} dbname=maildb user={user}'.format(
            port=mdb.port(),
            user='mxback'
        )

        coord.conn = psycopg2.connect(connstr)
        coord.conn.autocommit = True

        coord.start()

        try:
            yield coord
        finally:
            coord.stop()
