import yt.wrapper as yt
from collections import namedtuple
from itertools import product
from datetime import datetime
import json
from pprint import pprint
from operator import itemgetter

CALC_NAME_FULL = 'direct/bonus-cashback-2020'


def order_id(test_id):
    return 1 * 10**8 + test_id


def act_id(test_id):
    return 10**9 + test_id


def agency_id(test_id, sub_id=0):
    return 10**6 + 10 * test_id + sub_id

def agency_name(test_id, sub_id=0):
    return 'agency-{}'.format(agency_id(test_id, sub_id))


def contract_eid(test_id, sub_id=0):
    return '{:06d}/99'.format(test_id * 10 + sub_id)


def contract_id(test_id, sub_id=0):
    return 2 * 10**6 + 10 * test_id + sub_id


def client_id(test_id):
    return 10**8 + test_id


def user_id(test_id):
    return 10**4 + test_id


def counter_id(test_id):
    return 10**5 + test_id


def goal_id(test_id):
    return 10**3 + test_id


def brand_id(test_id, sub_id=0):
    return 2 * 10**5 + 100 * test_id + sub_id


def wallet_cid(test_id):
    return 2 * 10**3 + test_id


def login(test_id):
    """
    >>> login(3)
    'ccc'
    """
    return chr(test_id + ord('a') - 1) * 3


def dt_milliseconds(iso_datetime):
    """
    >>> dt_milliseconds("2020-05-28")
    1590613200000
    """
    return int(datetime.strptime(iso_datetime, "%Y-%m-%d").timestamp() * 1000)


def names_from_schema(schema):
    return [
        x['name'] for x in schema if not x['name'].startswith('__')
    ]


PROGRAM_ID_RMP = 2
PROGRAM_ID_AUTO = 3
PROGRAM_ID_METRIKA = 4
PROGRAM_ID_AUTOTARGETING = 5
PROGRAM_ID_RETARGETING = 6

CATEGORY_ID_RMP = 2
CATEGORY_ID_AUTO = 3
CATEGORY_ID_METRIKA = 4
CATEGORY_ID_AUTOTARGETING = 5
CATEGORY_ID_RETARGETING = 6


CampaignsRecord = namedtuple('CampaignsRecord', [
    'iso_date',
    'pk_cid',
    'type',
    'strategy_name',
    'strategy_data',
    'statusActive',
])

CampaignsSchema = [
    {'name': 'iso_date', 'type': 'string'},
    {'name': 'pk_cid', 'type': 'int64'},
    {'name': 'type', 'type': 'string'},
    {'name': 'strategy_name', 'type': 'string'},
    {'name': 'strategy_data', 'type': 'string'},
    {'name': 'statusActive', 'type': 'string'},
]


GoalsRecord = namedtuple('GoalsRecord', [
    'dt',
    'ClickDirectCampaignID',
    'CounterID',
    'goal_id',
    'goal_type',
    'goal_reaches',
    'counter_visits',
])

GoalsSchema = [
    {'name': 'dt', 'type': 'string'},
    {'name': 'ClickDirectCampaignID', 'type': 'int64'},
    {'name': 'CounterID', 'type': 'uint32'},
    {'name': 'goal_id', 'type': 'int64'},
    {'name': 'goal_type', 'type': 'string'},
    {'name': 'goal_reaches', 'type': 'uint64'},
    {'name': 'counter_visits', 'type': 'uint64'},
]


CountersRecord = namedtuple('CountersRecord', [
    'dt',
    'ClickDirectCampaignID',
    'CounterID',
    'counter_visits',
    'order_clicks',
    'counter_in_settings',
])

CountersSchema = [
    {'name': 'dt', 'type': 'string'},
    {'name': 'ClickDirectCampaignID', 'type': 'uint32'},
    {'name': 'CounterID', 'type': 'uint32'},
    {'name': 'counter_visits', 'type': 'uint64'},
    {'name': 'order_clicks', 'type': 'int64'},
    {'name': 'counter_in_settings', 'type': 'boolean'},
]


SalesDailyRecord = namedtuple('SalesDailyRecord', [
    'dt',
    'act_id',
    'agency_id',
    'agency_name',
    'manager_name',
    'client_name',
    'order_client_id',
    'service_id',
    'currency',
    'ar_commission_type',
    'service_order_id',
    'amount_nds',
    'amount_nsp',
    'amount',
])

SalesDailySchema = [
    {'name': 'dt', 'type': 'string'},
    {'name': 'act_id', 'type': 'int64'},
    {'name': 'agency_id', 'type': 'int64'},
    {'name': 'agency_name', 'type': 'utf8'},
    {'name': 'manager_name', 'type': 'utf8'},
    {'name': 'client_name', 'type': 'utf8'},
    {'name': 'order_client_id', 'type': 'int64'},
    {'name': 'service_id', 'type': 'int64'},
    {'name': 'currency', 'type': 'utf8'},
    {'name': 'ar_commission_type', 'type': 'int64'},
    {'name': 'service_order_id', 'type': 'int64'},
    {'name': 'amount_nds',  'type': 'double'},
    {'name': 'amount_nsp', 'type': 'double'},
    {'name': 'amount', 'type': 'double'},
]


GroupOrderActDivRecord = namedtuple('GroupOrderActDivRecord', [
    'dt',
    'act_id',
    'service_id',
    'group_service_order_id',
    'service_order_id',
    'client_id',
    'inv_amount',
    'order_amount',
])

