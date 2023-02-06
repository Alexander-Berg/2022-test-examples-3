import pytest

from google.protobuf.json_format import Parse, ParseDict
# noinspection PyUnresolvedReferences
from google.protobuf import field_mask_pb2  # noqa
from search.beholder.src.worker.replicated import ReplicatedIncidentDatabase
from search.beholder.proto.structures import protocols_pb2, database_pb2


PROTOCOL = Parse(r'''{
  "id": 2110,
  "etag": "80af4064-0a2c-4dd7-b749-a5713b954282",
  "componentName": "market_cpa",
  "parentComponentName": "market",
  "ticket": "MARKETINCIDENTS-7820",
  "startedManually": true,
  "startedBy": "g-plastinin",
  "startedAt": 1624436131,
  "updatedAt": 1624437563,
  "isChatCreated": true,
  "zoom": "https://yandex.zoom.us/j/93350841987?pwd=RzdmdjEwZjJzNTJiQWlhOFppQTU1Zz09",
  "chat": {
    "recordId": 2477,
    "id": "1327960967",
    "joinUrl": "https://t.me/joinchat/OI3HflosVuI2ZTEy",
    "title": "💑 Зеленый протокол",
    "pinMessageId": "4",
    "pinText": "#proto\nТекущий статус: <code>отключение vla помогает, ошибки приходят в норму</code>\n\nУровень протокола: green\nТикет: <a href=\"https://st.yandex-team.ru/MARKETINCIDENTS-7820\">MARKETINCIDENTS-7820</a>\nКомпонента: <a href=\"https://warden.z.yandex-team.ru/components/market/s/market_cpa\">market/market_cpa</a>\nСсылка на чат: https://t.me/joinchat/OI3HflosVuI2ZTEy\nZoom: https://yandex.zoom.us/j/93350841987?pwd=RzdmdjEwZjJzNTJiQWlhOFppQTU1Zz09\nРоли:\n    Дежурные:\n        - @ultroizmuroma - <a href=\"https://staff.yandex-team.ru/barinovale\">staff</a>\n        - @gusarm - <a href=\"https://staff.yandex-team.ru/gusar-mv\">staff</a>\n        - @artemkny - <a href=\"https://staff.yandex-team.ru/krinitsyn\">staff</a>\n"
  },
  "comments": {
    "timeline": 137035835
  },
  "roles": {
    "duty": [
      {
        "login": "barinovale",
        "username": "ultroizmuroma",
        "phone": 19913
      },
      {
        "login": "gusar-mv",
        "username": "gusarm",
        "phone": 41240
      },
      {
        "login": "krinitsyn",
        "username": "artemkny",
        "phone": 31025
      }
    ]
  }
}''', protocols_pb2.Protocol())  # noqa


@pytest.fixture(scope='module')
def database():
    return ReplicatedIncidentDatabase()


def test_create(database: ReplicatedIncidentDatabase):
    database.create(PROTOCOL)


def test_find(database: ReplicatedIncidentDatabase):
    filter_request: database_pb2.Filter = ParseDict(
        {
            'operator': 'AND',
            'filters': [
                {
                    'field': 'comments.timeline',
                    'value': '137035835'
                },
                {
                    'field': 'chat.recordId',
                    'value': '2477'
                },
            ]
        },
        database_pb2.Filter()
    )

    result = database.get(filter_request)
    assert len(result.protocols) == 1

    filter_request.filters[0].value = '2478'  # bad request

    result = database.get(filter_request)
    assert len(result.protocols) == 0


def test_update(database: ReplicatedIncidentDatabase):
    protocol = protocols_pb2.Protocol(
        id=2110,
        started_manually=False,
        mask=field_mask_pb2.FieldMask(paths=[
            'started_manually'
        ])
    )

    database.update(protocol)

    new_filter = database_pb2.Filter(
        field='id',
        value='2110'
    )

    result = database.get(new_filter)
    assert len(result.protocols) == 1
    assert not result.protocols[0].started_manually
