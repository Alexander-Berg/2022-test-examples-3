# coding: utf-8

import json
import pytest
from hamcrest import assert_that, equal_to
import time

from market.idx.datacamp.proto.common.SchemaType_pb2 import PUSH_TO_PULL, PULL_TO_PUSH, PULL, PUSH
from market.idx.datacamp.proto.tables.Partner_pb2 import PartnerAdditionalInfo
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampPartersYtRows
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf


def dict2tskv(params):
    def sort_key(item):
        name = item[0]
        if name in ('shop_id', '#shop_id'):
            return (0, name)
        return (1, name)

    def to_str(x):
        if isinstance(x, list):
            return "" if len(x) == 0 else ";".join(map(str, x)) + ";"
        return str(x) if not isinstance(x, bool) else str(x).lower()

    props = list(params.items())
    props.sort(key=sort_key)
    return '\n'.join([k + '\t' + to_str(v) for k, v in props])


@pytest.fixture(scope='module')
def partners():
    return [
        {'shop_id': 1},
        {'shop_id': 3},
        {'shop_id': 4},
    ]


@pytest.yield_fixture(scope='module')
def stroller(
    config,
    yt_server,
    log_broker_stuff,
    partners_table,
):
    with make_stroller(
        config,
        yt_server,
        log_broker_stuff,
        partners_table=partners_table,
    ) as stroller_env:
        yield stroller_env


def do_request_change_schema(client, shop_id, data):
    return client.put('/shops/{}/change_schema'.format(shop_id), data=json.dumps(data))


def request(client, shop_id, change_schema_type=None, start_ts=None, finish_ts=None, final_schema=None):
    data = {}
    if change_schema_type is not None:
        data['change_schema_type'] = change_schema_type
    if start_ts is not None:
        data['start_ts_sec'] = start_ts
    if finish_ts is not None:
        data['finish_ts_sec'] = finish_ts
    if final_schema is not None:
        data['is_push_partner'] = True if final_schema == PUSH else False

    response = do_request_change_schema(client, shop_id, data)
    return response


def test_change_schema_for_known_partner(stroller, partners_table):
    shop_id = 1

    now = int(time.time())
    start_ts01 = now + 10
    finish_ts01 = now + 20
    start_ts02 = now + 30
    finish_ts02 = now + 40

    # шаг 1 - начинаем процесс перевода известного нам партнера из PULL-схемы в PUSH
    response = request(stroller, shop_id, change_schema_type='pull_to_push', start_ts=start_ts01)
    assert_that(response, HasStatus(200))
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows(
                    [{
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PULL_TO_PUSH,
                                'start_ts': {
                                    'seconds': start_ts01
                                },
                                'schema_switches_count': 0,
                            }
                        })
                    }]
                ), 'Incorrect start from PULL to PUSH')
    assert_that(response.headers['Content-type'], equal_to('text/plain; charset=utf-8'))

    # шаг 2 - делаем ошибочный запрос - начать процесс перевода партнера из PUSH-схемы в PULL,
    # когда предыдущий процесс еще не завершился; проверяем, что данные в таблице НЕ изменились
    response = request(stroller, shop_id, change_schema_type='push_to_pull', start_ts=start_ts02)
    assert_that(response, HasStatus(400))
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows(
                    [{
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PULL_TO_PUSH,
                                'start_ts': {
                                    'seconds': start_ts01
                                },
                                'schema_switches_count': 0,
                            }
                        })
                    }]
                ), 'Incorrect request, table data should not change')

    # шаг 3 - заканчиваем процесс перевода из PULL-схемы в PUSH
    response = request(stroller, shop_id, finish_ts=finish_ts01, final_schema=PUSH)
    assert_that(response, HasStatus(200))
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows(
                    [{
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PULL_TO_PUSH,
                                'start_ts': {
                                    'seconds': start_ts01
                                },
                                'finish_ts': {
                                    'seconds': finish_ts01
                                },
                                'schema_switches_count': 0,
                            },
                            'partner_schema': PUSH,
                        })
                    }]
                ), 'Incorrect finish from PULL to PUSH')

    # шаг 4 - начинаем процесс перевода известного нам партнера из PUSH-схемы в PULL
    response = request(stroller, shop_id, change_schema_type='push_to_pull', start_ts=start_ts02)
    assert_that(response, HasStatus(200))
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows(
                    [{
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PUSH_TO_PULL,
                                'start_ts': {
                                    'seconds': start_ts02
                                },
                                'schema_switches_count': 0,
                            }
                        })
                    }]
                ), 'Incorrect start from PUSH to PULL')

    # шаг 5 - заканчиваем процесс перевода из PUSH-схемы в PULL
    response = request(stroller, shop_id, finish_ts=finish_ts02, final_schema=PULL)
    assert_that(response, HasStatus(200))
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows(
                    [{
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PUSH_TO_PULL,
                                'start_ts': {
                                    'seconds': start_ts02
                                },
                                'finish_ts': {
                                    'seconds': finish_ts02
                                },
                                'schema_switches_count': 0,
                            },
                            'partner_schema': PULL,
                        })
                    }]
                ), 'Incorrect finish from PUSH to PULL')