GroupOrderActDivSchema = [
    {'name': 'dt', 'type': 'string'},
    {'name': 'act_id', 'type': 'int64'},
    {'name': 'service_id', 'type': 'int64'},
    {'name': 'group_service_order_id', 'type': 'int64'},
    {'name': 'service_order_id', 'type': 'int64'},
    {'name': 'client_id', 'type': 'int64'},
    {'name': 'inv_amount', 'type': 'double'},
    {'name': 'order_amount', 'type': 'double'},
]


OrderDwhRecord = namedtuple('OrderDwhRecord', [
    'dt_msk',
    'wallet_cid',
    'BillingOrderID',
    'Cost',
])

OrderDwhSchema = [
    {'name': 'dt_msk', 'type': 'string'},
    {'name': 'wallet_cid', 'type': 'int64'},
    {'name': 'BillingOrderID', 'type': 'int64'},
    {'name': 'Cost', 'type': 'int64'},
]

SpentCashbackRecord = namedtuple('SpentCashbackRecord', [
    'client_id',
    'cashback_amount',
])

SpentCashbackSchema = [
    {'name': 'client_id', 'type': 'int64'},
    {'name': 'cashback_amount', 'type': 'double'},
]

ClientTiersSchema = [
    {'name': 'client_id', 'type': 'int64'},
    {'name': 'client_login', 'type': 'string'},
]

ClientTiersRecord = namedtuple('ClientTiersRecord', [
    'client_id',
    'client_login',
])


ProductsRecord = namedtuple('Products', [
    'cid',
    'yyyymm',
    'product',
    'fraction_of_revenue',
])

ProductsSchema = [
    {'name': 'cid', 'type': 'int64'},
    {'name': 'yyyymm', 'type': 'string'},
    {'name': 'product', 'type': 'string'},
    {'name': 'fraction_of_revenue', 'type': 'double'},
    # {'name': 'client_id', 'type': 'int64'},
    # {'name': 'revenue_product', 'type': 'double'},
    # {'name': 'revenue_campaign', 'type': 'double'},
]

ClientsCashbackProgramsSchema = [
    {'name': '__hash__', 'type': 'int64'},
    {'name': '__source__', 'type': 'string'},
    {'name': 'client_cashback_program_id', 'type': 'int64'},
    {'name': 'ClientID', 'type': 'int64'},
    {'name': 'cashback_program_id', 'type': 'int64'},
]

ClientsCashbackProgramsRecord = namedtuple(
    'ClientsCashbackProgramsRecord',
    names_from_schema(ClientsCashbackProgramsSchema)
)

ClientsCashbackHistorySchema = [
    {'name': '__hash__', 'type': 'int64'},
    {'name': '__source__', 'type': 'string'},
    {'name': 'client_cashback_history_id', 'type': 'int64'},
    {'name': 'client_cashback_program_id', 'type': 'int64'},
    {'name': 'state_change', 'type': 'string'},
    {'name': 'change_time', 'type': 'string'},
]

ClientsCashbackHistoryRecord = namedtuple(
    'ClientsCashbackHistoryRecord',
    names_from_schema(ClientsCashbackHistorySchema),
)


CashbackProgramsSchema = [
    {'name': '__hash__', 'type': 'int64'},
    {'name': '__source__', 'type': 'string'},
    {'name': 'cashback_program_id', 'type': 'int64'},
    {'name': 'cashback_category_id', 'type': 'int64'},
    {'name': 'percent', 'type': 'string'},
    # {'name': 'is_general', 'type': 'int64'},
    # {'name': 'is_enabled', 'type': 'int64'},
]

CashbackProgramsRecord = namedtuple(
    'CashbackProgramsRecord',
    names_from_schema(CashbackProgramsSchema)
)

ProgramsRecord = namedtuple('ProgramsRecord', [
    'clients_cashback_history',
    'clients_cashback_programs',
    'cashback_programs',
])


ResultRecord = namedtuple('ResultRecord', [
    'client_id',
    'service_id',
    'currency',
    'reward_wo_nds',
    'reward',
    'calc_name_full',
    'cashback_details',
])

ResultSchema = [
    {'name': 'client_id', 'type': 'int64'},
    {'name': 'service_id', 'type': 'int32'},
    {'name': 'currency', 'type': 'utf8'},
    {'name': 'reward_wo_nds', 'type': 'double'},
    {'name': 'reward', 'type': 'double'},
    {'name': 'calc_name_full', 'type': 'string'},
    {'name': 'cashback_details', 'type': 'string'},
]


TestCaseRecords = namedtuple('TestCaseRecords', [
    'campaigns',
    'goals',
    'counters',
    'sales_daily',
    'group_order_act_div',
    'order_dwh',
    'spent_cashback',
    'products',
    'client_tiers',
    'result',
])


