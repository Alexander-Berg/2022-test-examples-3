# coding: utf-8

from hamcrest import assert_that, equal_to
from http_request import build_http_request_body
from psycopg2.extras import DictCursor


def execute(conn, command):
    cur = conn.cursor(cursor_factory=DictCursor)
    cur.execute(command)
    return cur


def fetch(conn, command):
    cur = execute(conn, command)
    return cur.fetchall()


def fetch_one(conn, command):
    rows = fetch(conn, command)
    assert len(rows) > 0
    return rows[0]


def http_save(env, message):
    body = build_http_request_body(message)
    response = env.mdbsave_api.save(body)
    assert_that(response.status_code, equal_to(200))
    return response.json()


def http_save_and_get_rcpt(env, message, rcpt_id="0"):
    response = http_save(env, message)
    for rcpt in response["rcpts"]:
        if rcpt["id"] == rcpt_id:
            return rcpt["rcpt"]
    raise RuntimeError("rcpt_id={} not found in response={}".format(rcpt_id, response))


def http_save_and_get_mid(env, message, rcpt_id="0"):
    rcpt = http_save_and_get_rcpt(env, message, rcpt_id)
    assert rcpt["status"] == "ok"
    assert rcpt["mid"]

    return rcpt["mid"]


def get_folder_created_by_http_save(env, message, rcpt_id="0"):
    rcpt = http_save_and_get_rcpt(env, message, rcpt_id)
    if "folder" in rcpt:
        return rcpt["status"], rcpt["folder"]["name"], rcpt["folder"]["type_code"]
    else:
        return rcpt["status"], "", ""
