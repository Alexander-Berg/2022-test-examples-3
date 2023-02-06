import hashlib
import mock

from crypta.lib.python.identifiers.identifiers import GenericID
from crypta.lab import lib as tasks
from crypta.lab.proto.view_pb2 import TSampleView
from crypta.lib.python.bt.workflow import execute_sync
from crypta.lib.python.zk import fake_zk_client
from data import (
    READY,
    sample_id,
    src_view,
    dst_view,
    valid_normalized,
    valid_unnormalized,
    valid_value,
    without_normalized,
    valid_cryptaid_1,
    valid_cryptaid_5,
    get_type,
    LAB_ID_YANDEXUID,
    LAB_ID_EMAIL,
    LAB_ID_PHONE,
    LAB_ID_LOGIN,
    LAB_ID_MM_DEVICE_ID,
    LAB_ID_PUID,
    LAB_ID_UUID,
    LAB_ID_CRYPTA_ID,
    HM_IDENTITY,
    HM_MD5,
    HM_SHA256,
    CRYPTA_ID_STATISTICS,
    IN_DEVICE,
)


def match_assert_test(yt, view_params):
    hashing_method = 0
    include_original = 1
    id_type = 2
    key = 3
    view_type = 4
    scope = 5
    path = 6
    dst_table_path = view_params[dst_view][path]
    assert yt.exists(dst_table_path)
    src_idtype = view_params[src_view][id_type]
    if src_idtype not in without_normalized:
        assert yt.get_attribute(dst_table_path, "row_count") == 2
    else:
        assert yt.get_attribute(dst_table_path, "row_count") == 1
    records = list(yt.read_table(dst_table_path, raw=False))
    valid_normalized_value_1 = valid_normalized[src_idtype][0]
    if view_params[src_view][hashing_method] == HM_MD5:
        valid_normalized_value_1 = hashlib.md5(valid_normalized_value_1).hexdigest()
    dst_idtype = view_params[dst_view][id_type]
    src_key = view_params[src_view][key]
    valid_id = valid_value[dst_idtype]
    if view_params[dst_view][hashing_method] == HM_MD5:
        valid_id = hashlib.md5(valid_id).hexdigest()
    elif view_params[dst_view][hashing_method] == HM_SHA256:
        valid_id = hashlib.sha256(valid_id).hexdigest()
    else:
        assert view_params[dst_view][hashing_method] == HM_IDENTITY
    if view_params[dst_view][include_original]:
        if src_idtype not in without_normalized:
            valid_unnormalized_value_1 = valid_unnormalized[src_idtype][0]
            if view_params[src_view][hashing_method] == HM_MD5:
                valid_unnormalized_value_1 = hashlib.md5(
                    GenericID(get_type[src_idtype], valid_unnormalized_value_1).normalize).hexdigest()
            else:
                assert view_params[src_view][hashing_method] == HM_IDENTITY
            if view_params[dst_view][view_type] == CRYPTA_ID_STATISTICS:
                valid_value_1 = {get_type[dst_idtype]: 1L, get_type[dst_idtype] + "_invalid": 1L}
                valid_value_2 = {get_type[dst_idtype]: 1L, get_type[dst_idtype] + "_invalid": 1L}
            else:
                if dst_idtype != LAB_ID_CRYPTA_ID or view_params[dst_view][scope] == IN_DEVICE:
                    valid_value_1, valid_value_2 = valid_id, valid_id
                else:
                    valid_value_1, valid_value_2 = valid_cryptaid_1, valid_cryptaid_5
                    if view_params[dst_view][hashing_method] == HM_MD5:
                        valid_value_1 = hashlib.md5(valid_value_1).hexdigest()
                        valid_value_2 = hashlib.md5(valid_value_2).hexdigest()
                    elif view_params[dst_view][hashing_method] == HM_SHA256:
                        valid_value_1 = hashlib.sha256(valid_value_1).hexdigest()
                        valid_value_2 = hashlib.sha256(valid_value_2).hexdigest()
                    else:
                        assert view_params[dst_view][hashing_method] == HM_IDENTITY
            true_records = [
                {"garbage": "valid normalized", src_key: valid_normalized_value_1,
                 view_params[dst_view][key]: valid_value_1, "type": get_type[src_idtype]},
                {"garbage": "valid_unnormalized", src_key: valid_unnormalized_value_1,
                 view_params[dst_view][key]: valid_value_2, "type": get_type[src_idtype]},
            ]
            if view_params[dst_view][view_type] == CRYPTA_ID_STATISTICS:
                true_records[0]["ccIdType"] = "cryptaId"
                true_records[1]["ccIdType"] = "cryptaId"
        else:
            if view_params[dst_view][view_type] == CRYPTA_ID_STATISTICS:
                stat_record = {get_type[dst_idtype]: 1L, get_type[dst_idtype] + "_invalid": 1L}
                true_records = [
                    {"garbage": "valid normalized", src_key: valid_normalized_value_1,
                     view_params[dst_view][key]: stat_record, "type": get_type[src_idtype]},
                ]
            else:
                true_records = [
                    {"garbage": "valid normalized", src_key: valid_normalized_value_1,
                     view_params[dst_view][key]: valid_id, "type": get_type[src_idtype]},
                ]
            if view_params[dst_view][view_type] == CRYPTA_ID_STATISTICS:
                true_records[0]["ccIdType"] = "cryptaId"
    else:
        if src_idtype not in without_normalized:
            if view_params[dst_view][view_type] == CRYPTA_ID_STATISTICS:
                valid_value_1 = {get_type[dst_idtype]: 1L, get_type[dst_idtype] + "_invalid": 1L}
                valid_value_2 = {get_type[dst_idtype]: 1L, get_type[dst_idtype] + "_invalid": 1L}
            else:
                if dst_idtype != LAB_ID_CRYPTA_ID or view_params[dst_view][scope] == IN_DEVICE:
                    valid_value_1, valid_value_2 = valid_id, valid_id
                else:
                    valid_value_1, valid_value_2 = valid_cryptaid_1, valid_cryptaid_5
                    if view_params[dst_view][hashing_method] == HM_MD5:
                        valid_value_1 = hashlib.md5(valid_value_1).hexdigest()
                        valid_value_2 = hashlib.md5(valid_value_2).hexdigest()
                    elif view_params[dst_view][hashing_method] == HM_SHA256:
                        valid_value_1 = hashlib.sha256(valid_value_1).hexdigest()
                        valid_value_2 = hashlib.sha256(valid_value_2).hexdigest()
                    else:
                        assert view_params[dst_view][hashing_method] == HM_IDENTITY
            true_records = [
                {view_params[dst_view][key]: valid_value_1},
                {view_params[dst_view][key]: valid_value_2},
            ]
            if view_params[dst_view][view_type] == CRYPTA_ID_STATISTICS:
                true_records[0]["ccIdType"] = "cryptaId"
                true_records[1]["ccIdType"] = "cryptaId"
        else:
            if view_params[dst_view][view_type] == CRYPTA_ID_STATISTICS:
                true_records = [
                    {view_params[dst_view][key]: {get_type[dst_idtype]: 1L, get_type[dst_idtype] + "_invalid": 1L}},
                ]
            else:
                true_records = [
                    {view_params[dst_view][key]: valid_id},
                ]
            if view_params[dst_view][view_type] == CRYPTA_ID_STATISTICS:
                true_records[0]["ccIdType"] = "cryptaId"

    records.sort()
    true_records.sort()
    assert records == true_records