def test_change_schema_for_unknown_partner(stroller, partners_table):
    shop_id = 2
    start_ts01 = 10
    finish_ts01 = 20

    # шаг 1 - начинаем перевод из PULL в PUSH-схему партнера, которого нет в таблице
    response = request(stroller, shop_id, change_schema_type='pull_to_push', start_ts=start_ts01)
    assert_that(response, HasStatus(200))
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows(
                    [{
                        'shop_id': shop_id,
                        'status': 'disable',
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PULL_TO_PUSH,
                                'start_ts': {
                                    'seconds': start_ts01
                                },
                                'schema_switches_count': 0,
                            }
                        })
                    }]
                ), 'Incorrect start for unknown partner')

    # шаг 2 - заканчиваем перевод из PULL в PUSH-схему
    response = request(stroller, shop_id, finish_ts=finish_ts01, final_schema=PUSH)
    assert_that(response, HasStatus(200))
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows(
                    [{
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PULL_TO_PUSH,
                                'start_ts': {
                                    'seconds': start_ts01
                                },
                                'finish_ts': {
                                    'seconds': finish_ts01
                                },
                                'schema_switches_count': 0,
                            },
                            'partner_schema': PUSH,
                        })
                    }]
                ), 'Incorrect finish for unknown partner')


def test_change_schema_unsuccessfully_pull_to_push(stroller, config, partners_table):
    """Тест проверяет, что в случае не успешного переключения из pull в push
    мы позволим изменить переходный период на обратный. + базовое поведение об отсутствии зацикливаний
    и запрете переключений в обратную схему раньше фиксированного промежутка времени"""
    shop_id = 3

    allowed_switching_interval_seconds = config.schema_switching_max_hours * 60 * 60
    start_ts01 = int(time.time()) - allowed_switching_interval_seconds - 10
    start_ts02 = start_ts01 + allowed_switching_interval_seconds
    start_ts03 = start_ts01 + 2 * allowed_switching_interval_seconds
    finish_ts01 = start_ts02 + 50

    # шаг 1 - начинаем процесс перевода известного нам партнера из PULL-схемы в PUSH
    response = request(stroller, shop_id, change_schema_type='pull_to_push', start_ts=start_ts01)
    assert_that(response, HasStatus(200))
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows(
                    [{
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PULL_TO_PUSH,
                                'start_ts': {
                                    'seconds': start_ts01
                                },
                                'schema_switches_count': 0,
                                'is_ready_to_index_from_datacamp': False,
                            }
                        })
                    }]
                ), 'Incorrect start from PULL to PUSH')

    # шаг 2 - повторная попытка начать процесс перевода партнера из PUSH-схемы в PULL,
    # должны сказать 200; проверяем, что данные в таблице НЕ изменились
    response = request(stroller, shop_id, change_schema_type='pull_to_push', start_ts=start_ts02)
    assert_that(response, HasStatus(200))
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows(
                    [{
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PULL_TO_PUSH,
                                'start_ts': {
                                    'seconds': start_ts01
                                },
                                'schema_switches_count': 0,
                                'is_ready_to_index_from_datacamp': False,
                            }
                        })
                    }]
                ), 'Incorrect request, table data should not change')

    # шаг 3 - пытаемся откатить процесс перевода партнера из PULL-схемы в PUSH, меняем тип переключения на обратный;
    # now - ts начала отката больше, чем отводится на процесс переключения; проверяем, что данные в таблице изменились
    response = request(stroller, shop_id, change_schema_type='push_to_pull', start_ts=start_ts02)
    assert_that(response, HasStatus(200))
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows(
                    [{
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PUSH_TO_PULL,
                                'start_ts': {
                                    'seconds': start_ts02
                                },
                                'schema_switches_count': 1,
                                'is_ready_to_index_from_datacamp': False,
                            }
                        })
                    }]
                ), 'Incorrect request, table data should change')

    # шаг 4 - что-то идет совсем не так, мы зациклились и снова пытаемся изменить схему
    # но больше одного раза нам сделать такое не дадут; проверяем, что данные в таблице изменились
    response = request(stroller, shop_id, change_schema_type='pull_to_push', start_ts=start_ts03)
    assert_that(response, HasStatus(400))
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows(
                    [{
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PUSH_TO_PULL,
                                'start_ts': {
                                    'seconds': start_ts02
                                },
                                'schema_switches_count': 1,
                                'is_ready_to_index_from_datacamp': False,
                            }
                        })
                    }]
                ), 'Incorrect request, table data should not change')

    # шаг 5 - заканчиваем процесс перевода из PUSH-схемы в PULL
    response = request(stroller, shop_id, finish_ts=finish_ts01, final_schema=PULL)
    assert_that(response, HasStatus(200))
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows(
                    [{
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PUSH_TO_PULL,
                                'start_ts': {
                                    'seconds': start_ts02
                                },
                                'finish_ts': {
                                    'seconds': finish_ts01
                                },
                                'schema_switches_count': 1,
                            },
                            'partner_schema': PULL,
                        })
                    }]
                ), 'Incorrect finish for pull partner')


