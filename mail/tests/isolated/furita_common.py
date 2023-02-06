# coding=utf-8


from parse import parse
import yatest.common


def get_path(resource):
    """ Возвращаем путь к ресурсу (это то, что указано в секции DATA в ya.make) """
    path = yatest.common.source_path(resource).split("/")
    path.pop()
    result = "/".join(path)
    return result


""""
def get_uid(context, user_name):
    return context.users[user_name] if user_name in context.users else None
"""


def get_rule(furita, uid, rule_name, type="user"):
    """ Получаем одно (и единственное!!) правило по uid и названию правила """
    response = furita.api_list(uid=uid, type=type)
    assert response.status_code == 200
    json_list = response.json()

    assert "rules" in json_list

    for rule in json_list["rules"]:
        if rule["name"] == rule_name:
            return rule

    return None


def get_rules(furita, uid, type="user"):
    """ Получаем все правила пользователя по uid """
    response = furita.api_list(uid=uid, type=type)
    assert response.status_code == 200
    json_list = response.json()

    assert "rules" in json_list
    return json_list["rules"]


def get_names_from_lines(lines):
    """ Получаем названия правил (массив) из нескольких строк в кавычках """
    res = []
    for line in lines.split("\n"):
        rule = parse('"{name}"', line)
        if rule is not None:
            res.append(rule["name"])
    return res
