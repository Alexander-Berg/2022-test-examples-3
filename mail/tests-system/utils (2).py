import imaplib
import time
import thread
import urllib
import urllib2
import json
import psycopg2
import psycopg2.extras
import random
import string
import uuid
import queries
import requests
from socket import AF_INET6
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
from urlparse import urlparse
import msgpack
from retry import *
from users import *
from urls import *
from nose.tools import *


@retry()
def load_url(url, add_headers={}, post_data=None):
    request = urllib2.Request(url, headers=add_headers, data=post_data)
    resp = urllib2.urlopen(request)
    return resp


def load_json(url, add_headers={}, post_data=None):
    resp = load_url(url, add_headers, post_data).read()
    return json.loads(resp)


def call_xeno_api(url, token, headers={}, post_data=None, post_type="application/json"):
    headers.update({"Authorization": "OAuth " + token})
    if post_data:
        headers.update({"Content-Type": post_type})

    separator = "?"
    if "?" in url:
        separator = "&"
    url += separator + "uuid=" + UUID
    return load_json(url, headers, post_data)


def auth_by_password(user, password):
    url = auth_by_password_url(user, password, X_TOKEN_CLIENT_ID, X_TOKEN_CLIENT_SECRET, DEVICE_ID)
    return load_json(url)


def auth_by_password_ex(user):
    user["client_id"] = X_TOKEN_CLIENT_ID
    user["client_secret"] = X_TOKEN_CLIENT_SECRET
    user["device_id"] = DEVICE_ID
    user["email"] = user["email"] if "email" in user else user["login"]
    user["imap_login"] = user["login"]
    user["imap_password"] = user["password"]
    if "smtp_host" in user:
        user["smtp_login"] = user["smtp_login"] if "smtp_login" in user else user["login"]
        user["smtp_password"] = (
            user["smtp_password"] if "smtp_password" in user else user["password"]
        )
    url = auth_by_password_ex_url(user)
    return load_json(url)


# How to generate in case of expire:
# In browser: https://social-test.yandex.ru/broker2/start?application=mailru-o2-mail&consumer=mail&return_brief_profile=1&retpath=https%3A%2F%2Fmail-test.yandex.ru&scope=userinfo+mail.imap
# Get task id from redirected url after auth
# curl -s "https://api.social-test.yandex.ru/api/task/$TASK_ID" | jq -rc ".token.refresh"
def get_access_token(application, refresh_token):
    resp = load_json(
        get_access_token_url(application), post_data="refresh_token={}".format(refresh_token)
    )
    try:
        return resp["result"]["access_token"]
    except:
        print("access_token not found in response")
        print("response: " + str(resp))


def get_task_id(email, application, external_uid, refresh_token, access_token):
    expired_token_time = 2147483647  # 2^31 - 1 max value in social broker
    params = {
        "token_scope": "all",
        "application_name": application,
        "profile_userid": external_uid,
        "token_expired": expired_token_time,
        "token_value": access_token,
        "refresh_token_value": refresh_token,
        "profile_email": email,
    }
    resp = load_json(create_task_url(), post_data=urllib.urlencode(params))
    return resp["task_id"]


def auth_by_oauth(email, application, external_uid, refresh_token, device):
    access_token = get_access_token(application, refresh_token)
    task_id = get_task_id(email, application, external_uid, refresh_token, access_token)
    return load_json(auth_by_oauth_url(task_id, X_TOKEN_CLIENT_ID, X_TOKEN_CLIENT_SECRET, device))


# retry auth due to frequent connection timeouts from imap.mail.ru
class AuthError(Exception):
    def __init__(self, resp):
        self.resp = resp


@retry(tries=5, delay=1)
def check_auth_failed(user, expected_status, expected_phrase, device=DEVICE_ID):
    with assert_raises(AuthError) as ae:
        _authorize(user, device)
    auth_error = ae.exception
    ok_(
        auth_error.resp["status"]["status"] == expected_status,
        auth_error.resp["status"]["phrase"] == expected_phrase,
    )
    return auth_error.resp


@retry(tries=5, delay=1)
def check_auth(user, device=DEVICE_ID):
    _authorize(user, device)


