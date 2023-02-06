from yatest.common import network
from mail.devpack.lib.pg import Postgresql


class TestPostgres(object):
    @classmethod
    def setup_class(cls):
        cls.pm = network.PortManager()
        port = cls.pm.get_port()
        cls.pg = Postgresql(port, "testdb")
        cls.pg.extract_tar()
        cls.pg.initdb()
        cls.pg.start()
        cls.pg.createdb()

    @classmethod
    def teardown_class(cls):
        cls.pg.stop()
        cls.pm.release()

    def test_select(self):
        result = self.pg.query("select 1;")
        assert result == [(1,)]
