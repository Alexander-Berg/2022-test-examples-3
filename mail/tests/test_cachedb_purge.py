import asyncio

from mail.puli.lib.dbtask.base_task import create_task


def test_cachedb_purge(context):
    context.cachedb.execute('''
        INSERT INTO cachedb.cache (uid, key, value, version, created, db_user)
        VALUES ('1', 'key1', 'value', 'version', '2020-04-03 17:35:36.03562', 'sendbernar')
             , ('1', 'key2', 'value', 'version', '2020-04-03 17:35:36.03562', 'sendbernar')

             , ('1', 'key3', 'value', 'version', '2020-04-03 17:35:36.03562', 'xeno')
             , ('1', 'key4', 'value', 'version', '2020-04-03 17:35:36.03562', 'xeno')
    ''')

    task = create_task('cachedb_purge', context.config)
    asyncio.get_event_loop().run_until_complete(task.run('cachedb'))

    ret = context.cachedb.query('SELECT count(*) FROM cachedb.cache')
    assert int(ret[0][0]) == 0
