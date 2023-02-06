import json as jsn

from tests_common.pytest_bdd import given
from mail.pypg.pypg.common import transaction, qexec
from mail.pypg.pypg.query_conf import load_from_my_file

QUERIES = load_from_my_file(__file__)


def get_dsn(context):
    return context.config['fbbdb']


@given(u'"{user_name:w}" frozen in blackbox')
def step_given_user_frozen_in_blackbox(context, user_name):
    uid = context.get_user(user_name).uid
    user_info = {"users": [
        {
            "id": uid,
            "uid": {
                "value": uid,
                "lite": False,
                "hosted": False
            },
            "login": user_name,
            "have_password": True,
            "have_hint": True,
            "karma": {"value": 0},
            "karma_status": {"value": 0},
            "dbfields": {"subscription.suid.2": ""},
            "attributes": {"203": "2"},
            "address-list": []
        }
    ]}

    with transaction(get_dsn(context)) as conn:
        qexec(
            conn,
            QUERIES.update_user,
            userinfo_response=jsn.dumps(user_info),
            uid=uid,
        )
