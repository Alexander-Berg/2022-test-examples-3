import datetime
import mock

from crypta.lib.python.bt.workflow import execute_sync
from crypta.lib.python.zk import fake_zk_client

from crypta.graph.matching.direct import lib as tasks


PROTO_CONFIG = '''
PublicEdges {
    SourceType: CRYPTA_ID DestinationTypes: YANDEXUID
    DestinationTypes: MM_DEVICE_ID DestinationTypes: EMAIL DestinationTypes: EMAIL_MD5
    DestinationTypes: UUID DestinationTypes: LOGIN DestinationTypes: AVITO_HASH
    DestinationTypes: PUID DestinationTypes: IDFA }

PublicEdges {
    SourceType: YANDEXUID DestinationTypes: CRYPTA_ID DestinationTypes: YANDEXUID
    DestinationTypes: MM_DEVICE_ID DestinationTypes: EMAIL DestinationTypes: EMAIL_MD5
    DestinationTypes: UUID DestinationTypes: LOGIN DestinationTypes: AVITO_HASH
    DestinationTypes: PUID DestinationTypes: IDFA}

PublicEdges {
    SourceType: MM_DEVICE_ID DestinationTypes: CRYPTA_ID  DestinationTypes: YANDEXUID
    DestinationTypes: EMAIL DestinationTypes: EMAIL_MD5
    DestinationTypes: UUID DestinationTypes: LOGIN DestinationTypes: AVITO_HASH
    DestinationTypes: PUID }

PublicEdges {
    SourceType: EMAIL DestinationTypes: CRYPTA_ID  DestinationTypes: YANDEXUID
    DestinationTypes: MM_DEVICE_ID DestinationTypes: EMAIL_MD5
    DestinationTypes: UUID DestinationTypes: LOGIN DestinationTypes: AVITO_HASH
    DestinationTypes: PUID }

PublicEdges {
    SourceType: EMAIL_MD5 DestinationTypes: CRYPTA_ID  DestinationTypes: YANDEXUID
    DestinationTypes: MM_DEVICE_ID DestinationTypes: EMAIL
    DestinationTypes: UUID DestinationTypes: LOGIN DestinationTypes: AVITO_HASH
    DestinationTypes: PUID }

PublicEdges {
    SourceType: UUID DestinationTypes: CRYPTA_ID  DestinationTypes: YANDEXUID
    DestinationTypes: MM_DEVICE_ID DestinationTypes: EMAIL DestinationTypes: EMAIL_MD5
    DestinationTypes: UUID DestinationTypes: LOGIN DestinationTypes: AVITO_HASH
    DestinationTypes: PUID }

PublicEdges {
    SourceType: LOGIN DestinationTypes: CRYPTA_ID  DestinationTypes: YANDEXUID
    DestinationTypes: MM_DEVICE_ID DestinationTypes: EMAIL DestinationTypes: EMAIL_MD5
    DestinationTypes: UUID DestinationTypes: AVITO_HASH
    DestinationTypes: PUID }

PublicEdges {
    SourceType: AVITO_HASH DestinationTypes: CRYPTA_ID  DestinationTypes: YANDEXUID
    DestinationTypes: MM_DEVICE_ID DestinationTypes: EMAIL DestinationTypes: EMAIL_MD5
    DestinationTypes: UUID DestinationTypes: LOGIN
    DestinationTypes: PUID }

PublicEdges {
    SourceType: PUID DestinationTypes: CRYPTA_ID  DestinationTypes: YANDEXUID
    DestinationTypes: MM_DEVICE_ID DestinationTypes: EMAIL DestinationTypes: EMAIL_MD5
    DestinationTypes: UUID DestinationTypes: LOGIN DestinationTypes: AVITO_HASH}

PublicEdges {
    SourceType: IDFA DestinationTypes: CRYPTA_ID DestinationTypes: YANDEXUID
    DestinationTypes: EMAIL}
'''


@mock.patch('library.python.resource.find', side_effect=lambda key: (
    mock.DEFAULT, PROTO_CONFIG)[key == '/public_edges.pb.txt'])
@mock.patch.object(tasks.direct.DirectByTypes, 'root_attributes', new_callable=mock.PropertyMock)
def execute_directbytypes(run_date, mock_indev, mock_find):
    task = tasks.direct.DirectByTypes(generate_date=run_date, run_date=run_date)
    mock_indev.return_value = {'generate_date': task.generate_date, 'run_date': task.run_date, }
    execute(task)


@mock.patch('library.python.resource.find', side_effect=lambda key: (
    mock.DEFAULT, PROTO_CONFIG)[key == '/public_edges.pb.txt'])
@mock.patch.object(tasks.direct.DirectByTypes, 'root_attributes', new_callable=mock.PropertyMock)
def execute_main_task(mock_indev, mock_find):
    task = tasks.direct_main_task.DirectMainTask()
    run_date = task.run_date
    mock_indev.return_value = {'generate_date': run_date, 'run_date': run_date, }
    execute(task)


def execute(task):
    with fake_zk_client() as fake_zk:
        execute_sync(task, fake_zk, do_fork=False)


