# -*- coding: utf-8 -*-
from test.fixtures.users import default_user

NORMAL_JSON = {
    "connection_id": "t:436322573",
    "error": "OK",
    "have_hint": False,
    "have_password": True,
    "karma": {
        "value": 0
    },
    "karma_status": {
        "value": 0
    },
    "login": "vantuz10050020170411",
    "oauth": {
        "client_ctime": "2015-08-13 17:05:47",
        "client_homepage": "",
        "client_icon": "https://avatars.mds.yandex.net/get-oauth/40350/e1af730960ff4d9ba7b1a646eed7ca0d-bb20e212f0af43bs8fd9abada2e1bb52/normal",
        "client_id": "e1af730960ff4d9ja7b1h646fed7sa0d",
        "client_is_yandex": True,
        "client_name": u"Яндекс.Диск",
        "ctime": "2017-02-08 17:07:03",
        "device_id": "DFBDE016-32ED-4S5A-9H9F-D1R497E64006",
        "device_name": u"iPhone (Иван)",
        "expire_time": "2018-02-08 17:07:03",
        "is_ttl_refreshable": True,
        "issue_time": "2017-02-08 17:07:03",
        "meta": "",
        "scope": "login:info mobile:all login:avatar cloud_api.data:user_data cloud_api.data:app_data yadisk:all",
        "token_id": "436322497",
        "uid": default_user.uid
    },
    "status": {
        "id": 0,
        "value": "VALID"
    },
    "uid": {
        "hosted": False,
        "lite": False,
        "value": default_user.uid
    }
}
