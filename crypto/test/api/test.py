import collections
import json

from psycopg2 import extras
import requests


def to_underscore_string(role):
    return '_'.join(role)


LOGIN_TABLE = 'api_idm_logins'
LOGIN_ROLE_TABLE = 'api_idm_login_roles'
LOGIN_TEST = 'test'
Role = collections.namedtuple('Role', ['group', 'subgroup', 'subsubgroup'])
ROLE_DIRECT = Role('lab', 'ext', 'direct')
ROLE_DIRECT_UNDERSCORE = to_underscore_string(ROLE_DIRECT)


def get_table(table_name, postgres):
    with postgres.connect() as connection, connection.cursor(cursor_factory=extras.RealDictCursor) as cursor:
        cursor.execute(f"SELECT * FROM {table_name};")
        return cursor.fetchall()


def save_login(login, postgres):
    with postgres.connect() as connection, connection.cursor() as cursor:
        cursor.execute(f"INSERT INTO {LOGIN_TABLE} (login) VALUES ('{login}');")


def save_login_role(login, role, postgres):
    with postgres.connect() as connection, connection.cursor() as cursor:
        cursor.execute(f"INSERT INTO {LOGIN_ROLE_TABLE} (login, role) VALUES ('{login}', '{role}');")


def send_request_get_tables(idm_api, postgres, move, login, role):
    response = requests.post(
        f'http://localhost:{idm_api.port}/idm/{move}',
        data={
            'login': login,
            'role': json.dumps(role._asdict()),
        }).json()

    logins = get_table(LOGIN_TABLE, postgres)
    login_roles = get_table(LOGIN_ROLE_TABLE, postgres)

    return {'response': response, 'logins': logins, 'login_roles': login_roles}


def get(idm_api, move):
    return requests.get(f'http://localhost:{idm_api.port}/idm/{move}').json()


def get_roles_by_login(idm_api, login):
    return get(idm_api, f'roles/{login}')


def test_add_role(idm_api, postgres, clear_db):
    return send_request_get_tables(idm_api, postgres, 'add-role', LOGIN_TEST, ROLE_DIRECT)


def test_add_role_twice(idm_api, postgres, clear_db):
    save_login(LOGIN_TEST, postgres)
    save_login_role(LOGIN_TEST, ROLE_DIRECT_UNDERSCORE, postgres)
    return send_request_get_tables(idm_api, postgres, 'add-role', LOGIN_TEST, ROLE_DIRECT)


def test_add_unknown_role(idm_api, postgres, clear_db):
    role = Role('test', 'test', 'test')
    return send_request_get_tables(idm_api, postgres, 'add-role', LOGIN_TEST, role)


def test_remove_role(idm_api, postgres, clear_db):
    save_login(LOGIN_TEST, postgres)
    save_login_role(LOGIN_TEST, ROLE_DIRECT_UNDERSCORE, postgres)
    return send_request_get_tables(idm_api, postgres, 'remove-role', LOGIN_TEST, ROLE_DIRECT)


def test_remove_not_assigned_role(idm_api, postgres, clear_db):
    save_login(LOGIN_TEST, postgres)
    return send_request_get_tables(idm_api, postgres, 'remove-role', LOGIN_TEST, ROLE_DIRECT)


def test_remove_role_unknown_user(idm_api, postgres, clear_db):
    return send_request_get_tables(idm_api, postgres, 'remove-role', LOGIN_TEST, ROLE_DIRECT)


def test_get_info(idm_api, postgres, clear_db):
    return get(idm_api, 'info')


def test_get_roles(idm_api, postgres, clear_db):
    login_a = 'test-a'
    login_b = 'test-b'

    save_login(login_a, postgres)
    save_login(login_b, postgres)

    save_login_role(login_a, ROLE_DIRECT_UNDERSCORE, postgres)
    save_login_role(login_b, ROLE_DIRECT_UNDERSCORE, postgres)

    return get(idm_api, 'get-roles')


def test_get_roles_by_login(idm_api, postgres, clear_db):
    role_market = Role('lab', 'ext', 'market')

    save_login(LOGIN_TEST, postgres)
    save_login_role(LOGIN_TEST, ROLE_DIRECT_UNDERSCORE, postgres)
    save_login_role(LOGIN_TEST, to_underscore_string(role_market), postgres)

    return get_roles_by_login(idm_api, LOGIN_TEST)


def test_get_roles_by_login_empty(idm_api, postgres, clear_db):
    save_login(LOGIN_TEST, postgres)
    return get_roles_by_login(idm_api, LOGIN_TEST)


def test_get_roles_by_unknown_login(idm_api, postgres, clear_db):
    return get_roles_by_login(idm_api, LOGIN_TEST)
