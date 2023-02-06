import datetime


VALUE = 'value'
KEY = 'key'
PUT_PARAMS = dict(i_uid=1, i_key=KEY, i_value=VALUE, i_version='1')
GET_PARAMS = dict(i_uid=1, i_key=KEY, i_version='1')


def replace(d, **kwargs):
    ret = dict(d)
    ret.update(**kwargs)
    return ret


def test_simple_put_and_get(context):
    with context.reflect_db() as db:
        assert db.code.put(**PUT_PARAMS) is True

        assert db.code.get(**GET_PARAMS) == [VALUE]


def test_get_with_another_version(context):
    with context.reflect_db() as db:
        assert db.code.put(**PUT_PARAMS) is True

        assert db.code.get(**GET_PARAMS) == [VALUE]
        assert db.code.get(**replace(GET_PARAMS, i_version='2')) == []


def test_get_empty_value(context):
    with context.reflect_db() as db:
        assert db.code.get(**GET_PARAMS) == []


def test_put_duplicate_value(context):
    with context.reflect_db() as db:
        assert db.code.put(**PUT_PARAMS) is True
        assert db.code.put(**replace(PUT_PARAMS, i_version='2')) is True

        assert db.code.put(**PUT_PARAMS) is False
        assert db.code.put(**replace(PUT_PARAMS, i_version='2')) is False
        assert db.code.put(**replace(PUT_PARAMS, i_value='another_value')) is False


def test_get_with_wrong_version(context):
    with context.reflect_db() as db:
        assert db.code.put(**PUT_PARAMS) is True
        assert db.code.get(**replace(GET_PARAMS, i_version='2')) == []


def test_purge(context):
    with context.reflect_db() as db:
        assert db.code.put(**PUT_PARAMS) is True
        assert db.code.put(**replace(PUT_PARAMS, i_key=KEY*2)) is True
        assert db.code.put(**replace(PUT_PARAMS, i_key=KEY*3)) is True

        user = db.cachedb.cache.select(uid=PUT_PARAMS['i_uid'], key=PUT_PARAMS['i_key'])[0].db_user

        assert db.impl.purge(i_count=1, i_user=user*2, i_cutoff=(datetime.datetime.now() + datetime.timedelta(days=1))) == 0

        assert db.impl.purge(i_count=1, i_user=user, i_cutoff=(datetime.datetime.now() - datetime.timedelta(days=1))) == 0
        assert db.impl.purge(i_count=1, i_user=user, i_cutoff=(datetime.datetime.now() + datetime.timedelta(days=1))) == 1
        assert db.impl.purge(i_count=2, i_user=user, i_cutoff=(datetime.datetime.now() + datetime.timedelta(days=1))) == 2
        assert db.impl.purge(i_count=1, i_user=user, i_cutoff=(datetime.datetime.now() + datetime.timedelta(days=1))) == 0