def direct_attributes():
    attributes = {
        "schema": [
            {"name": "id1", "type": "string"},
            {"name": "id1Type", "type": "string"},
            {"name": "id2", "type": "string"},
            {"name": "id2Type", "type": "string"},
            {"name": "dates", "type": "any"},
            {"name": "cryptaId", "type": "string"},
            {"name": "logSource", "type": "string"},
            {"name": "sourceType", "type": "string"}
        ]
    }
    return attributes


def households_attributes():
    attributes = {
        "schema": [
            {"name": "hhid", "type": "string"},
            {"name": "crypta_id", "type": "string"},
            {"name": "yuid", "type": "string"}
        ]
    }
    return attributes


def create_directbytypes_table(yt, config, dataset):
    vertices_no_multi_profile_by_id_type = \
        config.direct_config.paths.directbytypes.vertices_no_multi_profile_by_id_type
    version_edges = config.direct_config.paths.directbytypes.version_edges
    vertices_attributes = {
        "schema": [
            {"name": "id", "type": "string"},
            {"name": "id_type", "type": "string"},
            {"name": "cryptaId", "type": "string"},
        ]
    }

    version_edges_attributes = {
        "schema": [
            {"name": "cryptaId", "type": "uint64"},
            {"name": "id2", "type": "string"},
        ]
    }

    yt.create(
        'table',
        vertices_no_multi_profile_by_id_type,
        attributes=vertices_attributes,
        recursive=True,
        force=True,
    )
    yt.create(
        'table',
        version_edges,
        attributes=version_edges_attributes,
        recursive=True,
        force=True,
    )
    run_date = datetime.datetime.now().strftime("%Y-%m-%d")
    yt.set_attribute(vertices_no_multi_profile_by_id_type, "generate_date", run_date)
    yt.set_attribute(version_edges, "generate_date", run_date)
    vertices_recs = []
    direct_recs = []
    version_recs = []
    crypta_ids = set()
    for record in dataset:
        rec = {
            'id': record[0],
            'id_type': record[1],
            'cryptaId': record[5],
        }
        vertices_recs.append(rec)
        rec = {
            'id': record[2],
            'id_type': record[3],
            'cryptaId': record[5],
        }
        vertices_recs.append(rec)
        rec = {
            'id': record[0],
            'id_type': record[1],
            'target_id': record[2],
            'target_id_type': record[3],
        }
        direct_recs.append(rec)
        rec = {
            'id': record[2],
            'id_type': record[3],
            'target_id': record[0],
            'target_id_type': record[1],
        }
        direct_recs.append(rec)

        crypta_id = record[5]
        if crypta_id not in crypta_ids:
            rec = {
                'cryptaId': int(crypta_id),
                'id2': crypta_id + '0',
            }
            version_recs.append(rec)
            crypta_ids.add(crypta_id)

    # one double crypta_id1
    crypta_id = min(crypta_ids)
    rec = {
        'cryptaId': int(crypta_id),
        'id2': crypta_id,
    }
    version_recs.append(rec)

    yt.write_table(vertices_no_multi_profile_by_id_type, vertices_recs, format="yson")
    yt.write_table(version_edges, version_recs, format="yson")
    yt.run_sort(
        vertices_no_multi_profile_by_id_type,
        sort_by=["id_type", "id"]
    )

    _, edges_by_crypta_id = create_direct_table(yt, config, dataset)
    return yt, edges_by_crypta_id, vertices_no_multi_profile_by_id_type, version_edges, run_date


def create_main_task_table(yt, config, dataset):
    return create_directbytypes_table(yt, config, dataset)


def create_households_table(yt, config):
    """ Create fake tables for households """
    for table in ("some", "another", "hh_match", "hh_enrich"):
        path = '{}/{}'.format(config.direct_config.paths.households.base_path, table)
        yt.create('table', path, recursive=True, attributes=households_attributes())


def create_direct_table(yt, config, dataset):
    edges_by_crypta_id = config.direct_config.paths.direct.edges_by_crypta_id
    yt.create(
        'table',
        edges_by_crypta_id,
        attributes=direct_attributes(),
        recursive=True,
        force=True,
    )
    run_date = datetime.datetime.now().strftime("%Y-%m-%d")
    yt.set_attribute(edges_by_crypta_id, "generate_date", run_date)
    yt.write_table(edges_by_crypta_id, get_direct_records(dataset), format="yson")
    yt.run_sort(
        edges_by_crypta_id,
        sort_by="cryptaId"
    )
    create_households_table(yt, config)
    return yt, edges_by_crypta_id


def get_direct_record(data):
    assert type(data) in [list, tuple]
    assert len(data) == 8

    id1, id1Type, id2, id2Type, dates, cryptaId, logSource, sourceType = range(8)

    assert type(data[id1]) == str
    assert type(data[id1Type]) == str
    assert type(data[id2]) == str
    assert type(data[id2Type]) == str
    assert type(data[dates]) == list or data[dates] is None
    if type(data[dates]) == list:
        for date in data[dates]:
            assert type(date) == str
    assert type(data[cryptaId]) == str
    assert type(data[logSource]) == str
    assert type(data[sourceType]) == str

    return {
        "id1": data[id1],
        "id1Type": data[id1Type],
        "id2": data[id2],
        "id2Type": data[id2Type],
        "dates": data[dates],
        "cryptaId": data[cryptaId],
        "logSource": data[logSource],
        "sourceType": data[sourceType]
    }


def get_direct_records(data):
    return [get_direct_record(row) for row in data]
