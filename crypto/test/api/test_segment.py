import json

from psycopg2 import extras
import pytest
import requests

TEST_SEGMENT = 'segment-test'
HEADERS_FORM_URLENCODED = {'accept': 'application/json; charset=utf-8',
                           'content-type': 'application/x-www-form-urlencoded'}


def get_segment_by_id(segment_id, postgres):
    with postgres.connect() as connection, connection.cursor(cursor_factory=extras.RealDictCursor) as cursor:
        cursor.execute(f"""
            SELECT * FROM api_segments WHERE id='{segment_id}';
        """)
        return cursor.fetchone()


def get_segment(postgres, response):
    if 'id' in response:
        segment_id = response['id']
        segment = get_segment_by_id(segment_id, postgres)

        segment = {'description_ru': segment.get('description_ru'),
                   'name_ru': segment.get('name_ru'),
                   'parent_id': segment.get('parent_id'),
                   'ticket': segment.get('ticket'),
                   'type': segment.get('type')
                   }

        response = {'description': response.get('description'),
                    'name': response.get('name'),
                    'parentId': response.get('parentId'),
                    'tickets': response.get('tickets'),
                    'type': response.get('type')
                    }
    else:
        segment = 'Not found'
        response.pop('requestId')

    return {'response': response, 'segment': segment}


def post_segment_group(api, name_ru, description_ru, parent_id):
    return requests.post(f"http://localhost:{api.port}/lab/segment/groups?",
                         params={'nameRu': name_ru,
                                 'descriptionRu': description_ru,
                                 'parentId': parent_id
                                 }).json()


def post_segment(api, name_ru, description_ru, tickets, parent_id, responsibles=None, stakeholders=None):
    return requests.post(f"http://localhost:{api.port}/lab/segment",
                         headers=HEADERS_FORM_URLENCODED,
                         data={'nameRu': name_ru,
                               'descriptionRu': description_ru,
                               'tickets': tickets,
                               'scope': 'INTERNAL',
                               'type': 'HEURISTIC',
                               'responsibles': responsibles,
                               'stakeholders': stakeholders,
                               'parentId': parent_id
                               }).json()


def post_user_segment(api, name_ru, description_ru, tickets):
    return requests.post(f"http://localhost:{api.port}/lab/segment/user",
                         headers=HEADERS_FORM_URLENCODED,
                         data={'nameRu': name_ru,
                               'descriptionRu': description_ru,
                               'tickets': tickets
                               }).json()


def put_segment(api, segment_id, name_ru='name', description_ru='desc'):
    name = json.dumps({"en": "", "ru": name_ru})
    description = json.dumps({"en": "", "ru": description_ru})
    return requests.put(f"http://localhost:{api.port}/lab/segment/{segment_id}/name_and_description?",
                        params={'name': name,
                                'description': description
                                }).json()


@pytest.mark.parametrize("name_ru", ["", "   ", "new name"])
def test_update_segment_name(api, postgres, init_db, name_ru):
    response = put_segment(api, TEST_SEGMENT, name_ru)
    return get_segment(postgres, response)


def test_update_segment_name_not_found(api, postgres, init_db):
    response = put_segment(api, 'test')
    return get_segment(postgres, response)


@pytest.mark.parametrize("description_ru", ["", "   ", "new description"])
def test_update_segment_description(api, postgres, init_db, description_ru):
    response = put_segment(api, TEST_SEGMENT, description_ru=description_ru)
    return get_segment(postgres, response)


def test_update_segment_description_not_found(api, postgres, init_db):
    response = put_segment(api, 'test')
    return get_segment(postgres, response)


@pytest.mark.parametrize("parent_id", [None, 'root'])
def test_add_segment_group(api, postgres, init_db, parent_id):
    response = post_segment_group(api, 'test segment', 'desc', parent_id)
    return get_segment(postgres, response)


