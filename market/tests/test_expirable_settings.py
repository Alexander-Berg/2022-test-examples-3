from datetime import datetime, timedelta
import pytest

from market.pylibrary.mindexerlib.expirable_settings import ExpirableSettings


ZK_PREFIX = '/test_expirable_settings'


@pytest.fixture
def settings(reusable_zk):
    return ExpirableSettings(zk=reusable_zk, prefix=ZK_PREFIX)


def test_expiry_date(settings):
    """ Settings should expire on given date """
    settings.set('key', 'value', expiry_date=datetime.now() + timedelta(hours=1))
    assert settings.get('key') == 'value'

    settings.set('key', 'value', expiry_date=datetime.now() - timedelta(hours=1))
    assert settings.get('key') is None


def test_ttl(settings):
    """ Settings should expire with given TTL """
    settings.set('key', 'value', ttl=timedelta(hours=1))
    assert settings.get('key') == 'value'

    settings.set('key', 'value', ttl=-timedelta(hours=1))
    assert settings.get('key') is None


def test_missing_key(settings):
    """ Returns None for unknown key """
    assert settings.get('non-key') is None


def test_types(settings):
    """ Primitive types are supported """
    settings.set('int', 10, ttl=timedelta(hours=1))
    settings.set('bool', True, ttl=timedelta(hours=1))
    settings.set('float', 2.5, ttl=timedelta(hours=1))
    settings.set('str', 'quick fox', ttl=timedelta(hours=1))

    assert settings.get('int') == 10
    assert settings.get('bool') is True
    assert settings.get('float') == 2.5
    assert settings.get('str') == 'quick fox'


def test_persistence(reusable_zk):
    """ Data should be stored persistently """
    write_settings = ExpirableSettings(zk=reusable_zk, prefix='test_expirable_settings')
    write_settings.set('key', 'value', ttl=timedelta(hours=1))

    read_settings = ExpirableSettings(zk=reusable_zk, prefix='test_expirable_settings')
    assert read_settings.get('key') == 'value'


def test_other_prefix(reusable_zk):
    """ Data should be stored persistently """
    write_settings = ExpirableSettings(zk=reusable_zk, prefix='test_expirable_settings')
    write_settings.set('key', 'value', ttl=timedelta(hours=1))

    read_settings = ExpirableSettings(zk=reusable_zk, prefix='test_expirable_settings/inner')
    assert read_settings.get('key') is None


def test_nested_key(settings):
    """ Nested keys should be supported """
    settings.set('grandkey/key/subkey', 'value', ttl=timedelta(hours=1))
    assert settings.get('grandkey/key/subkey') == 'value'


def test_ttl_or_expiry_date_must_be_set(settings):
    """ Parameters validation """
    with pytest.raises(AssertionError):
        settings.set('key', 'value')


def test_zero_ttl_is_ok(settings):
    """ ttl=0 may be used to drop downtime """
    settings.set('key', 'value', ttl=timedelta(hours=0))


def test_get_expiry_date(settings):
    """ Check getting expiry date """
    settings.set('key', 'value', expiry_date=datetime(year=2030, month=7, day=8))

    assert settings.get_expiry_date('key') == datetime(year=2030, month=7, day=8)


def test_cant_get_past_expiry_date(settings):
    """ Check that we cannot get expiry date in the past """
    settings.set('key', 'value', expiry_date=datetime(year=2010, month=7, day=8))

    assert not settings.get_expiry_date('key')


def test_purge(settings, reusable_zk):
    """ Check that expired keys go away after purge """
    settings.set('actual', 'value', ttl=timedelta(hours=1))
    settings.set('expired', 'value', ttl=timedelta(hours=0))
    settings.set('subkey/expired', 'value', ttl=timedelta(hours=0))
    settings.set('subkey/not_expired', 'value', ttl=timedelta(hours=1))
    settings.set('subkey/expired/grand_child', 'value', ttl=timedelta(hours=1))

    settings.purge()

    assert reusable_zk.get_children(ZK_PREFIX) == ['actual', 'subkey']
    assert reusable_zk.get_children(ZK_PREFIX + '/' + 'subkey') == ['not_expired']


def test_items(settings, reusable_zk):
    """ Check iteration over items """
    settings.set('actual', 'value', ttl=timedelta(hours=1))
    settings.set('expired', 'value', ttl=timedelta(hours=0))
    settings.set('subkey/actual', 'value', ttl=timedelta(hours=1))
    settings.set('subkey/expired', 'value', ttl=timedelta(hours=0))

    items = {k: v for k, v in list(settings.items())}

    now = datetime.now()
    assert items['actual'] > now
    assert items['expired'] <= now
    assert items['subkey/actual'] > now
    assert items['subkey/expired'] <= now
