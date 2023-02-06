import xml.etree.ElementTree as ET
from .http import load_url, load_json, urlencode
import yaml


BLACKBOX_URL = "http://pass-test.yandex.ru"
OAUTH_URL = "https://oauth-test.yandex.ru"
SOCIAL_URL = "https://api.social-test.yandex.ru"


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


def oauth_credentials():
    return yaml.safe_load(open("etc/collectors/.oauth_client.yml", "r"))["config"]


def get_access_token_data(username, password):
    oauth_data = oauth_credentials()
    post_data = {
        "client_id": oauth_data["oauth_client_id"],
        "client_secret": oauth_data["oauth_client_secret"],
        "grant_type": "password",
        "username": username,
        "password": password,
        "user_ip": "::1",
    }
    return load_json(OAUTH_URL + "/token", post_data=urlencode(post_data))


def create_social_task(token, expires_in, uid, app_name="yandex-mail-collector"):
    post_data = {
        "application_name": app_name,
        "token_value": token,
        "token_expired": expires_in,
        "token_scope": "all",
        "profile_userid": uid,
    }
    return load_json(
        SOCIAL_URL + "/brokerapi/test/create_task?consumer=mail", post_data=urlencode(post_data)
    )


def get_access_token(username, password):
    return get_access_token_data(username, password)["access_token"]


def get_social_task_id(username, password, app_name="yandex-mail-collector"):
    token_data = get_access_token_data(username, password)
    create_social_task_resp = create_social_task(
        token_data["access_token"], token_data["expires_in"], token_data["uid"], app_name=app_name
    )
    return create_social_task_resp["task_id"]