def get_view(params):
    def _get_view(_, view_id):
        hashing_method = 0
        include_original = 1
        id_type = 2
        key = 3
        view_type = 4
        scope = 5
        path = 6

        view = TSampleView()
        view.ID = u"ID"
        view.SampleID = sample_id
        view.Path = params[view_id][path]
        view.State = READY
        view.Options.DerivedFrom = u"DerivedFrom"
        view.Options.Matching.HashingMethod = params[view_id][hashing_method]
        view.Options.Matching.IncludeOriginal = params[view_id][include_original]
        view.Options.Matching.IdType = params[view_id][id_type]
        view.Options.Matching.Key = params[view_id][key]
        view.Type = params[view_id][view_type]
        view.Options.Matching.Scope = params[view_id][scope]

        return view

    return _get_view


def execute_match(view_params, **task_params):
    with mock.patch.object(tasks.match.Match, "get_view", new=get_view(view_params)),\
            mock.patch.object(tasks.match.Match, "update_sample_view_state", new=lambda self, id, view_id, state: ""),\
            mock.patch.object(tasks.match.Match, "get_min_size", new=lambda self, id, view_id: -1):
        execute(tasks.match.Match(**task_params))


def execute(task):
    with fake_zk_client() as fake_zk:
        execute_sync(task, fake_zk, do_fork=False)


