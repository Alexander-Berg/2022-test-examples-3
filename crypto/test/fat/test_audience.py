import os.path
import random
import uuid

import pytest
import yt.wrapper as yt
from yt.wrapper import (
    yson,
)

import crypta.audience.lib.tasks as tasks
from crypta.audience.lib.tasks.audience.tables import (
    Output,
)
from crypta.audience.test.fat.fixtures import (
    create_userdata_table,
    create_related_goals_tables,
    YANDEXUIDS,
    OTHER_YANDEXUIDS,
)
from crypta.audience.test.fat.identifiers import (
    random_related_goals,
)
from crypta.lib.python.bt import test_helpers
from crypta.lib.python.bt.conf import conf


def generate_meta_attrs(
    segment_id,
    id_type="yuid",
    matching_type=None,
    device_matching_type=None,
    segment_type="geo",
    source_id="0"
):
    related_goals = random_related_goals()
    create_related_goals_tables(related_goals)

    meta = {
        tasks.audience.tables.Input.Attributes.STATUS: Output.Statuses.NEW,
        tasks.audience.tables.Input.Attributes.ID_TYPE: id_type,
        tasks.audience.tables.Input.Attributes.CRYPTA_RELATED_GOALS: related_goals,
        tasks.audience.tables.Input.Attributes.SEGMENT_ID: segment_id,
        "crypta_segment_info": {
            "content_type": "null",
            "deleted": "0",
            "geo_segment_form": "circle",
            "geo_segment_type": "condition",
            "id": str(segment_id),
            "lookalike_link": "0",
            "lookalike_value": "0",
            "owner_id": "177845260",
            "segment_type": segment_type,
            "source_id": source_id
        }
    }
    if matching_type:
        meta[tasks.audience.tables.Input.Attributes.MATCHING_TYPE] = matching_type
    if device_matching_type:
        meta[tasks.audience.tables.Input.Attributes.DEVICE_MATCHING_TYPE] = device_matching_type
    return meta


def create_empty_input_batch():
    id = str(uuid.uuid4())
    path = yt.ypath_join(conf.paths.audience.input_batch, id)
    users_path = yt.ypath_join(path, 'Users')
    meta_path = yt.ypath_join(path, 'Meta')
    yt.create('map_node', path, recursive=True)
    yt.create('table', users_path)
    yt.create('table', meta_path)
    return users_path, meta_path


def create_input_batch():
    users_path, meta_path = create_empty_input_batch()
    segments = [yson.YsonUint64(random.randint(1, 1000000)) for _ in range(10)]
    records = []
    for segment_id in segments:
        records.extend((
            {
                'user_id': yson.YsonUint64(value),
                'segment_id': segment_id
            }
            for value in list(random.sample(YANDEXUIDS, 10)) + list(random.sample(OTHER_YANDEXUIDS, 4)))
        )
    yt.write_table(users_path, records)
    yt.write_table(meta_path, [
        {
            'segment_id': segment_id,
            'meta': generate_meta_attrs(segment_id)
        } for segment_id in segments
    ])


def contents(path):
    assert yt.exists(path)
    return yt.list(path, max_size=10000, absolute=True)


def table_name(path):
    return os.path.basename(path)


def table_size(path):
    return yt.get_attribute(path, 'row_count')


@pytest.fixture(scope="function")
def batched_input(request, prepared_local_yt):
    create_userdata_table()
    create_input_batch()
    return prepared_local_yt


def test_geo_batches(batched_input):
    assert yt.exists(conf.paths.audience.input_batch)
    test_helpers.execute(tasks.audience.geo.CreatePrunedUserData())
    test_helpers.execute(tasks.audience.geo.EnqueueBigBatches())
    assert len(contents(conf.paths.audience.output_batch)) == 1