def generate_programs_data():
    clients_cashback_history = [
        ClientsCashbackHistoryRecord(
            client_cashback_history_id=1,
            client_cashback_program_id=10,
            state_change='in',
            change_time='2020-08-01 12:00:00',
        ),
        ClientsCashbackHistoryRecord(
            client_cashback_history_id=2,
            client_cashback_program_id=20,
            state_change='in',
            change_time='2020-08-01 12:00:00',
        ),
        ClientsCashbackHistoryRecord(
            client_cashback_history_id=3,
            client_cashback_program_id=30,
            state_change='in',
            change_time='2020-08-01 12:00:00',
        ),
        ClientsCashbackHistoryRecord(
            client_cashback_history_id=4,
            client_cashback_program_id=40,
            state_change='in',
            change_time='2020-08-01 12:00:00',
        ),
        ClientsCashbackHistoryRecord(
            client_cashback_history_id=5,
            client_cashback_program_id=50,
            state_change='in',
            change_time='2020-08-01 12:00:00',
        ),
    ]

    clients_cashback_programs = [
        ClientsCashbackProgramsRecord(
            client_cashback_program_id=10,
            ClientID=0,
            cashback_program_id=PROGRAM_ID_RMP,
        ),
        ClientsCashbackProgramsRecord(
            client_cashback_program_id=20,
            ClientID=0,
            cashback_program_id=PROGRAM_ID_AUTO,
        ),
        ClientsCashbackProgramsRecord(
            client_cashback_program_id=30,
            ClientID=0,
            cashback_program_id=PROGRAM_ID_METRIKA,
        ),
        ClientsCashbackProgramsRecord(
            client_cashback_program_id=40,
            ClientID=client_id(6),
            cashback_program_id=PROGRAM_ID_AUTOTARGETING,
        ),
        ClientsCashbackProgramsRecord(
            client_cashback_program_id=50,
            ClientID=client_id(7),
            cashback_program_id=PROGRAM_ID_RETARGETING,
        ),
    ]

    cashback_programs = [
        CashbackProgramsRecord(
            cashback_program_id=PROGRAM_ID_RMP,
            cashback_category_id=CATEGORY_ID_RMP,
            percent="0.1",
        ),
        CashbackProgramsRecord(
            cashback_program_id=PROGRAM_ID_AUTO,
            cashback_category_id=CATEGORY_ID_AUTO,
            percent="0.08",
        ),
        CashbackProgramsRecord(
            cashback_program_id=PROGRAM_ID_METRIKA,
            cashback_category_id=CATEGORY_ID_METRIKA,
            percent="0.04",
        ),
        CashbackProgramsRecord(
            cashback_program_id=PROGRAM_ID_AUTOTARGETING,
            cashback_category_id=CATEGORY_ID_AUTOTARGETING,
            percent="0.04",
        ),
        CashbackProgramsRecord(
            cashback_program_id=PROGRAM_ID_RETARGETING,
            cashback_category_id=CATEGORY_ID_RETARGETING,
            percent="0.05", # 0.06 in reality, 0.05 here
        )
    ]

    return ProgramsRecord(
        clients_cashback_history=clients_cashback_history,
        clients_cashback_programs=clients_cashback_programs,
        cashback_programs=cashback_programs
    )


def test_no_reward(test_id):
    campaigns = [CampaignsRecord(
        iso_date='2020-09-01',
        pk_cid=order_id(test_id),
        type='default',
        strategy_name='default',
        strategy_data='',
        statusActive='Yes',
    )]

    goals = [GoalsRecord(
        dt='2020-09-01',
        ClickDirectCampaignID=order_id(test_id),
        CounterID=counter_id(test_id),
        goal_id=goal_id(test_id),
        goal_type='url',
        goal_reaches=0,
        counter_visits=0,
    )]

    counters = [CountersRecord(
        dt='2020-09-01',
        ClickDirectCampaignID=order_id(test_id),
        CounterID=counter_id(test_id),
        counter_visits=10,
        order_clicks=1000,
        counter_in_settings=True,
    )]

    sales_daily = [SalesDailyRecord(
        dt='2020-09-01',
        act_id=act_id(test_id),
        agency_id=agency_id(test_id),
        agency_name=agency_name(test_id),
        manager_name=u'Тестовый Тест Тестович',
        client_name=u'Иванов Иван',
        order_client_id=client_id(test_id),
        service_id=7,
        currency='RUR',
        ar_commission_type=7,
        service_order_id=order_id(test_id),
        amount_nds=200.0,
        amount_nsp=0.0,
        amount=1200.0,
    )]

    group_order_act_div = [GroupOrderActDivRecord(
        dt='2020-09-01',
        act_id=act_id(test_id),
        service_id=7,
        group_service_order_id=0,
        service_order_id=0,
        client_id=client_id(test_id),
        inv_amount=1.0,
        order_amount=1.0,
    )]

    order_dwh = [OrderDwhRecord(
        dt_msk='2020-09-01T00:00:00,Europe/Moscow',
        wallet_cid=wallet_cid(test_id),
        BillingOrderID=order_id(test_id),
        Cost=100000,
    )]

    spent_cashback = [SpentCashbackRecord(
        client_id=client_id(test_id),
        cashback_amount=10.0 * test_id
    )]

    client_tiers = [ClientTiersRecord(
        client_id=client_id(test_id),
        client_login=login(test_id),
    )]

    result = [ResultRecord(
        client_id=client_id(test_id),
        service_id=7,
        currency='RUR',
        reward_wo_nds=0.0,
        reward=0.0,
        calc_name_full=CALC_NAME_FULL,
        cashback_details=json.dumps({
            "agency_id": agency_id(test_id),
            "agency_name": agency_name(test_id),
            "client_logins": login(test_id),
            "spent_amount_wo_nds": 10.0 * test_id,
            "reward": 0,
            "reward_wo_nds": 0,
            "details": [
                {
                    "program_id": PROGRAM_ID_RMP,
                    "reward": 0,
                    "reward_wo_nds": 0,
                    "amt": 0,
                    "amt_wo_nds": 0,
                    "pct": 0,
                },
                {
                    "program_id": PROGRAM_ID_AUTO,
                    "reward": 0,
                    "reward_wo_nds": 0,
                    "amt": 0,
                    "amt_wo_nds": 0,
                    "pct": 0,
                },
                {
                    "program_id": PROGRAM_ID_METRIKA,
                    "reward": 0,
                    "reward_wo_nds": 0,
                    "amt": 0,
                    "amt_wo_nds": 0,
                    "pct": 0,
                },
            ]
        }, sort_keys=True),
    )]

    return TestCaseRecords(
        campaigns=campaigns,
        goals=goals,
        counters=counters,
        sales_daily=sales_daily,
        group_order_act_div=group_order_act_div,
        order_dwh=order_dwh,
        spent_cashback=spent_cashback,
        products=[],
        client_tiers=client_tiers,
        result=result,
    )


