# coding=utf-8
import json
import requests
from datetime import datetime

from actions.hire.main import calc_days_diff, get_dep_url, calc_major_heads, get_max_salary, get_ticket_type, \
    get_comment_summonees, get_logins_from_heads, parse_is_ok, get_ok_summonees_comments, find_in_market_hire_comments, \
    get_next_url, get_ticket_stage

ROOT_DEPS = {
    "root_url_1": {
        "department": {
            "heads": "heads_someval"
        }
    }
}


def test_calc_days_diff():
    assert calc_days_diff("2019-03-20T07:34:53.686+0000", datetime(2019, 3, 22)) == 2


def test_get_dep_url():
    assert get_dep_url({
        "description": u'ncy** | тест ||\n|| **Подразделение, в которое требуется человек/Department** | (('
                       u'https://staff.yandex-team.ru/departments/yandex_edu_personel_rec_second_2181 Группа подбора '
                       u'для бизнес подразделений Яндекс.М '
    }, [], 'vacancy') == u"yandex_edu_personel_rec_second_2181"

    assert get_dep_url(
        {
            "description": u'some ticket decription'
        },
        [{
            "createdBy": {"id": "robot-femida"},
            "text": u'Предлагает согласовать оффер кандидату **Подразделение**: ((https://staff.yandex-team.ru/departments/yandex_monetize_market_5785_dep22937 Группа кросс-функциональных продуктов))'
        }],
        'offer') == u"yandex_monetize_market_5785_dep22937"


def test_calc_major_heads():
    dep_is_root = {
        "department": {
            "url": "root_url_1"
        }
    }
    assert calc_major_heads(dep_is_root, ROOT_DEPS) == "heads_someval"

    dep = {
        "department": {
            "url": "dep_url_2"
        },
        "ancestors": [
            {
                "department": {
                    "url": "dep_url_3"
                }
            },
            {
                "department": {
                    "url": "root_url_1"
                }
            },
        ]
    }
    assert calc_major_heads(dep, ROOT_DEPS) == "heads_someval"

    dep_without_root = {"department": {
        "url": "dep_url_4"
    },
        "ancestors": [
            {
                "department": {
                    "url": "dep_url_5"
                }
            },
        ]
    }

    exception_raised = False
    try:
        calc_major_heads(dep_without_root, ROOT_DEPS)
    except Exception:
        exception_raised = True

    assert exception_raised


def test_get_max_salary():
    r = get_max_salary(
        {"description": u"ква ||\n|| **Максимальный оклад/Max salary** | 50000 RUB ||\n|| **Комментарий к окладу"
         })
    assert r == {"value": 50000}


def test_get_max_salary_2():
    r = get_max_salary({
        u"description": u"#|\\n|| **Вакансия в Фемиде** | ((https://femida.yandex-team.ru/vacancies/67518 VAC 67518 Асессор)) ||\\n|| **Профессия/Profession** | Асессор ||\\n|| **Профессиональный уровень/Level** | Специалист ||\\n|| **Подразделение, в которое требуется человек/Department** | ((https://staff.yandex-team.ru/departments/yandex_monetize_market_quality_market Группа асессоров Маркета)) ||\\n|| **Подразделение, в которое ротируется сотрудник/Department in which employee is transferred** |  ||\\n|| **ФИО уходящего сотрудника/Name of replaceable employee** | staff:mshulya ||\\n|| **Дата увольнения (перевода)/Date of dismissal (rotation)** | 2019-04-12 ||\\n|| **Вид договора/Contract type** |  ||\\n|| **Причина замены сотрудника/The reason for replacing the employee** | Сотрудник увольняется ||\\n|| **Деятельность/Activity** | ((https://abc.yandex-team.ru/services/3811 Сервисы пользовательского направления Маркета)) --- ||\\n|| **Города/Сities** | Надомник (РФ) ||\\n|| **Максимальный оклад/Max salary** | 40000 RUB ||\\n|| **Комментарий к окладу/Max salary comment** | Компенсация интернета 700 рублей ||\\n|| **Система оплаты труда/Wage system** | Сдельная ||\\n|| **Кого добавить в копию/Who should be added to copy** | staff:wuddy --- ||\\n|| **Комментарий/Comment** |  ||\\n|#\\n"
    })
    assert r == {"value": 40000}