@retry(tries=5, delay=1)
def auth(user, device_id=DEVICE_ID):
    resp = _authorize(user, device_id)
    assert "uid" in resp, resp
    xtoken = resp["xtoken"]
    resp = get_auth_token_by_xtoken(xtoken)
    assert len(resp["access_token"]) > 0
    ret = {"uid": int(resp["uid"]), "access_token": resp["access_token"], "xtoken": xtoken}
    return ret


def _authorize(user, device_id):
    if user["auth_type"] is AUTH_TYPE_OAUTH:
        resp = auth_by_oauth(
            user["login"],
            user["application"],
            user["external_uid"],
            user["refresh_token"],
            device_id,
        )
    elif user["auth_type"] is AUTH_TYPE_PASSWORD:
        resp = auth_by_password(user["login"], user["password"])
    else:
        resp = auth_by_password_ex(user)

    if resp["status"]["status"] != 1:
        raise AuthError(resp)
    return resp


def get_auth_token_by_xtoken(xtoken):
    url = auth_token_url()
    post = "grant_type={}&client_id={}&client_secret={}&access_token={}&device_id={}".format(
        "x-token", AUTH_TOKEN_CLIENT_ID, AUTH_TOKEN_CLIENT_SECRET, xtoken, DEVICE_ID
    )
    return load_json(url, post_data=post)


def gen_message():
    msg_id = str(uuid.uuid4())
    msg = open("sample_message.txt", "r").read()
    msg = "Message-Id: {}\r\n".format(msg_id) + msg
    return msg, msg_id


def append_message(mailbox, folder="inbox", flags="", receive_date=None):
    if not receive_date:
        receive_date = imaplib.Time2Internaldate(time.time())
    msg, msg_id = gen_message()
    mailbox.append(folder, flags, receive_date, msg)
    return msg_id


def store_draft(xtoken, draft_base_mid=None, store_fid=None, attachments_ids=None):
    headers = {
        "X-Original-Host": "verstka9-qa.yandex.ru",
        "X-Request-Id": "1412408e465edf207a17a4a09b9837e6",
        "X-Real-IP": "2a02:6b8:0:1a72::3ac",
    }

    post_data = {
        "to": "devnull@yandex.ru",
        "subj": "Hello",
        "send": "I am Victor Victorovich.",
    }

    if draft_base_mid is not None:
        post_data["draft_base"] = draft_base_mid

    if store_fid is not None:
        post_data["store_fid"] = store_fid

    if attachments_ids is not None:
        post_data["att_ids"] = attachments_ids

    response = call_xeno_api(store_url(), xtoken, headers=headers, post_data=json.dumps(post_data))
    if response["status"]["status"] != 1:
        raise Exception("store draft bad status: {}".format(response["status"]["status"]))

    return response


def send(
    auth_token, mail_from, to="devnull@yandex.ru", text="Hot dog", subj="Hello", draft_base_mid=None
):
    headers = {
        "X-Original-Host": "verstka9-qa.yandex.ru",
        "X-Request-Id": "1412408e465edf207a17a4a09b9837e6",
        "X-Real-IP": "2a02:6b8:0:1a72::3ac",
    }

    post_data = {"from_mailbox": mail_from, "to": to, "subj": subj, "send": text}

    if draft_base_mid is not None:
        post_data["draft_base"] = draft_base_mid

    return call_xeno_api(send_url(), auth_token, headers=headers, post_data=json.dumps(post_data))


def get_user_shard_id(uid):
    resp = load_json(sharpei_url(uid))
    return str(resp["id"])


def get_bucket_id(shard_id):
    resp = load_json(acquired_buckets_url())
    for bucket in resp:
        for shard in bucket["shards"]:
            if shard["id"] == shard_id:
                return bucket["bucket_id"]
    raise Exception("bucket not found for shard '{}'".format(shard_id))


def get_connection_string(uid):
    resp = load_json(sharpei_url(uid))
    shard = resp["addrs"][0]
    conn = "host={} port={} dbname={} user={}".format(
        shard["host"], shard["port"], shard["dbname"], queries.DB_USER
    )
    return conn


@retry()
def get_connection(uid):
    conn = psycopg2.connect(get_connection_string(uid))
    conn.autocommit = True
    return conn


def get_cursor(conn):
    return conn.cursor(cursor_factory=psycopg2.extras.DictCursor)