def test_rmp_reward(test_id):
    campaigns = [CampaignsRecord(
        iso_date='2020-09-01',
        pk_cid=order_id(test_id),
        type='mobile_content',
        strategy_name='default',
        strategy_data='',
        statusActive='Yes',
    )]

    goals = [GoalsRecord(
        dt='2020-09-01',
        ClickDirectCampaignID=order_id(test_id),
        CounterID=counter_id(test_id),
        goal_id=goal_id(test_id),
        goal_type='url',
        goal_reaches=0,
        counter_visits=0,
    )]

    counters = [CountersRecord(
        dt='2020-09-01',
        ClickDirectCampaignID=order_id(test_id),
        CounterID=counter_id(test_id),
        counter_visits=10,
        order_clicks=1000,
        counter_in_settings=True,
    )]

    sales_daily = [SalesDailyRecord(
        dt='2020-09-01',
        act_id=act_id(test_id),
        agency_id=agency_id(test_id),
        agency_name=agency_name(test_id),
        manager_name=u'Тестовый Тест Тестович',
        client_name=u'Иванов Иван',
        order_client_id=client_id(test_id),
        service_id=7,
        currency='RUR',
        ar_commission_type=7,
        service_order_id=order_id(test_id),
        amount_nds=200.0,
        amount_nsp=0.0,
        amount=1200.0,
    )]

    group_order_act_div = [GroupOrderActDivRecord(
        dt='2020-09-01',
        act_id=act_id(test_id),
        service_id=7,
        group_service_order_id=0,
        service_order_id=0,
        client_id=client_id(test_id),
        inv_amount=1.0,
        order_amount=1.0,
    )]

    order_dwh = [OrderDwhRecord(
        dt_msk='2020-09-01T00:00:00,Europe/Moscow',
        wallet_cid=wallet_cid(test_id),
        BillingOrderID=order_id(test_id),
        Cost=100000,
    )]

    spent_cashback = [SpentCashbackRecord(
        client_id=client_id(test_id),
        cashback_amount=10.0 * test_id
    )]

    client_tiers = [ClientTiersRecord(
        client_id=client_id(test_id),
        client_login=login(test_id),
    )]

    result = [ResultRecord(
        client_id=client_id(test_id),
        service_id=7,
        currency='RUR',
        reward_wo_nds=100.0,
        reward=120.0,
        calc_name_full=CALC_NAME_FULL,
        cashback_details=json.dumps({
            "agency_id": agency_id(test_id),
            "agency_name": agency_name(test_id),
            "client_logins": login(test_id),
            "spent_amount_wo_nds": 10.0 * test_id,
            "reward": 120,
            "reward_wo_nds": 100,
            "details": [
                {
                    "program_id": PROGRAM_ID_RMP,
                    "reward": 120,
                    "reward_wo_nds": 100,
                    "amt": 1200,
                    "amt_wo_nds": 1200,
                    "pct": 0.1,
                },
                {
                    "program_id": PROGRAM_ID_AUTO,
                    "reward": 0,
                    "reward_wo_nds": 0,
                    "amt": 0,
                    "amt_wo_nds": 0,
                    "pct": 0,
                },
                {
                    "program_id": PROGRAM_ID_METRIKA,
                    "reward": 0,
                    "reward_wo_nds": 0,
                    "amt": 0,
                    "amt_wo_nds": 0,
                    "pct": 0,
                },
            ]
        }, sort_keys=True),
    )]

    return TestCaseRecords(
        campaigns=campaigns,
        goals=goals,
        counters=counters,
        sales_daily=sales_daily,
        group_order_act_div=group_order_act_div,
        order_dwh=order_dwh,
        spent_cashback=spent_cashback,
        products=[],
        client_tiers=client_tiers,
        result=result,
    )


