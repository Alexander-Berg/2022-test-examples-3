import psycopg2
import requests
import time


def wait_webservice_is_ready(url):
    postponed_exception = None
    for _ in range(5):
        time.sleep(0.1)
        try:
            requests.get(url, timeout=0.5)
            return
        except Exception as e:
            postponed_exception = e
    raise postponed_exception


def wait_db_is_ready(db_connstring):
    postponed_exception = None
    for _ in range(10):
        time.sleep(1)
        try:
            psycopg2.connect(db_connstring)
            return
        except Exception as e:
            postponed_exception = e
    raise postponed_exception


def wait_collector_iteration(api, popid, timeout=5):
    deadline = time.monotonic() + timeout
    while deadline >= time.monotonic():
        try:
            rpops = api.list(popid)["rpops"]
            if rpops[0]["last_connect"] == "0":
                continue
            return
        except:
            pass