def execute(cursor, command):
    cursor.execute(command)


def fetch(cursor, command):
    cursor.execute(command)
    return cursor.fetchall()


def fetch_one(cursor, command):
    rows = fetch(cursor, command)
    if len(rows) == 0:
        raise Exception("fetch one error: empty response for command '{}'".format(command))
    return rows[0]


def dump_mailish_folders(folders):
    return ", ".join(["{} {}".format(folder["fid"], folder["imap_path"]) for folder in folders])


def dump_messages(messages):
    return ", ".join(
        "msg_id={} fid={}".format(message["msg_id"], message["fid"]) for message in messages
    )


def extract_path(url):
    url = url.replace("https://", "").replace("http://", "")
    path_begin = url.find("/")
    if path_begin == -1:
        return ""
    args_begin = url.find("?")
    return url[path_begin:] if args_begin == -1 else url[path_begin:args_begin]


def is_controller_loaded(uid):
    controllers = load_json(list_controllers_url())["controllers"]
    for controller in controllers:
        if controller["uid"] == str(uid):
            return True
    return False


@retry(tries=30)
def check_controller_loaded(uid):
    if not is_controller_loaded(uid):
        raise Exception("controller not loaded, uid={}".format(uid))


@retry(tries=30)
def check_controller_not_loaded(uid):
    if is_controller_loaded(uid):
        raise Exception("controller loaded for uid={}".format(uid))


def upload_attachment(xtoken, file_path):
    headers = {"Authorization": "OAuth " + xtoken}
    files = {"attachment": open(file_path, "rb")}
    resp = requests.post(upload_url(UUID), headers=headers, files=files).json()
    if resp["status"]["status"] != 1:
        raise Exception("uplodad attachment bad status: {}".format(resp["status"]["status"]))
    return resp["id"]


def make_karma_changed_event_body(uid, karma_value):
    event = {
        "comment": "karma.changed",
        "karma": str(karma_value),
        "name": "account.changed",
        "timestamp": 1518803070,
        "uid": str(uid),
    }

    message = [
        "update_karma",
        "suid",
        "passport-stream",
        "account.changed",
        "LCN",
        "session",
        {},
        json.dumps(event),
    ]

    return msgpack.packb(message)


def update_karma(uid, karma_value):
    body = make_karma_changed_event_body(uid, karma_value)
    response = load_url(process_passport_events_url(), post_data=body).read()
    if response != "{}":
        raise Exception("update_karma error: bad response: {}".format(response))


def messages(auth_token, fid, first=0, last=1):
    body = json.dumps({"requests": [{"fid": fid, "first": first, "last": last}]})
    return call_xeno_api(messages_url(), auth_token, post_data=body)


def get_folder_mids(uid, fid, cursor):
    messages = fetch(cursor, queries.mailish_messages_by_fid(uid, fid))
    return map(lambda resp: str(resp[-2]), messages)


@retry(tries=30)
def check_folder_mids_by_seen_status(uid, fid, seen, cursor, mids_to_check):
    messages = fetch(cursor, queries.messages_mids_by_seen_status(uid, fid, seen))
    mids = [str(message["mid"]) for message in messages]
    for mid in mids_to_check:
        ok_(mid in mids, msg="mid {} not found in {}".format(mid, mids))
    return map(lambda resp: str(resp[0]), messages)


def api_mops_mark(uid, status, mids):
    body = urllib.urlencode({"uid": uid, "status": status, "mids": ",".join(mids)})
    return load_json(api_mops_mark_url(), post_data=body)


def api_mops_move(uid, dest_fid, mids):
    body = urllib.urlencode({"uid": uid, "fid": dest_fid, "mids": ",".join(mids)})
    return load_json(api_mops_move_url(), post_data=body)


def api_mops_remove(uid, mids):
    body = urllib.urlencode({"uid": uid, "mids": ",".join(mids)})
    return load_json(api_mops_remove_url(), post_data=body)


def api_mops_create_folder(uid, name, parent_fid=None, symbol=None):
    params = {"uid": uid, "name": name}
    if parent_fid is not None:
        params["parent_fid"] = parent_fid
    if symbol is not None:
        params["symbol"] = symbol
    return load_json(api_mops_create_folder_url(), post_data=urllib.urlencode(params))