def match_attributes(config, path):
    if path == config.paths.graph.vertices_no_multi_profile:
        attributes = {
            "schema": [
                {
                    "name": "id",
                    "type": "string",
                },
                {
                    "name": "id_type",
                    "type": "string",
                },
                {
                    "name": "cryptaId",
                    "type": "string"
                },
            ]
        }
    elif path == config.paths.graph.vertices_by_crypta_id:
        attributes = {
            "schema": [
                {
                    "name": "id",
                    "type": "string",
                },
                {
                    "name": "id_type",
                    "type": "string",
                },
                {
                    "name": "cryptaId",
                    "type": "string"
                },
                {
                    "name": "ccIdType",
                    "type": "string"
                },
            ]
        }
    elif path.startswith(config.paths.indevicebytypes.base_path):
        attributes = {
            "schema": [
                {
                    "name": "id",
                    "type": "string",
                },
                {
                    "name": "id_type",
                    "type": "string",
                },
                {
                    "name": "target_id",
                    "type": "string"
                },
                {
                    "name": "target_id_type",
                    "type": "string"
                },
            ]
        }
    else:
        attributes = {
            "schema": [
                {
                    "name": "garbage",
                    "type": "string",
                },
                {
                    "name": "id",
                    "type": "string",
                },
                {
                    "name": "type",
                    "type": "string",
                },
            ]
        }
    return attributes


def make_match_tables(yt, view_params):
    path = 6
    yt.create(
        'table',
        view_params[src_view][path],
        attributes=match_attributes(),
        recursive=True,
        force=True,
    )


def get_view_type(id_type):
    if id_type == LAB_ID_YANDEXUID:
        return "yandexuid"
    if id_type == LAB_ID_MM_DEVICE_ID:
        return "mm_device_id"
    if id_type == LAB_ID_EMAIL:
        return "email"
    if id_type == LAB_ID_PHONE:
        return "phone"
    if id_type == LAB_ID_PUID:
        return "puid"
    if id_type == LAB_ID_LOGIN:
        return "login"
    if id_type == LAB_ID_UUID:
        return "uuid"
    if id_type == LAB_ID_CRYPTA_ID:
        return "crypta_id"
    raise ValueError("invalid value of scope view in matching options")


def get_path(base_path, src_view, dst_view):
    hashing_method = 0
    id_type = 2
    type_in = get_view_type(src_view[id_type])
    if src_view[hashing_method] == HM_MD5:
        type_in += "_md5"
    type_out = get_view_type(dst_view[id_type])
    assert type_in != type_out

    if type_in == "crypta_id" or type_out == "crypta_id":
        path = base_path + "/" + type_in + "/" + type_out
    else:
        path = base_path + "/" + type_in + "/direct/" + type_out
    return path


def create_match_tables(yt, config, dataset, view_params):
    vertices = config.paths.graph.vertices_no_multi_profile
    vertices_by_crypta_id = config.paths.graph.vertices_by_crypta_id
    if view_params[dst_view][5] == IN_DEVICE:
        indevice_table = get_path(config.paths.indevicebytypes.base_path, view_params[src_view], view_params[dst_view])
        yt.create(
            'table',
            indevice_table,
            attributes=match_attributes(config, indevice_table),
            recursive=True,
            force=True,
        )
        yt.write_table(indevice_table, dataset[indevice_table])
        yt.run_sort(
            indevice_table,
            sort_by=["id", "id_type"],
        )
    yt.create(
        'table',
        vertices,
        attributes=match_attributes(config, vertices),
        recursive=True,
        force=True,
    )
    yt.write_table(vertices, dataset[vertices])
    yt.run_sort(
        vertices,
        sort_by="id",
    )
    yt.create(
        'table',
        vertices_by_crypta_id,
        attributes=match_attributes(config, vertices_by_crypta_id),
        recursive=True,
        force=True,
    )
    yt.write_table(vertices_by_crypta_id, dataset[vertices_by_crypta_id])
    yt.run_sort(
        vertices_by_crypta_id,
        sort_by="cryptaId",
    )
    path = 6
    yt.create(
        'table',
        view_params[src_view][path],
        attributes=match_attributes(config, view_params[src_view][path]),
        recursive=True,
        force=True,
    )
    yt.write_table(view_params[src_view][path], dataset[view_params[src_view][path]])
