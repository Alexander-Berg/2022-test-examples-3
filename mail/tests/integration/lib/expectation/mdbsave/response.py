import json

from mail.notsolitesrv.tests.integration.lib.util.message import make_imap_id, make_mid


def make_mdb_save_response_rcpt_node(id, uid):
    return {
        "id": id,
        "rcpt": {
            "uid": uid,
            "status": "ok",
            "mid": make_mid(),
            "imap_id": make_imap_id(),
            "tid": "1",
            "duplicate": False,
            "folder": {
                "fid": "1",
                "name": "Inbox",
                "type": "",
                "type_code": 0
            },
            "labels": [{"lid": "2", "symbol": ""}]
        }
    }


def make_success_response_body(users):
    sorted_users = list(enumerate(sorted(users.values(), key=lambda user: user.email), start=1))
    result = {"rcpts": []}
    for id, user in sorted_users:
        result["rcpts"].append(make_mdb_save_response_rcpt_node(id=str(id), uid=str(user.uid)))
    return json.dumps(result)
