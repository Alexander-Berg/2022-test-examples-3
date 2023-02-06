#! /usr/bin/python

import thread
import requests
import json
from socket import AF_INET6, gethostname
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
from urlparse import urlparse
from nose.tools import *
from utils import *
from users import *
from urls import *
from lock_listener import LockListener
import queries


class WebServer(HTTPServer):
    address_family = AF_INET6

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            self.process_query()

        def do_POST(self):
            self.process_query()

        def process_query(self):
            req = urlparse(self.path)
            params = (
                dict(param.split("=") for param in req.query.split("&"))
                if len(req.query) > 0
                else {}
            )
            self.server.queries.append(
                {"path": req.path, "params": params, "headers": self.headers}
            )
            req = "OK"
            self.send_response(200)
            self.send_header("Content-Length", len(req))
            self.send_header("x-mmapi-status", "ok")
            self.end_headers()
            self.wfile.write(req)

        def log_message(self, fmt, *args):
            print(fmt % args)

    def __init__(self, port):
        HTTPServer.__init__(self, ("", port), WebServer.Handler)
        self.queries = []

    def start(self):
        thread.start_new_thread(self.serve_forever, ())


def setup():
    global lock_listener
    lock_listener = LockListener()
    lock_listener.start()

    global fake_mobile_api
    fake_mobile_api = WebServer(FAKE_NODE_INFO["mobile_api_port"])
    fake_mobile_api.start()

    global fake_internal_api
    fake_internal_api = WebServer(FAKE_NODE_INFO["internal_port"])
    fake_internal_api.start()

    if "common_mailru_user" not in TEST_USERS or "not_owned_user" not in TEST_USERS:
        raise Exception("test users not found")

    global not_owned_user
    not_owned_user = TEST_USERS["not_owned_user"]
    resp = auth(not_owned_user)
    not_owned_user["uid"] = resp["uid"]
    not_owned_user["conn"] = get_connection(not_owned_user["uid"])
    not_owned_user["cur"] = get_cursor(not_owned_user["conn"])


def teardown():
    lock_listener.stop()

    fake_internal_api.shutdown()
    fake_mobile_api.shutdown()


def setup_fake_mobile_api():
    fake_mobile_api.queries = []


def setup_fake_internal_api():
    fake_internal_api.queries = []


def test_load_user_when_not_owner():
    resp = requests.get(load_user_url(not_owned_user["uid"]))
    eq_(resp.status_code, 406)
    eq_(resp.text, "user not owned by this node")


@with_setup(setup_fake_internal_api)
def test_auth():
    resp = auth(not_owned_user)

    eq_(len(fake_internal_api.queries), 1)
    eq_(fake_internal_api.queries[0]["path"], "/load_user")
    eq_(fake_internal_api.queries[0]["params"]["uid"], str(not_owned_user["uid"]))

    not_owned_user["auth_token"] = resp["access_token"]

    execute(not_owned_user["cur"], queries.update_security_lock(not_owned_user["uid"], False))


@with_setup(setup_fake_mobile_api)
def test_proxy_modify_mobile_api_query():
    headers = {"Authorization": "OAuth " + not_owned_user["auth_token"]}
    url = mark_with_label_url(123, "", 123, True)
    resp = requests.get(url, headers=headers)
    eq_(resp.status_code, 200)
    eq_(len(fake_mobile_api.queries), 1)
    eq_(fake_mobile_api.queries[0]["path"], extract_path(url))


@with_setup(setup_fake_mobile_api)
def test_proxy_mops_api_query():
    url = api_mops_mark_url()
    resp = requests.post(url, data={"uid": not_owned_user["uid"], "status": "read"})
    eq_(resp.status_code, 200)
    eq_(len(fake_mobile_api.queries), 1)
    eq_(fake_mobile_api.queries[0]["path"], extract_path(url))


@with_setup(setup_fake_mobile_api)
def test_proxy_sendbernar_api_query():
    url = api_sendbernar_save_url(not_owned_user["uid"], "1", 123)
    resp = requests.post(url, data={})
    eq_(resp.status_code, 200)
    eq_(len(fake_mobile_api.queries), 1)
    eq_(fake_mobile_api.queries[0]["path"], extract_path(url))


@with_setup(setup_fake_internal_api)
def test_proxy_karma_changed_event():
    url = process_passport_events_url()
    body = make_karma_changed_event_body(not_owned_user["uid"], 0)
    resp = requests.post(url, data=body)
    eq_(resp.status_code, 200)
    eq_(len(fake_internal_api.queries), 1)
    eq_(fake_internal_api.queries[0]["path"], extract_path(url))


@with_setup(setup_fake_mobile_api)
def test_proxy_when_no_auth_data():
    resp = requests.get(mark_with_label_url(123, "", 123, True))
    eq_(len(fake_mobile_api.queries), 0)
    eq_(resp.status_code, 200)
    resp = json.loads(resp.text)
    eq_(resp["status"], 3)


@with_setup(setup_fake_mobile_api)
def test_proxy_with_wrong_auth_data():
    headers = {"Authorization": "OAuth 123"}
    resp = requests.get(mark_with_label_url(123, "", 123, True), headers=headers)
    eq_(len(fake_mobile_api.queries), 0)
    eq_(resp.status_code, 200)
    resp = json.loads(resp.text)
    eq_(resp["status"], 3)


@with_setup(setup_fake_mobile_api)
def test_proxy_headers():
    headers = {"Authorization": "OAuth " + not_owned_user["auth_token"], "Host": "fakehost"}
    url = mark_with_label_url(123, "", 123, True)
    resp = requests.get(url, headers=headers)
    eq_(resp.status_code, 200)
    eq_(len(fake_mobile_api.queries), 1)
    eq_(fake_mobile_api.queries[0]["path"], extract_path(url))
    eq_(resp.headers["x-mmapi-status"], "ok")
    assert fake_internal_api.queries[0]["headers"].getheader("host") != "fakehost"