def api_mops_update_folder(uid, fid, name, parent_fid=None):
    params = {"uid": uid, "fid": fid, "name": name}
    if parent_fid is not None:
        params["parent_fid"] = parent_fid
    return load_json(api_mops_update_folder_url(), post_data=urllib.urlencode(params))


def api_mops_delete_folder(uid, fid):
    body = urllib.urlencode({"uid": uid, "fid": fid})
    return load_json(api_mops_delete_folder_url(), post_data=body)


def api_mops_create_label(uid, name, color, label_type, get_or_create=False):
    url = api_mops_get_or_create_label_url() if get_or_create else api_mops_create_label_url()
    body = urllib.urlencode({"uid": uid, "name": name, "color": color, "type": label_type})
    return load_json(url, post_data=body)


def api_mops_create_label_by_symbol(uid, symbol, get_or_create=False):
    url = (
        api_mops_get_or_create_label_by_symbol_url()
        if get_or_create
        else api_mops_create_label_by_symbol_url()
    )
    body = urllib.urlencode({"uid": uid, "symbol": symbol})
    return load_json(url, post_data=body)


def api_mops_label(uid, mids, lids, mark=True):
    url = api_mops_label_url() if mark else api_mops_unlabel_url()
    body = urllib.urlencode({"uid": uid, "mids": ",".join(mids), "lids": ",".join(lids)})
    return load_json(url, post_data=body)


def api_mops_update_label(uid, lid, name=None, color=None):
    params = {"uid": uid, "lid": lid}
    if name:
        params["name"] = name
    if color:
        params["color"] = color

    return load_json(api_mops_update_label_url(), post_data=urllib.urlencode(params))


def api_mops_delete_label(uid, lid):
    body = urllib.urlencode({"uid": uid, "lid": lid})
    return load_json(api_mops_delete_label_url(), post_data=body)


def api_mops_set_folder_symbol(uid, fid, symbol=None):
    params = {"uid": uid, "fid": fid}
    if symbol:
        params["symbol"] = symbol
    return load_json(api_mops_set_folder_symbol_url(), post_data=urllib.urlencode(params))


def api_sendbernar_save(uid, fid, old_mid=None):
    body, msg_id = gen_message()
    return load_json(api_sendbernar_save_url(uid, fid, old_mid), post_data=body)


class Cache(HTTPServer):
    address_family = AF_INET6

    class Handler(BaseHTTPRequestHandler):
        def do_POST(self):
            body = self.read_body()
            req_data = dict(param.split("=") for param in body.split("&")) if len(body) > 0 else {}
            req = urlparse(self.path)
            req_data["path"] = req.path
            self.server.queries.append(req_data)

            resp_body = "Not found"
            resp_status = 404

            if req.path[:4] == "/get":
                if req_data["key"] in self.server.cache:
                    resp_status = 200
                    resp_body = json.dumps(self.server.cache[req_data["key"]])
                else:
                    resp_status = 204
                    resp_body = "OK"
            elif req.path[:4] == "/set":
                self.server.cache[req_data["key"]] = {
                    "value": req_data["value"],
                    "ttl": int(req_data["ttl"]),
                }
                resp_status = 200
                resp_body = "OK"

            self.send_response(resp_status)
            self.send_header("Content-Length", len(resp_body))
            self.end_headers()
            self.wfile.write(resp_body)

        def read_body(self):
            content_len = int(self.headers.getheader("content-length", 0))
            return self.rfile.read(content_len)

        def log_message(self, *args):
            pass

    def __init__(self, port):
        HTTPServer.__init__(self, ("", port), Cache.Handler)
        self.queries = []
        self.cache = {}

    def start(self):
        thread.start_new_thread(self.serve_forever, ())

    def make_key(self, oauth_app, refresh_token):
        return urllib.quote_plus("{}${}".format(oauth_app, refresh_token))

    def has_query(self, path, key):
        for query in self.queries:
            if query["key"] == key:
                if query["path"].startswith(path):
                    return True
        return False

    def reset_queries(self):
        self.queries = []


def get_fid_for_tab(tab):
    mapping = {"relevant": -10, "news": -11, "social": -12}
    return mapping[tab]