def test_get_max_salary_fail():
    r = get_max_salary(
        {"description": u"ква ||\n|| **Максимальный доклад/Max salary** | 50000 RUB ||\n|| **Комментарий к окладу"
         })
    assert r['value'] == 40001
    assert 'error' in r
    assert len(r['error']) > 0


def test_get_ticket_type():
    assert get_ticket_type({
        "summary": u"Новая (Рекрутер: Младший — Специалист)"
    }) == "hire_new"


def test_get_comment_summonees():
    assert get_comment_summonees(
        json.loads('{"summonees":[{"self":"...","id":"oroboros","display":"Dmitry Davydov"}]}')
    ) == ['oroboros']

    assert get_comment_summonees(
        json.loads('{"summonees":[{}, {"id":"oroboros"}]}')
    ) == ["oroboros"]

    assert get_comment_summonees(
        json.loads('{"summonees":[]}')
    ) == []

    assert get_comment_summonees(
        json.loads('{}')
    ) == []


def test_get_logins_from_heads():
    assert get_logins_from_heads(
        json.loads(
            '[{"person": {"is_deleted": false, "uid": "1120000000007856", "official": {"affiliation": "yandex", "is_dismissed": false, "is_robot": false, "is_homeworker": false}, "login": "yana-ya", "id": 5017, "name": {"last": {"ru": "Яковлева", "en": "Yakovleva"}, "first": {"ru": "Яна", "en": "Yana"}}}, "role": "hr_partner", "id": 8057}]')
    ) == []

    assert get_logins_from_heads(
        json.loads(
            '[{"person": {"is_deleted": false, "uid": "1120000000007856", "official": {"affiliation": "yandex", "is_dismissed": false, "is_robot": false, "is_homeworker": false}, "login": "yana-ya", "id": 5017, "name": {"last": {"ru": "Яковлева", "en": "Yakovleva"}, "first": {"ru": "Яна", "en": "Yana"}}}, "role": "not_hr_partner", "id": 8057}]')
    ) == ['yana-ya']


def test_parse_ok():
    test_str = u'''
    <[sdfsdf sdf

    sda f
    asd
    f]>
    привет! привет. привет
     ок

    <[sdfsdf]>
    '''
    assert parse_is_ok(test_str)
    assert parse_is_ok(u"ок!")
    assert parse_is_ok(u"ok. ")


bot_comment = json.loads(u'''
{"text":"Согласуй!", "summonees":[{"id":"oroboros"}]}
''')
summonee_comment = json.loads(u'''
{"text":"согласен", "createdBy":{"id":"oroboros"}} 
''')
summonee_not_ok_comment = json.loads(u'''
{"text":"думаю", "createdBy":{"id":"oroboros"}} 
''')


def test_get_ok_summonees_comments():
    comments = [bot_comment, summonee_comment]
    assert get_ok_summonees_comments(comments, 0) == {u'oroboros': u'ok'}


def test_get_ok_summonees_comments_multiple_1():
    comments = [bot_comment, summonee_not_ok_comment, summonee_comment]
    assert get_ok_summonees_comments(comments, 0) == {u'oroboros': u'ok'}


def test_get_ok_summonees_comments_multiple_2():
    comments = [bot_comment, summonee_comment, summonee_not_ok_comment]
    assert get_ok_summonees_comments(comments, 0) == {u'oroboros': u'ok'}


def test_get_ok_summonees_comments_not_ok():
    comments = [bot_comment, summonee_not_ok_comment]
    assert get_ok_summonees_comments(comments, 0) == {u'oroboros': summonee_not_ok_comment['text']}


comments = '''
[
    {"text": "search_str1", "createdBy": {"id": "robot-market-hire"}},
    {"text": "search_str2", "createdBy": {"id": "somebody"}},
    {"text": "not_searchable string", "createdBy": {"id": "robot-market-hire"}},
    {"text": "search_str3", "createdBy": {"id": "robot-market-hire"}}
]
'''


def test_find_in_market_hire_comments():
    print find_in_market_hire_comments(json.loads(comments), "search_str") == {
        "0": json.loads('{"text": "search_str1", "createdBy": {"id": "robot-market-hire"}}'),
        "3": json.loads('{"text": "search_str3", "createdBy": {"id": "robot-market-hire"}}'),
    }


