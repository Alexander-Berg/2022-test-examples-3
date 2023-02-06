#! /usr/bin/env python3

import base64

from sanic import Sanic
from sanic.response import json, text


app = Sanic()


def get_uid_from_username(username):
    login = username.split("@")[0]
    return login.split("-")[-1]


@app.post("/token")
async def token(request):
    uname = request.form.get("username")
    return json({"access_token": base64.urlsafe_b64encode(get_uid_from_username(uname).encode())})


xml_template = """<?xml version="1.0" encoding="UTF-8"?>
<doc>
<OAuth>
<uid>{uid}</uid>
<token_id>7140748</token_id>
<device_id></device_id>
<device_name></device_name>
<scope>mobmail:all</scope>
<ctime>2019-04-06 23:12:36</ctime>
<issue_time>2019-04-06 23:12:36</issue_time>
<expire_time>2020-04-05 23:12:36</expire_time>
<is_ttl_refreshable>1</is_ttl_refreshable>
<client_id>d13a8fc2f5194983b332b027a042bf47</client_id>
<client_name>Collectors</client_name>
<client_icon></client_icon>
<client_homepage></client_homepage>
<client_ctime>2019-04-03 19:02:12</client_ctime>
<client_is_yandex>0</client_is_yandex>
<xtoken_id></xtoken_id>
<meta></meta>
</OAuth>
<uid hosted="0">{uid}</uid>
<login>{login}</login>
<have_password>1</have_password>
<have_hint>1</have_hint>
<karma confirmed="0">0</karma>
<karma_status>0</karma_status>
<address-list>
<address validated="1" default="1" rpop="0" silent="0" unsafe="0" native="1" born-date="2014-02-13 15:12:09">{login}@yandex.ru</address>
</address-list>
<status id="0">VALID</status>
<error>OK</error>
<connection_id>t:7140748</connection_id>
</doc>"""


@app.get("/blackbox")
@app.post("/blackbox")
async def token(request):
    method = request.args["method"][0]
    uid = ""
    login = ""
    if method == "oauth":
        token = request.args["oauth_token"][0]
        uid = base64.urlsafe_b64decode(token).decode()
        login = "new-collector-" + uid
    elif method == "userinfo":
        if "suid" in request.args:
            uid = request.args["suid"][0]
            login = "new-collector-" + uid
        elif "uid" in request.args:
            uid = request.args["uid"][0]
            login = "new-collector-" + uid
        elif "login" in request.args:
            login = request.args["login"][0].split("@")[0]
            uid = get_uid_from_username(login)

    return text(xml_template.format(uid=uid, login=login))


if __name__ == "__main__":
    app.run(host="::", port=8031, access_log=False)
