import pytest
import requests

from crm.space.test.helpers import get_secret_value_by_key
from crm.space.test.ora_client import Oracle


SECRET_IDS = {'robot-space-odyssey': 'sec-01ek232vrzr6r0wv0vpq295xy5',
              'zomb-crmtest': 'sec-01cw6bnk82e7mfs64skw8ntg95'}


def get_session_id(user):
    if user not in SECRET_IDS:
        raise Exception(f'There is no secret for user - {user}')
    return get_secret_value_by_key(SECRET_IDS[user], 'Session_id')


def get_auth_credential(user):
    session_cookie = 'Session_id=' + get_session_id(user)
    headers = {'Cookie': session_cookie}
    response = requests.get('https://crm-test.yandex-team.ru/info', headers=headers, verify=False)
    return {'Session_cookie': session_cookie, 'x-csrf-token': response.cookies.get('x-csrf-token')}


@pytest.fixture(scope="session")
def credential_space_odyssey():
    return get_auth_credential('robot-space-odyssey')


@pytest.fixture(scope="session")
def credential_zomb_crmtest():
    return get_auth_credential('zomb-crmtest')


# TODO: how to set tnsnames.ora file path for connection
@pytest.fixture(scope="class")
def add_managers_to_lift():
    ora = Oracle()
    manager_data = [(16527, 16527, 16, 0, 0, 1, 0, 1, '', None, 50, 31, 0, 'zomb-crmtest', ''),
                    (96003, 96003, 16, 0, 0, 1, 0, 1, '', 16527, 50, 31, 0, 'space-odyssey', 'zomb-crmtest')]
    check_manager_sql = "SELECT ID FROM CRM.LIFT_MANAGER WHERE ID = :1"

    for manager in manager_data:
        res = ora.query(check_manager_sql, [manager[0]])
        if not res:
            bind_ids = [":" + str(i + 1) for i in range(len(manager))]
            manager_sql = """INSERT INTO CRM.LIFT_MANAGER
            (ID, USER_ID, TIER_ID, IS_TEAMLEAD, IS_TIERLEAD, IS_TOP_MANAGER, IS_DUMMY, GEO_ID, DUMMY_MANAGER_NAME,
            TEAMLEAD_ID, CAPACITY, ROLE_ID, IS_SUBTIERLEAD, LOGIN, TEAMLEAD_LOGIN) VALUES (%s)""" % (",".join(bind_ids))
            ora.nonQueryBatch(manager_sql, [manager])


@pytest.fixture(scope="class")
def add_accounts_to_lift():
    ora = Oracle()
    account_ids = [73040022, 72641665, 72867867, 73154834, 72733778, 99592746, 73109423, 100648506, 73281091]
    bind_ids = [":" + str(i + 1) for i in range(len(account_ids))]
    check_account_sql = "SELECT ACCOUNT_ID FROM CRM.LIFT_ACCOUNT WHERE ACCOUNT_ID IN (%s)" % (",".join(bind_ids))

    res = ora.query(check_account_sql, account_ids)
    if len(res) != len(account_ids):
        res = [i[0] for i in res]
        for i in res:
            account_ids.remove(i)

        for account_id in account_ids:
            values = [(account_id, 'name ' + str(account_id), None, None, None, None, None, None, None, 218, None,
                       1, 213, 6, 16, 325, 4, 43, None, 'agencies_login', 'domain' + str(account_id),
                       'Директ: Рекламные кампании(100%)', None, None, 0, 0, account_id, account_id)]
            bind_ids = [":" + str(i + 1) for i in range(len(values[0]))]
            sql = """INSERT INTO CRM.LIFT_ACCOUNT
            (ACCOUNT_ID, NAME, INIT_ANALYST_MANAGER_ID, INIT_ACCOUNT_MANAGER_ID, INIT_SALES_MANAGER_ID,
            NEW_ANALYST_MANAGER_ID, NEW_ACCOUNT_MANAGER_ID, NEW_SALES_MANAGER_ID, RECOMMENDED_SALES_MANAGER_ID,
            INDUSTRY_ID, APPROVAL_ID, DOMAINS_COUNT, GEO_ID, LIFT_STATUS, MONTHS_IN_TIER, PRIORITY, TIER_ID, SUBTIER_ID,
            UNMANAGED_REASON_ID, AGENCIES_LOGINS, MAIN_DOMAINS_LIST, SERVICES, ACCOUNT_COMMENT, MAGIC_FIELD, LTV, SOW,
            AVG_COST_3M, AVG_COST_6M) VALUES(%s)""" % (",".join(bind_ids))
            ora.nonQueryBatch(sql, values)