def test_find_in_market_hire_comments_len():
    ok_request_id = 2
    author_requested_code = "search_str3"

    assert len(find_in_market_hire_comments(json.loads(comments)[ok_request_id:], author_requested_code)) == 1


def test_get_next_url():
    class TestHttpResponse(requests.Response):
        pass

    resp = TestHttpResponse()
    resp.headers = {"Link": "< a >; rel=\"next\" link1, < b >; rel=\"prev\" link2"}

    assert get_next_url(resp) == "a"


itest_data = {
    "MARKETJOBTEST-1": {
        "url": "yandex_monetize_market_0677",
        "ok_request_ids": [0],
        "summonees": [u'grumannavot'],
        "max_salary": None,
        "ok_summonees": "not_ok"
    },
    "MARKETJOBTEST-2": {
        "url": "yandex_monetize_market_marketdev_business_stat",
        "ok_request_ids": [],
        "summonees": [u'geradmi'],
        "max_salary": 35000,
        "ok_summonees": None
    }
}


# ignored — tokens required
def integration_testing():
    import actions.hire.main as hire_main
    hire_main.SCRIPT_ENV = "Testing"
    hire_main.STAFF_TOKEN = '...'
    hire_main.TRACKER_TOKEN = '...'
    hire_main.ROBOT_MARKET_HIRE_LOGIN = "oroboros"

    from actions.hire.main import get_market_components_query, get_children_of_market_root, \
        request_tracker_api_all_pages, get_queue, request_tracker_comments, get_dep, get_summonees

    import logging
    logging.basicConfig(level=logging.INFO)
    hire_main.log = logging.getLogger(__name__)
    components_query = get_market_components_query()
    assert components_query == '(Components:56098 or Components:56207)'

    root_deps = get_children_of_market_root()
    assert len(root_deps.keys()) > 0
    assert "yandex_monetize_market_design" in root_deps.keys()
    assert "yandex_monetize_market_marketdev" in root_deps.keys()

    tickets = request_tracker_api_all_pages(
        "/issues?query=Queue:%s and status:!resolved and status:!closed and status:!onHold and %s and Created: > 01-03-2019&perPage=%d" %
        (
            get_queue(),
            components_query,
            # "Components:49159", # Маркет_HR for testing
            hire_main.TRACKER_PER_PAGE))

    assert len(tickets) > 0

    for ticket in tickets:
        comments = request_tracker_comments(ticket['key'])
        assert len(comments) > 0

        stage = get_ticket_stage(comments)
        assert stage == "vacancy"  # no offers in test data

        dep_url = get_dep_url(ticket, comments, stage).lower()
        assert dep_url == itest_data[ticket['key']]['url']

        dep = get_dep(dep_url)
        assert dep['department']['url'] == dep_url

        ok_request_ids = list(find_in_market_hire_comments(
            comments, hire_main.CODE_KEYWORD + u'/ok_requested_stage=%s' % stage).keys())
        assert ok_request_ids == itest_data[ticket['key']]['ok_request_ids']

        summonees = get_summonees(dep, root_deps, ticket, stage, comments)
        assert summonees == itest_data[ticket['key']]['summonees']

        max_salary = get_max_salary(ticket)
        if itest_data[ticket['key']]['max_salary'] is not None:
            assert 'error' not in max_salary
            assert max_salary['value'] == itest_data[ticket['key']]['max_salary']
        else:
            assert 'error' in max_salary

        if len(ok_request_ids) == 0:
            continue

        ok_summonees = get_ok_summonees_comments(comments, ok_request_ids[-1])
        if itest_data[ticket['key']]['ok_summonees'] is None:
            assert ok_summonees is None
        elif itest_data[ticket['key']]['ok_summonees'] == 'not_ok':
            assert all(x != hire_main.OK_KEYWORD for x in ok_summonees)
        elif itest_data[ticket['key']]['ok_summonees'] == 'ok':
            assert all(x == hire_main.OK_KEYWORD for x in ok_summonees)

        assert calc_days_diff(comments[ok_request_ids[-1]]['createdAt'], datetime.now()) > 0

