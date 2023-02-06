import copy
import datetime
import time

from requests import codes
import retry
import yatest
from yt import yson

from crypta.lib.python import time_utils
from crypta.lib.python.yt.test_helpers import tables
from crypta.siberia.bin.common import test_helpers
from crypta.siberia.bin.common.data.proto.user_set_status_pb2 import TUserSetStatus
import crypta.siberia.bin.expirator.lib.test_helpers as expirator_test_helpers
import crypta.siberia.bin.users_uploader.lib.test_helpers as users_uploader_test_helpers
from crypta.lib.proto.identifiers import id_pb2


def test_lab_flow(env, local_ydb, local_yt, siberia_client, siberia_describer, local_mutator, local_segmentator, tvm_api, tvm_ids):
    """
    Сценарий работы в Лаборатории:
    1. Создаём юзер сет
    2. Загружаем туда пользователей через загрузчик
    3. Ждём, когда опишутся пользователи
    4. Создаём сегмент
    5. Удаляем сегмент
    6. Запускается протухатор и удаляет данные
    7. Удаляем юзер сет
    """
    ttl = int(datetime.timedelta(days=14).total_seconds())
    user_set_id = siberia_client.user_sets_add(title="XXX", ttl=ttl).UserSetId
    run_users_uploader(user_set_id, local_yt, siberia_client, tvm_ids, env)
    wait_stats_ready(siberia_client, user_set_id, expected_processed_count=4, expected_total_count=2)
    segment = siberia_client.segments_make(user_set_id, "ZZZ", rule='city == "Moscow"')
    wait_segment_ready(siberia_client, user_set_id, segment.Id)
    siberia_client.segments_remove(user_set_id, [segment.Id])
    wait_segment_removed(local_ydb, siberia_client, user_set_id)
    run_expirator(local_ydb, siberia_client, tvm_api, tvm_ids, env, crypta_frozen_time=int(time.time() + ttl))
    wait_user_set_data_removed(local_ydb, siberia_client, user_set_id)
    siberia_client.user_sets_get_stats(user_set_id)
    siberia_client.user_sets_remove(user_set_id)
    wait_user_set_removed(siberia_client, user_set_id)


def test_describe_ids_flow(env, local_ydb, local_yt, siberia_client, siberia_describer, local_mutator, local_segmentator, tvm_api, tvm_ids):
    """
    Сценарий работы при описании айдишников
    1. Просим описать айдишники
    2. Получаем описание
    3. Юзер сет протухает и исчезает
    """
    ids = id_pb2.TIds(Ids=[
        id_pb2.TId(Type="login", Value="login1"),
        id_pb2.TId(Type="login", Value="login2"),
        id_pb2.TId(Type="login", Value="login3"),
    ])
    user_set_id = siberia_client.user_sets_describe_ids(ids).UserSetId
    wait_stats_ready(siberia_client, user_set_id, expected_processed_count=3, expected_total_count=2)
    run_expirator(local_ydb, siberia_client, tvm_api, tvm_ids, env, crypta_frozen_time=int(time.time() + datetime.timedelta(days=7).total_seconds()))
    wait_user_set_removed(siberia_client, user_set_id)


@retry.retry(tries=10, delay=1)
def wait_stats_ready(siberia_client, user_set_id, expected_processed_count, expected_total_count):
    stats = siberia_client.user_sets_get_stats(user_set_id)
    assert stats.Info.Ready
    assert expected_processed_count == stats.Info.ProcessedUsersCount
    assert expected_total_count == stats.UserDataStats.Counts.Total


@retry.retry(tries=10, delay=1)
def wait_segment_ready(siberia_client, user_set_id, segment_id):
    segments = list(siberia_client.segments_search(user_set_id).Segments)
    assert 1 == len(segments)

    segment = segments[0]
    assert "ready" == segment.Status
    assert segment_id == segment.Id
    assert 2 == segment.Size

    for user in siberia_client.segments_list_users(user_set_id, segment_id).Users:
        assert "Moscow" == user.Info.Attributes["city"].Values[0]


@retry.retry(tries=10, delay=1)
def wait_segment_removed(local_ydb, siberia_client, user_set_id):
    assert 0 == len(siberia_client.segments_search(user_set_id).Segments)

    assert not local_ydb.dump_table(test_helpers.get_user_set_path(user_set_id, test_helpers.YDB_PATHS.UserSegmentsTable))
    assert not local_ydb.dump_table(test_helpers.get_user_set_path(user_set_id, test_helpers.YDB_PATHS.SegmentUsersTable))


@retry.retry(tries=10, delay=1)
def wait_user_set_data_removed(local_ydb, siberia_client, user_set_id):
    assert TUserSetStatus().MetaDataOnly == siberia_client.user_sets_get(user_set_id).Status

    assert not local_ydb.client.is_path_exists(test_helpers.get_user_set_path(user_set_id))


@retry.retry(tries=10, delay=1)
def wait_user_set_removed(siberia_client, user_set_id):
    test_helpers.assert_http_error(codes.not_found, siberia_client.user_sets_get_stats, user_set_id)


def run_users_uploader(user_set_id, local_yt, siberia_client, tvm_ids, env):
    schema = [
        {"name": "login", "type": "string", "required": True},
        {"name": "city", "type": "string", "required": True},
    ]
    schema = yson.YsonList(schema)
    schema.attributes["strict"] = True
    schema.attributes["unique_keys"] = False

    source_table = tables.get_yson_table_with_schema(yatest.common.test_source_path("data/users.yson"), "//users", schema)
    source_table.write_to_local(local_yt.get_yt_client())

    config_path = users_uploader_test_helpers.get_config_path(
        yt_proxy=local_yt.get_server(),
        source_table_path=source_table.cypress_path,
        siberia_host=siberia_client.host,
        siberia_port=siberia_client.port,
        user_set_id=user_set_id,
        tvm_src_id=tvm_ids.full_permissions,
        tvm_dst_id=tvm_ids.api,
        fields_id_types={
            "login": "login",
        },
    )
    binary = yatest.common.binary_path("crypta/siberia/bin/users_uploader/bin/crypta-siberia-upload-users")
    yatest.common.execute([binary, "--config", config_path], env=env)


def run_expirator(local_ydb, siberia_client, tvm_api, tvm_ids, env, crypta_frozen_time):
    config_path = expirator_test_helpers.get_config_path(
        ydb_endpoint=local_ydb.endpoint,
        ydb_database=local_ydb.database,
        siberia_host=siberia_client.host,
        siberia_port=siberia_client.port,
        tvm_src_id=tvm_ids.full_permissions,
        tvm_dst_id=tvm_ids.api,
        tvm_api_port=tvm_api.port,
    )
    env = copy.deepcopy(env)
    env[time_utils.CRYPTA_FROZEN_TIME_ENV] = str(crypta_frozen_time)

    binary = yatest.common.binary_path("crypta/siberia/bin/expirator/bin/crypta-siberia-expirator")
    yatest.common.execute([binary, "--config", config_path], env=env)