def test_auto_reward(test_id):
    campaigns = [CampaignsRecord(
        iso_date='2020-09-01',
        pk_cid=order_id(test_id),
        type='default',
        strategy_name='autobudget_goal_id',
        strategy_data='',
        statusActive='Yes',
    )]

    goals = [GoalsRecord(
        dt='2020-09-01',
        ClickDirectCampaignID=order_id(test_id),
        CounterID=counter_id(test_id),
        goal_id=goal_id(test_id),
        goal_type='url',
        goal_reaches=0,
        counter_visits=0,
    )]

    counters = [CountersRecord(
        dt='2020-09-01',
        ClickDirectCampaignID=order_id(test_id),
        CounterID=counter_id(test_id),
        counter_visits=10,
        order_clicks=1000,
        counter_in_settings=True,
    )]

    sales_daily = [SalesDailyRecord(
        dt='2020-09-01',
        act_id=act_id(test_id),
        agency_id=agency_id(test_id),
        agency_name=agency_name(test_id),
        manager_name=u'Тестовый Тест Тестович',
        client_name=u'Иванов Иван',
        order_client_id=client_id(test_id),
        service_id=7,
        currency='RUR',
        ar_commission_type=7,
        service_order_id=order_id(test_id),
        amount_nds=200.0,
        amount_nsp=0.0,
        amount=1200.0,
    )]

    group_order_act_div = [GroupOrderActDivRecord(
        dt='2020-09-01',
        act_id=act_id(test_id),
        service_id=7,
        group_service_order_id=0,
        service_order_id=0,
        client_id=client_id(test_id),
        inv_amount=1.0,
        order_amount=1.0,
    )]

    order_dwh = [OrderDwhRecord(
        dt_msk='2020-09-01T00:00:00,Europe/Moscow',
        wallet_cid=wallet_cid(test_id),
        BillingOrderID=order_id(test_id),
        Cost=100000,
    )]

    spent_cashback = [SpentCashbackRecord(
        client_id=client_id(test_id),
        cashback_amount=10.0 * test_id
    )]

    client_tiers = [ClientTiersRecord(
        client_id=client_id(test_id),
        client_login=login(test_id),
    )]

    result = [ResultRecord(
        client_id=client_id(test_id),
        service_id=7,
        currency='RUR',
        reward_wo_nds=80.0,
        reward=96.0,
        calc_name_full=CALC_NAME_FULL,
        cashback_details=json.dumps({
            "agency_id": agency_id(test_id),
            "agency_name": agency_name(test_id),
            "client_logins": login(test_id),
            "spent_amount_wo_nds": 10.0 * test_id,
            "reward": 96,
            "reward_wo_nds": 80,
            "details": [
                {
                    "program_id": PROGRAM_ID_RMP,
                    "reward": 0,
                    "reward_wo_nds": 0,
                    "amt": 0,
                    "amt_wo_nds": 0,
                    "pct": 0,
                },
                {
                    "program_id": PROGRAM_ID_AUTO,
                    "reward": 96,
                    "reward_wo_nds": 80,
                    "amt": 1200,
                    "amt_wo_nds": 1000,
                    "pct": 0.08,
                },
                {
                    "program_id": PROGRAM_ID_METRIKA,
                    "reward": 0,
                    "reward_wo_nds": 0,
                    "amt": 0,
                    "amt_wo_nds": 0,
                    "pct": 0,
                },
            ]
        }, sort_keys=True),
    )]

    return TestCaseRecords(
        campaigns=campaigns,
        goals=goals,
        counters=counters,
        sales_daily=sales_daily,
        group_order_act_div=group_order_act_div,
        order_dwh=order_dwh,
        spent_cashback=spent_cashback,
        products=[],
        client_tiers=client_tiers,
        result=result,
    )


def test_metrika_reward(test_id):
    campaigns = [CampaignsRecord(
        iso_date='2020-09-01',
        pk_cid=order_id(test_id),
        type='default',
        strategy_name='default',
        strategy_data='',
        statusActive='Yes',
    )]

    # 10% goal reaches
    goals = [GoalsRecord(
        dt='2020-09-01',
        ClickDirectCampaignID=order_id(test_id),
        CounterID=counter_id(test_id),
        goal_id=goal_id(test_id),
        goal_type='url',
        goal_reaches=10,
        counter_visits=100,
    )]

    # 50% counter
    counters = [CountersRecord(
        dt='2020-09-01',
        ClickDirectCampaignID=order_id(test_id),
        CounterID=counter_id(test_id),
        counter_visits=600,
        order_clicks=1000,
        counter_in_settings=True,
    )]

    sales_daily = [SalesDailyRecord(
        dt='2020-09-01',
        act_id=act_id(test_id),
        agency_id=agency_id(test_id),
        agency_name=agency_name(test_id),
        manager_name=u'Тестовый Тест Тестович',
        client_name=u'Иванов Иван',
        order_client_id=client_id(test_id),
        service_id=7,
        currency='RUR',
        ar_commission_type=7,
        service_order_id=order_id(test_id),
        amount_nds=200.0,
        amount_nsp=0.0,
        amount=1200.0,
    )]

    group_order_act_div = [GroupOrderActDivRecord(
        dt='2020-09-01',
        act_id=act_id(test_id),
        service_id=7,
        group_service_order_id=0,
        service_order_id=0,
        client_id=client_id(test_id),
        inv_amount=1.0,
        order_amount=1.0,
    )]

    order_dwh = [OrderDwhRecord(
        dt_msk='2020-09-01T00:00:00,Europe/Moscow',
        wallet_cid=wallet_cid(test_id),
        BillingOrderID=order_id(test_id),
        Cost=100000,
    )]

    spent_cashback = [SpentCashbackRecord(
        client_id=client_id(test_id),
        cashback_amount=10.0 * test_id
    )]

    client_tiers = [ClientTiersRecord(
        client_id=client_id(test_id),
        client_login=login(test_id),
    )]

    result = [ResultRecord(
        client_id=client_id(test_id),
        service_id=7,
        currency='RUR',
        reward_wo_nds=40.0,
        reward=48.0,
        calc_name_full=CALC_NAME_FULL,
        cashback_details=json.dumps({
            "agency_id": agency_id(test_id),
            "agency_name": agency_name(test_id),
            "client_logins": login(test_id),
            "spent_amount_wo_nds": 10.0 * test_id,
            "reward": 48,
            "reward_wo_nds": 40,
            "details": [
                {
                    "program_id": PROGRAM_ID_RMP,
                    "reward": 0,
                    "reward_wo_nds": 0,
                    "amt": 0,
                    "amt_wo_nds": 0,
                    "pct": 0,
                },
                {
                    "program_id": PROGRAM_ID_AUTO,
                    "reward": 0,
                    "reward_wo_nds": 0,
                    "amt": 0,
                    "amt_wo_nds": 0,
                    "pct": 0,
                },
                {
                    "program_id": PROGRAM_ID_METRIKA,
                    "reward": 48,
                    "reward_wo_nds": 40,
                    "amt": 1200,
                    "amt_wo_nds": 1000,
                    "pct": 0.04,
                },
            ]
        }, sort_keys=True),
    )]

    return TestCaseRecords(
        campaigns=campaigns,
        goals=goals,
        counters=counters,
        sales_daily=sales_daily,
        group_order_act_div=group_order_act_div,
        order_dwh=order_dwh,
        spent_cashback=spent_cashback,
        products=[],
        client_tiers=client_tiers,
        result=result,
    )


