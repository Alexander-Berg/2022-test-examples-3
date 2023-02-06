from .http import load_json


def db_user():
    return "imap"


def get_conninfo_by_uid(uid):
    res = load_json(
        "http://sharpei-testing.mail.yandex.net/conninfo?uid={}&mode=master".format(uid)
    )
    address = res["addrs"][0]
    return "host={} port={} dbname={} user={}".format(
        address["host"], address["port"], address["dbname"], db_user()
    )