@pytest.mark.parametrize("name_ru", ["", "   "])
def test_add_segment_group_bad_name_ru(api, postgres, init_db, name_ru):
    response = post_segment_group(api, name_ru, 'desc', 'root')
    return get_segment(postgres, response)


@pytest.mark.parametrize("description_ru", ["", "   "])
def test_add_segment_group_bad_description_ru(api, postgres, init_db, description_ru):
    response = post_segment_group(api, 'name', description_ru, 'root')
    return get_segment(postgres, response)


@pytest.mark.parametrize("parent_id", ["", "   ", "test"])
def test_add_segment_group_bad_parent_id(api, postgres, init_db, parent_id):
    response = post_segment_group(api, 'name', 'desc', parent_id)
    return get_segment(postgres, response)


@pytest.mark.parametrize("segment_id", [TEST_SEGMENT, 'test'])
def test_get_segment(api, postgres, init_db, segment_id):
    response = requests.get(f"http://localhost:{api.port}/lab/segment/{segment_id}").json()
    return get_segment(postgres, response)


def test_add_segment(api, postgres, init_db):
    response = post_segment(api, 'test segment', 'desc', 'CRYPTA-1', TEST_SEGMENT)
    return get_segment(postgres, response)


@pytest.mark.parametrize("name_ru", [None, "", "    "])
def test_add_segment_bad_name_ru(api, postgres, init_db, name_ru):
    response = post_segment(api, name_ru, 'desc', 'CRYPTA-1', TEST_SEGMENT)
    return get_segment(postgres, response)


@pytest.mark.parametrize("description_ru", [None, "", "    "])
def test_add_segment_bad_description_ru(api, postgres, init_db, description_ru):
    response = post_segment(api, 'name', description_ru, 'CRYPTA-1', TEST_SEGMENT)
    return get_segment(postgres, response)


@pytest.mark.parametrize("tickets", [None, "", "   "])
def test_add_segment_bad_ticket(api, postgres, init_db, tickets):
    response = post_segment(api, 'name', 'desc', tickets, TEST_SEGMENT)
    return get_segment(postgres, response)


@pytest.mark.parametrize("parent_id", [None, "", "   ", "test"])
def test_add_segment_bad_parent_id(api, postgres, init_db, parent_id):
    response = post_segment(api, 'name', 'desc', 'CRYPTA-1', parent_id)
    return get_segment(postgres, response)


@pytest.mark.parametrize("responsibles", ["", "   "])
def test_add_segment_bad_responsible(api, postgres, init_db, responsibles):
    response = post_segment(api, 'name', 'desc', 'CRYPTA-1', TEST_SEGMENT, responsibles)
    return get_segment(postgres, response)


@pytest.mark.parametrize("stakeholders", ["", "   "])
def test_add_segment_bad_stakeholder(api, postgres, init_db, stakeholders):
    response = post_segment(api, 'name', 'desc', 'CRYPTA-1', TEST_SEGMENT, stakeholders=stakeholders)
    return get_segment(postgres, response)


def test_add_user_segment(api, postgres, init_db):
    response = post_user_segment(api, 'test segment', 'desc', 'CRYPTA-1')
    return get_segment(postgres, response)


@pytest.mark.parametrize("name_ru", [None, "", "    "])
def test_add_user_segment_bad_name_ru(api, postgres, init_db, name_ru):
    response = post_user_segment(api, name_ru, 'desc', 'CRYPTA-1')
    return get_segment(postgres, response)


@pytest.mark.parametrize("description_ru", [None, "", "    "])
def test_add_user_segment_bad_description_ru(api, postgres, init_db, description_ru):
    response = post_user_segment(api, 'name', description_ru, 'CRYPTA-1')
    return get_segment(postgres, response)


@pytest.mark.parametrize("tickets", [None, "", "   "])
def test_add_user_segment_bad_ticket(api, postgres, init_db, tickets):
    response = post_user_segment(api, 'name', 'desc', tickets)
    return get_segment(postgres, response)