def test_auto_no_metrika(test_id):
    campaigns = [CampaignsRecord(
        iso_date=iso_date,
        pk_cid=order_id(test_id),
        type='default',
        strategy_name=strategy_name,
        strategy_data='',
        statusActive='Yes',
    ) for iso_date, strategy_name in [
        ('2020-09-01', 'autobudget_avg_cpa'),
        ('2020-09-02', 'autobudget_avg_cpa'),
        ('2020-09-03', 'autobudget_roi'),
    ]]

    goals = [GoalsRecord(
        dt=dt,
        ClickDirectCampaignID=order_id(test_id),
        CounterID=counter_id(test_id),
        goal_id=goal_id(test_id),
        goal_type='url',
        goal_reaches=goal_reaches,
        counter_visits=100,
    ) for dt, goal_reaches in [
        ('2020-09-01', 0),
        ('2020-09-02', 10),
        ('2020-09-03', 10),
    ]]

    # 50% counter
    counters = [CountersRecord(
        dt=dt,
        ClickDirectCampaignID=order_id(test_id),
        CounterID=counter_id(test_id),
        counter_visits=600,
        order_clicks=1000,
        counter_in_settings=True,
    ) for dt in [
        '2020-09-01',
        '2020-09-02',
        '2020-09-03',
    ]]

    sales_daily = [SalesDailyRecord(
        dt='2020-09-01',
        act_id=act_id(test_id),
        agency_id=agency_id(test_id),
        agency_name=agency_name(test_id),
        manager_name=u'Тестовый Тест Тестович',
        client_name=u'Иванов Иван',
        order_client_id=client_id(test_id),
        service_id=7,
        currency='RUR',
        ar_commission_type=7,
        service_order_id=order_id(test_id),
        amount_nds=600.0,
        amount_nsp=0.0,
        amount=3600.0,
    )]

    group_order_act_div = [GroupOrderActDivRecord(
        dt='2020-09-01',
        act_id=act_id(test_id),
        service_id=7,
        group_service_order_id=0,
        service_order_id=0,
        client_id=client_id(test_id),
        inv_amount=1.0,
        order_amount=1.0,
    )]

    order_dwh = [OrderDwhRecord(
        dt_msk='{}T00:00:00,Europe/Moscow'.format(dt),
        wallet_cid=wallet_cid(test_id),
        BillingOrderID=order_id(test_id),
        Cost=100000,
    ) for dt in [
        '2020-09-01',
        '2020-09-02',
        '2020-09-03',
    ]]

    spent_cashback = [SpentCashbackRecord(
        client_id=client_id(test_id),
        cashback_amount=10.0 * test_id
    )]

    client_tiers = [ClientTiersRecord(
        client_id=client_id(test_id),
        client_login=login(test_id),
    )]

    result = [ResultRecord(
        client_id=client_id(test_id),
        service_id=7,
        currency='RUR',
        reward_wo_nds=280.0,
        reward=336.0,
        calc_name_full=CALC_NAME_FULL,
        cashback_details=json.dumps({
            "agency_id": agency_id(test_id),
            "agency_name": agency_name(test_id),
            "client_logins": login(test_id),
            "spent_amount_wo_nds": 10.0 * test_id,
            "reward": 336,
            "reward_wo_nds": 280,
            "details": [
                {
                    "program_id": PROGRAM_ID_RMP,
                    "reward": 0,
                    "reward_wo_nds": 0,
                    "amt": 0,
                    "amt_wo_nds": 0,
                    "pct": 0,
                },
                {
                    "program_id": PROGRAM_ID_AUTO,
                    "reward": 288,
                    "reward_wo_nds": 240,
                    "amt": 3600,
                    "amt_wo_nds": 3000,
                    "pct": 0.04,
                },
                {
                    "program_id": PROGRAM_ID_METRIKA,
                    "reward": 48,
                    "reward_wo_nds": 40,
                    "amt": 1200,
                    "amt_wo_nds": 1000,
                    "pct": 0.08,
                },
            ]
        }, sort_keys=True),
    )]

    return TestCaseRecords(
        campaigns=campaigns,
        goals=goals,
        counters=counters,
        sales_daily=sales_daily,
        group_order_act_div=group_order_act_div,
        order_dwh=order_dwh,
        spent_cashback=spent_cashback,
        products=[],
        client_tiers=client_tiers,
        result=result,
    )


