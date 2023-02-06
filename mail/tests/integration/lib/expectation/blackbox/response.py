import json

from mail.notsolitesrv.tests.integration.lib.util.user import User


def make_success_body_response(user: User):
    response = {
        "users": [
            {
                "address-list": [{
                    "born-date": "2014-02-13 15:12:09",
                    "native": True,
                    "unsafe": False,
                    "silent": False,
                    "rpop": False,
                    "default": True,
                    "validated": True,
                    "address": user.email
                }],
                "attributes": {},
                "id": str(user.uid),
                "uid": {
                    "hosted": False,
                    "lite": False,
                    "value": str(user.uid)
                },
                "login": user.login,
                "have_password": True,
                "have_hint": True,
                "karma": {"value": 0},
                "karma_status": {"value": 0},
                "dbfields": {
                    "userphones.confirmed.uid": "",
                    "account_info.reg_date.uid": "2014-02-13 15:12:09",
                    "account_info.lang.uid": "ru",
                    "account_info.fio.uid": " ".join([user.login, user.login]),
                    "account_info.country.uid": "ru"
                }
            }
        ]
    }
    if user.org_id is not None:
        response["users"][0]["attributes"]["1031"] = user.org_id
    if not user.is_mailish:
        dbfields = response["users"][0]["dbfields"]
        dbfields["subscription.suid.-"] = str(user.suid)
        dbfields["subscription.login_rule.-"] = "1"
        dbfields["subscription.login.-"] = user.login
    return json.dumps(response)


def make_server_error_body_response():
    return json.dumps({
        "error": "server error",
        "exception": {
            "id": 2,
            "value": "SERVER_ERROR"
        }
    })
