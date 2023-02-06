from travel.avia.shared_flights.lib.python.db.engine import conn_string


def test_conn_string_cluster_id():
    cs = conn_string(
        user='avia',
        password='password',
        database='db',
        cluster_id='1234',
        hostname='',
        port='6543',
    )
    assert cs == r'postgresql+psycopg2://avia:password@c-1234.rw.db.yandex.net/' \
                 r'db?port=6543&sslmode=require&target_session_attrs=read-write'
    cs = conn_string(
        user='avia',
        password='password',
        database='db',
        cluster_id='1234',
        port='6543',
    )
    assert cs == r'postgresql+psycopg2://avia:password@c-1234.rw.db.yandex.net/' \
                 r'db?port=6543&sslmode=require&target_session_attrs=read-write'


def test_conn_string_hostname():
    cs = conn_string(
        user='avia',
        password='password',
        database='db',
        cluster_id='',
        hostname='pgaas.yandex.net',
        port='6543',
    )
    assert cs == r'postgresql+psycopg2://avia:password@pgaas.yandex.net/' \
                 r'db?port=6543&sslmode=require&target_session_attrs=read-write'

    cs = conn_string(
        user='avia',
        password='password',
        database='db',
        hostname='pgaas.yandex.net',
        port='6543',
    )
    assert cs == r'postgresql+psycopg2://avia:password@pgaas.yandex.net/' \
                 r'db?port=6543&sslmode=require&target_session_attrs=read-write'
