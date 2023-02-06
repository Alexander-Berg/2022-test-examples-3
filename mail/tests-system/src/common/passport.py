import xml.etree.ElementTree as ET
from .http import load_url, load_json, urlencode


BLACKBOX_URL = "http://pass-test.yandex.ru"
SOCIAL_URL = "https://social-test.yandex.ru"


def userinfo_by_login(login):
    resp = load_url(
        BLACKBOX_URL
        + "/blackbox?userip=127.0.0.1&method=userinfo&dbfields=subscription.suid.2&login="
        + login
    ).read()

    root = ET.fromstring(resp)
    dbfields = root.findall("dbfield")
    suid = None
    for field in dbfields:
        if field.attrib["id"] == "subscription.suid.2":
            suid = field.text
    return {"uid": int(root.find("uid").text), "suid": int(suid), "login": login}


def create_social_task(email, access_token, refresh_token, expires_in, app_name):
    post_data = {
        "application_name": app_name,
        "token_value": access_token,
        "token_expired": expires_in,
        "token_scope": "all",
        "refresh_token_value": refresh_token,
        "profile_userid": email,
        "profile_email": email,
    }
    resp = load_json(
        SOCIAL_URL + "/brokerapi/test/create_task?consumer=mail", post_data=urlencode(post_data)
    )
    return resp["task_id"]


def get_access_token(application, refresh_token):
    url = "{}/proxy2/application/{}/refresh_token".format(SOCIAL_URL, application)
    resp = load_json(url, post_data="refresh_token={}".format(refresh_token))
    return resp["result"]


def get_social_task_id(login, application, refresh_token):
    token_data = get_access_token(application, refresh_token)
    return create_social_task(
        login, token_data["access_token"], refresh_token, token_data["expires_in"], application
    )
