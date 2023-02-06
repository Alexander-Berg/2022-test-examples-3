# coding: utf-8

from random import choice
from string import ascii_lowercase

from mail.devpack.lib.components.sharpei import FakeBlackbox

from mdbsave_types import User


def create_user(env):
    name = "".join(choice(ascii_lowercase) for i in range(5))
    blackbox = env.components[FakeBlackbox]
    bb_response = blackbox.register(name + "@yandex.ru")
    if bb_response.status_code != 200:
        raise RuntimeError("bad blackbox response code: {}".format(bb_response.status_code))
    bb_response_dict = bb_response.json()
    if bb_response_dict["status"] != "ok" or "uid" not in bb_response_dict:
        raise RuntimeError("bad blackbox response: {}".format(bb_response_dict))
    uid = bb_response_dict["uid"]
    return User(uid, name, "yandex.ru", name)