def test_autotargeting(test_id):
    campaigns = [CampaignsRecord(
        iso_date='2020-09-01',
        pk_cid=order_id(test_id),
        type='text',
        strategy_name='default',
        strategy_data='',
        statusActive='Yes',
    )]

    goals = [GoalsRecord(
        dt='2020-09-01',
        ClickDirectCampaignID=order_id(test_id),
        CounterID=counter_id(test_id),
        goal_id=goal_id(test_id),
        goal_type='url',
        goal_reaches=0,
        counter_visits=0,
    )]

    counters = [CountersRecord(
        dt='2020-09-01',
        ClickDirectCampaignID=order_id(test_id),
        CounterID=counter_id(test_id),
        counter_visits=10,
        order_clicks=1000,
        counter_in_settings=True,
    )]

    sales_daily = [SalesDailyRecord(
        dt='2020-09-01',
        act_id=act_id(test_id),
        agency_id=agency_id(test_id),
        agency_name=agency_name(test_id),
        manager_name=u'Тестовый Тест Тестович',
        client_name=u'Иванов Иван',
        order_client_id=client_id(test_id),
        service_id=7,
        currency='RUR',
        ar_commission_type=7,
        service_order_id=order_id(test_id),
        amount_nds=200.0,
        amount_nsp=0.0,
        amount=1200.0,
    )]

    group_order_act_div = [GroupOrderActDivRecord(
        dt='2020-09-01',
        act_id=act_id(test_id),
        service_id=7,
        group_service_order_id=0,
        service_order_id=0,
        client_id=client_id(test_id),
        inv_amount=1.0,
        order_amount=1.0,
    )]

    order_dwh = [OrderDwhRecord(
        dt_msk='2020-09-01T00:00:00,Europe/Moscow',
        wallet_cid=wallet_cid(test_id),
        BillingOrderID=order_id(test_id),
        Cost=100000,
    )]

    spent_cashback = [SpentCashbackRecord(
        client_id=client_id(test_id),
        cashback_amount=10.0 * test_id
    )]

    products = [ProductsRecord(
        cid=order_id(test_id),
        yyyymm='202009',
        product='autotargeting',
        fraction_of_revenue=0.5
    )]

    client_tiers = [ClientTiersRecord(
        client_id=client_id(test_id),
        client_login=login(test_id),
    )]

    result = [ResultRecord(
        client_id=client_id(test_id),
        service_id=7,
        currency='RUR',
        reward_wo_nds=20.0,
        reward=24.0,
        calc_name_full=CALC_NAME_FULL,
        cashback_details=json.dumps({
            "agency_id": agency_id(test_id),
            "agency_name": agency_name(test_id),
            "client_logins": login(test_id),
            "spent_amount_wo_nds": 10.0 * test_id,
            "reward": 24,
            "reward_wo_nds": 20,
            "details": [
                {
                    "program_id": PROGRAM_ID_AUTOTARGETING,
                    "reward": 24,
                    "reward_wo_nds": 20,
                    "amt": 600,
                    "amt_wo_nds": 500,
                    "pct": 0.04,
                },
            ]
        }, sort_keys=True),
    )]

    return TestCaseRecords(
        campaigns=campaigns,
        goals=goals,
        counters=counters,
        sales_daily=sales_daily,
        group_order_act_div=group_order_act_div,
        order_dwh=order_dwh,
        spent_cashback=spent_cashback,
        products=products,
        client_tiers=client_tiers,
        result=result,
    )


