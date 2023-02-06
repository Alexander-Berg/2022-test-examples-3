from mail.devpack.lib.components.fbbdb import FbbDb


def test_fbbdb(coordinator):
    fbbdb = coordinator.components[FbbDb]
    fbbdb.execute("insert into fbb.users values (1, 1000, 'test-fbbdb', 'pg', true);")
    result = fbbdb.query("select uid, suid, login, db, is_corp from fbb.users where uid = 1;")
    assert result == [(1, 1000, 'test-fbbdb', 'pg', True)]
