from typing import Union, Tuple

from contextlib import contextmanager


def user(login: str, telegram=None, work_phone: int = None, is_marty=False):
    telegram = telegram or login
    login = login.lower()
    telegram = telegram.lower()
    return {
        'department_group': {
            'url': 'yandex_mnt_sa_runtime_mon' if is_marty else 'yandex_test_staff_group',
            'name': 'Служба выполнения регламентных операций' if is_marty else 'Тестовая группа стафф',
        },
        'work_phone': work_phone or 12345,
        'official': {
            'is_dismissed': False
        },
        'accounts': [
            {'value': f'{telegram}', 'type': 'telegram', 'private': False, 'value_lower': f'{telegram.lower()}', 'id': 12345},
        ],
        'login': login,
        'id': 12345,
        '_telegram': telegram
    }


@contextmanager
def temp_users(context, *user_datas: Union[str, dict]):
    logins = set()
    usernames = set()
    context.auth._by_username = context.auth._by_username or {}
    context.auth._by_login = context.auth._by_login

    for data in user_datas:
        if isinstance(data, str):
            login, telegram = data, data

            login = login.lower()
            telegram = telegram.lower()

            person = user(login, telegram)
        else:
            login, telegram = data['login'], data['_telegram']
            person = data

        logins.add(login)
        usernames.add(telegram)

        context.auth._by_username[telegram] = person
        context.auth._by_login[login] = person

    try:
        yield
    finally:
        for login in logins:
            context.auth._by_login.pop(login, None)

        for telegram in usernames:
            context.auth._by_username.pop(telegram, None)
