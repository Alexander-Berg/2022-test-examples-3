# coding: utf-8

import time

from ads.bsyeti.big_rt.py_test_lib import (
    extract_all_states_from_simple_table,
)

from market.idx.datacamp.picrobot.processor.proto.state_pb2 import (
    TPicrobotState
)

from .conftest import (
    pack_message,
    request_event,
    launch,
    image_event,
    delete_event,
    delete_response_event,
    generate_mds_json,
)


BASE_YT_DIR = '//tmp'
PICTURE_URL = 'https://example.com/new_img1'
PICTURE_NAMESPACE = 'marketpic'
PICTURE_GROUP = 123
PICTURE_NAME = 'pic'


def test_delete_old_removed_image(stand):
    ts = int(time.time()) - 6 * 30 * 24 * 3600  # полгода назад

    # запросили картинку
    data = {0: [
        pack_message(request_event(
            url=PICTURE_URL,
            namespace=PICTURE_NAMESPACE,
            offer='https://example.com/offer'
        )),
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)
    output_offset = 0
    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    # скачали картинку
    data = {0: [
        pack_message(image_event(
            url=PICTURE_URL,
            mds_json=generate_mds_json(
                namespace=PICTURE_NAMESPACE,
                group_id=PICTURE_GROUP,
                name=PICTURE_NAME,
            )
        )),
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)
    output_offset = 0
    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    # проверим, что появилась Mds-инфо в стейте
    raw_state = extract_all_states_from_simple_table(stand.yt_client, stand.state_table_path, decompress=True)
    state = TPicrobotState()
    state.ParseFromString(raw_state[PICTURE_URL])
    assert set(info.MdsId.Namespace for info in state.MdsInfo) == {PICTURE_NAMESPACE}

    # удалим картинку в два притопа и три прихлопа
    data = {0: [
        pack_message(delete_event(  # пометить
            url=PICTURE_URL,
            namespace=PICTURE_NAMESPACE,
            group_id=PICTURE_GROUP,
            image_name=PICTURE_NAME,
            ts=ts
        )),
        pack_message(delete_event(  # удалить
            url=PICTURE_URL,
            namespace=PICTURE_NAMESPACE,
            group_id=PICTURE_GROUP,
            image_name=PICTURE_NAME,
            ts=ts
        )),
        pack_message(delete_response_event(  # удалено
            url=PICTURE_URL,
            namespace=PICTURE_NAMESPACE,
            group_id=PICTURE_GROUP,
            image_name=PICTURE_NAME,
            isDeleted=True,
            ts=ts
        )),
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    # в стейте не осталось записей
    raw_state = extract_all_states_from_simple_table(stand.yt_client, stand.state_table_path, decompress=True)
    assert PICTURE_URL not in raw_state


def test_dont_delete_freshly_removed_image(stand):
    ts = int(time.time()) - 24 * 3600  # вчера

    # запросили картинку
    data = {0: [
        pack_message(request_event(
            url=PICTURE_URL,
            namespace=PICTURE_NAMESPACE,
            offer='https://example.com/offer'
        )),
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)
    output_offset = 0
    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    # скачали картинку
    data = {0: [
        pack_message(image_event(
            url=PICTURE_URL,
            mds_json=generate_mds_json(
                namespace=PICTURE_NAMESPACE,
                group_id=PICTURE_GROUP,
                name=PICTURE_NAME,
            )
        )),
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)
    output_offset = 0
    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    # проверим, что появилась Mds-инфо в стейте
    raw_state = extract_all_states_from_simple_table(stand.yt_client, stand.state_table_path, decompress=True)
    state = TPicrobotState()
    state.ParseFromString(raw_state[PICTURE_URL])
    assert set(info.MdsId.Namespace for info in state.MdsInfo) == {PICTURE_NAMESPACE}

    # удалим картинку в два притопа и три прихлопа
    data = {0: [
        pack_message(delete_event(  # пометить
            url=PICTURE_URL,
            namespace=PICTURE_NAMESPACE,
            group_id=PICTURE_GROUP,
            image_name=PICTURE_NAME,
            ts=ts
        )),
        pack_message(delete_event(  # удалить
            url=PICTURE_URL,
            namespace=PICTURE_NAMESPACE,
            group_id=PICTURE_GROUP,
            image_name=PICTURE_NAME,
            ts=ts
        )),
        pack_message(delete_response_event(  # удалено
            url=PICTURE_URL,
            namespace=PICTURE_NAMESPACE,
            group_id=PICTURE_GROUP,
            image_name=PICTURE_NAME,
            isDeleted=True,
            ts=ts
        )),
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    # в стейте осталась запись без Mds-инфо но с историей удаления
    raw_state = extract_all_states_from_simple_table(stand.yt_client, stand.state_table_path, decompress=True)
    state = TPicrobotState()
    state.ParseFromString(raw_state[PICTURE_URL])
    assert not state.MdsInfo
    assert all(meta.Deleted for meta in state.DeleteMeta)
