# coding=utf-8


import requests


class Furita():

    def __init__(self, host="localhost:5559"):
        self.__host = "http://{host}".format(host=host)

    def ping(self):
        return requests.get("{host}/ping".format(host=self.__host), timeout=0.5)

    def api_list(self, uid=None, suid=None, id=None, detailed=False, type=None):
        url = "{host}/api/list.json?db=pg".format(host=self.__host)
        if uid is not None:
            url = url + "&uid={uid}".format(uid=uid)
        if suid is not None:
            url = url + "&user={suid}".format(suid=suid)
        if id is not None:
            url = url + "&id={id}".format(id=id)
        if detailed is True:
            url = url + "&detailed=1"
        if type is not None:
            url = url + "&type={type}".format(type=type)
        return requests.get(url)

    def api_edit(self, uid, name, params={}):
        url = []
        url.append("{host}/api/edit.json?db=pg".format(host=self.__host))
        url.append("uid={uid}".format(uid=uid))
        url.append("name={name}".format(name=name))
        url.append("letter={letter}".format(letter="nospam" if "letter" not in params else params["letter"]))
        url.append("field1={field1}".format(field1="from" if "field1" not in params else params["field1"]))
        url.append("field2={field2}".format(field2="3" if "field2" not in params else params["field2"]))
        url.append("field3={field3}".format(field3="test@test.ru" if "field3" not in params else params["field3"]))
        url.append("attachment={attachment}".format(attachment="" if "attachment" not in params else params["attachment"]))
        url.append("logic={logic}".format(logic="0" if "logic" not in params else params["logic"]))
        url.append("clicker={clicker}".format(clicker="delete" if "clicker" not in params else params["clicker"]))
        url.append("move_folder={move_folder}".format(move_folder="1" if "move_folder" not in params else params["move_folder"]))
        url.append("move_label={move_label}".format(move_label="1" if "move_label" not in params else params["move_label"]))
        url.append("forward_address={forward_address}".format(forward_address="test@test.ru" if "forward_address" not in params else params["forward_address"]))
        url.append("autoanswer={autoanswer}".format(autoanswer="" if "autoanswer" not in params else params["autoanswer"]))
        url.append("notify_address={notify_address}".format(notify_address="test@test.ru" if "notify_address" not in params else params["notify_address"]))
        url.append("order={order}".format(order="0" if "order" not in params else params["order"]))
        url.append("stop={stop}".format(stop="0" if "stop" not in params else params["stop"]))
        url.append("noconfirm={noconfirm}".format(noconfirm="1" if "noconfirm" not in params else params["noconfirm"]))
        url.append("auth_domain={auth_domain}".format(auth_domain="" if "auth_domain" not in params else params["auth_domain"]))
        url.append("confirm_domain={confirm_domain}".format(confirm_domain="" if "confirm_domain" not in params else params["confirm_domain"]))
        url.append("lang={lang}".format(lang="ru" if "lang" not in params else params["lang"]))
        url.append("from={_from}".format(_from="" if "from" not in params else params["from"]))

        return requests.get("&".join(url))

    def api_remove(self, uid, ids):
        url = "{host}/api/remove.json?db=pg&uid={uid}".format(host=self.__host, uid=uid)
        if len(ids) != 0:
            url += "&id=" + "&id=".join(ids)
        return requests.get(url)

    def api_apply(self, uid, id):
        url = "{host}/api/apply.json?db=pg&uid={uid}&id={id}".format(host=self.__host, uid=uid, id=id)
        return requests.get(url)