def test_retargeting(test_id):
    campaigns = [CampaignsRecord(
        iso_date='2020-09-01',
        pk_cid=order_id(test_id),
        type='text',
        strategy_name='default',
        strategy_data='',
        statusActive='Yes',
    )]

    goals = [GoalsRecord(
        dt='2020-09-01',
        ClickDirectCampaignID=order_id(test_id),
        CounterID=counter_id(test_id),
        goal_id=goal_id(test_id),
        goal_type='url',
        goal_reaches=0,
        counter_visits=0,
    )]

    counters = [CountersRecord(
        dt='2020-09-01',
        ClickDirectCampaignID=order_id(test_id),
        CounterID=counter_id(test_id),
        counter_visits=10,
        order_clicks=1000,
        counter_in_settings=True,
    )]

    sales_daily = [SalesDailyRecord(
        dt='2020-09-01',
        act_id=act_id(test_id),
        agency_id=agency_id(test_id),
        agency_name=agency_name(test_id),
        manager_name=u'Тестовый Тест Тестович',
        client_name=u'Иванов Иван',
        order_client_id=client_id(test_id),
        service_id=7,
        currency='RUR',
        ar_commission_type=7,
        service_order_id=order_id(test_id),
        amount_nds=200.0,
        amount_nsp=0.0,
        amount=1200.0,
    )]

    group_order_act_div = [GroupOrderActDivRecord(
        dt='2020-09-01',
        act_id=act_id(test_id),
        service_id=7,
        group_service_order_id=0,
        service_order_id=0,
        client_id=client_id(test_id),
        inv_amount=1.0,
        order_amount=1.0,
    )]

    order_dwh = [OrderDwhRecord(
        dt_msk='2020-09-01T00:00:00,Europe/Moscow',
        wallet_cid=wallet_cid(test_id),
        BillingOrderID=order_id(test_id),
        Cost=100000,
    )]

    spent_cashback = [SpentCashbackRecord(
        client_id=client_id(test_id),
        cashback_amount=10.0 * test_id
    )]

    products = [ProductsRecord(
        cid=order_id(test_id),
        yyyymm='202009',
        product='retargeting',
        fraction_of_revenue=0.8
    )]

    client_tiers = [ClientTiersRecord(
        client_id=client_id(test_id),
        client_login=login(test_id),
    )]

    result = [ResultRecord(
        client_id=client_id(test_id),
        service_id=7,
        currency='RUR',
        reward_wo_nds=40.0,
        reward=48.0,
        calc_name_full=CALC_NAME_FULL,
        cashback_details=json.dumps({
            "agency_id": agency_id(test_id),
            "agency_name": agency_name(test_id),
            "client_logins": login(test_id),
            "spent_amount_wo_nds": 10.0 * test_id,
            "reward": 48,
            "reward_wo_nds": 40,
            "details": [
                {
                    "program_id": PROGRAM_ID_RETARGETING,
                    "reward": 48,
                    "reward_wo_nds": 40,
                    "amt": 960,
                    "amt_wo_nds": 800,
                    "pct": 0.06,
                },
            ]
        }, sort_keys=True),
    )]

    return TestCaseRecords(
        campaigns=campaigns,
        goals=goals,
        counters=counters,
        sales_daily=sales_daily,
        group_order_act_div=group_order_act_div,
        order_dwh=order_dwh,
        spent_cashback=spent_cashback,
        products=products,
        client_tiers=client_tiers,
        result=result,
    )


def make_test_table(yt_client, name, records, schema):
    test_data_root = '//home/search-research/apolygalov/cashback/test_data'
    # test_data_root = '//home/search-research/ga/agency_rewards/bonus_cashback/test_data'
    table_name = test_data_root + '/' + name
    yt_client.create("table", table_name, recursive=True, ignore_existing=True, attributes={'schema': schema})
    yt_client.write_table(table_name, records)


def make_test_data():
    test_data = [
        test_no_reward(1),
        test_rmp_reward(2),
        test_auto_reward(3),
        test_metrika_reward(4),
        test_auto_no_metrika(5),
        test_autotargeting(6),
        test_retargeting(7),
    ]

    test_acc = test_data[0]
    test_data_tail = test_data[1:]
    for case in test_data_tail:
        for i, table in enumerate(case):
            test_acc[i].extend(table)

    data = TestCaseRecords(*(list(map(lambda x: dict(x._asdict()), table)) for table in test_acc))

    yt_client = yt.client.Yt(proxy='hahn')

    make_test_table(yt_client, 'campaigns/202009', data.campaigns, CampaignsSchema)
    make_test_table(yt_client, 'goals/202009', data.goals, GoalsSchema)
    make_test_table(yt_client, 'counters/202009', data.counters, CountersSchema)
    make_test_table(yt_client, 'sales_daily/2020-09-01', data.sales_daily, SalesDailySchema)
    make_test_table(yt_client, 'group_order_act_div/2020-09-01', data.group_order_act_div, GroupOrderActDivSchema)
    make_test_table(yt_client, 'order_dwh/2020-09', data.order_dwh, OrderDwhSchema)
    make_test_table(yt_client, 'spent_cashback/2020-09-01', data.spent_cashback, SpentCashbackSchema)
    make_test_table(yt_client, 'products/202009', data.products, ProductsSchema)
    make_test_table(yt_client, 'client_tiers', data.client_tiers, ClientTiersSchema)
    make_test_table(yt_client, 'result', data.result, ResultSchema)

    programs = generate_programs_data() # <- extra programs here for autotargeting and retargeting (test_id 6 and 7)
    programs = ProgramsRecord(*(list(map(lambda x: dict(x._asdict()), table)) for table in programs))
    for table in programs:
        for r in table:
            r['__hash__'] = 0
            r['__source__'] = "-"
    make_test_table(yt_client, 'programs/ppc:1/straight/clients_cashback_history', programs.clients_cashback_history, ClientsCashbackHistorySchema)
    make_test_table(yt_client, 'programs/ppc:1/straight/clients_cashback_programs', programs.clients_cashback_programs, ClientsCashbackProgramsSchema)
    make_test_table(yt_client, 'programs/ppcdict/straight/cashback_programs', programs.cashback_programs, CashbackProgramsSchema)

def main():
    make_test_data()

if __name__ == "__main__":
    main()