def test_change_schema_unsuccessfully_push_to_pull(stroller, config, partners_table):
    """Тест проверяет, что в случае неуспешного переключения из push в pull
    мы позволим изменить переходный период на обратный,
    и начнем индексировать из старой целевой схемы как можно быстрее."""
    shop_id = 4

    allowed_switching_interval_seconds = config.schema_switching_max_hours * 60 * 60
    start_ts01 = int(time.time()) - allowed_switching_interval_seconds - 10
    start_ts02 = start_ts01 + allowed_switching_interval_seconds
    finish_ts01 = start_ts02 + 50
    start_ts03 = finish_ts01 + 50
    finish_ts03 = start_ts03 + 50

    # шаг 1 - начинаем процесс перевода известного нам партнера из PUSH-схемы в PULL
    response = request(stroller, shop_id, change_schema_type='push_to_pull', start_ts=start_ts01)
    assert_that(response, HasStatus(200))
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows(
                    [{
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PUSH_TO_PULL,
                                'start_ts': {
                                    'seconds': start_ts01
                                },
                                'schema_switches_count': 0,
                            }
                        })
                    }]
                ), 'Incorrect start from PUSH to PULL')

    # шаг 2 - пытаемся откатить процесс перевода партнера из PUSH-схемы в PULL, меняем тип переключения на обратный;
    # now - ts начала отката больше, чем отводится на процесс переключения; проверяем, что данные в таблице изменились
    response = request(stroller, shop_id, change_schema_type='pull_to_push', start_ts=start_ts02)
    assert_that(response, HasStatus(200))
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows(
                    [{
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PULL_TO_PUSH,
                                'start_ts': {
                                    'seconds': start_ts02
                                },
                                'schema_switches_count': 1,
                                'is_ready_to_index_from_datacamp': True,  # индексируем из старой целевой схемы как можно быстрее
                            }
                        })
                    }]
                ), 'Incorrect request, table data should change')

    # шаг 3 - заканчиваем процесс перевода из PULL-схемы в PUSH
    response = request(stroller, shop_id, finish_ts=finish_ts01, final_schema=PUSH)
    assert_that(response, HasStatus(200))
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows(
                    [{
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PULL_TO_PUSH,
                                'start_ts': {
                                    'seconds': start_ts02
                                },
                                'finish_ts': {
                                    'seconds': finish_ts01
                                },
                                'schema_switches_count': 1,
                                'is_ready_to_index_from_datacamp': True,

                            },
                            'partner_schema': PUSH,
                        })
                    }]
                ), 'Incorrect finish for push partner')

    # шаг 4 - теперь еще раз пробуем переключить партнера, и раз предыдущий процесс был закончен,
    # то все должно получиться и счетчик переключений должен обнулиться; проверяем, что данные в таблице изменились
    response = request(stroller, shop_id, change_schema_type='push_to_pull', start_ts=start_ts03)
    assert_that(response, HasStatus(200))
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows(
                    [{
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PUSH_TO_PULL,
                                'start_ts': {
                                    'seconds': start_ts03
                                },
                                'schema_switches_count': 0,
                            }
                        })
                    }]
                ), 'Incorrect request, table data should change')

    # шаг 5 - заканчиваем процесс перехода из push->pull, проверяем таблицу
    response = request(stroller, shop_id, finish_ts=finish_ts03, final_schema=PULL)
    assert_that(response, HasStatus(200))
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows(
                    [{
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PUSH_TO_PULL,
                                'start_ts': {
                                    'seconds': start_ts03
                                },
                                'finish_ts': {
                                    'seconds': finish_ts03
                                },
                                'schema_switches_count': 0,
                            },
                            'partner_schema': PULL,
                        })
                    }]
                ), 'Incorrect finish for push partner')
