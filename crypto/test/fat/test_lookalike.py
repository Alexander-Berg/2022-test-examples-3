import itertools
import logging
import os
import random
import time

import pytest
import yt.wrapper as yt

import crypta.lib.python.bt.conf.conf as conf
from crypta.lib.python.bt import test_helpers
import crypta.audience.lib.tasks.lookalike as tasks
from crypta.lookalike.lib.python.utils.config import config as lal_config
from crypta.lookalike.proto import yt_node_names_pb2
from crypta.audience.test.fat.fixtures import (
    create_userdata_table,
    create_user_embeddings_table,
    create_related_goals_tables,
    verify_output_table,
)
from crypta.audience.test.fat.identifiers import (
    random_related_goals,
    random_name,
    YANDEXUIDS,
    JUNK_YANDEXUIDS,
)

logger = logging.getLogger(__name__)


def segment_count():
    return random.randint(10, 1000)


def _random_strings(count):
    return ' '.join(random_name(2) for _ in xrange(count))


class MockSegment(object):
    def __init__(self, size, segment_type, yandexuids, with_vectors=True):
        self.name = random_name(10)
        self.permanent_id = random.randint(1, 100000)
        self.volatile_id = str(hash(self.permanent_id))
        self.segment_type = segment_type
        self.users = random.sample(yandexuids, size)
        self.with_vectors = with_vectors
        self.related_goals = random_related_goals()

    @property
    def input_data(self):
        return ({'id_value': yandexuid} for yandexuid in self.users)

    @property
    def done_user_data(self):
        return ({'yandexuid': yandexuid,
                 'permanent_id': self.permanent_id,
                 'ts': int(time.time()),
                 'volatile_id': self.volatile_id,
                 'crypta_segment_type': self.segment_type}
                for yandexuid in self.users)

    @property
    def done_meta_data(self):
        return [{'permanent_id': self.permanent_id,
                 'last_updated': int(time.time()),
                 'volatile_id': self.volatile_id,
                 'count': segment_count()}
                ]


def write_table(path, data, append=False):
    logger.info('Writing data to [%s]', path)
    ytpath = yt.TablePath(path, append=append)
    yt.create('table', ytpath, recursive=True, ignore_existing=True)
    yt.write_table(ytpath, data, format='yson', raw=False)


def write_file(path, data):
    logger.info('Writing data to [%s]', path)
    yt.create('file', path, recursive=True, ignore_existing=True)
    yt.write_file(path, data)


def prepare_segment_input(segment):
    path = os.path.join(conf.paths.lookalike.input, segment.segment_type)
    path = os.path.join(path, segment.name)
    write_table(path, segment.input_data, append=True)
    create_related_goals_tables(segment.related_goals)
    yt.set_attribute(path, 'segment_id', segment.permanent_id)
    yt.set_attribute(path, 'crypta_related_goals', segment.related_goals)
    yt.set_attribute(path, 'crypta_status', 'new')
    if random.random() > 0.5:
        yt.set_attribute(path, "crypta_segment_info", {
            "content_type": "null",
            "deleted": "0",
            "geo_segment_form": "null",
            "geo_segment_type": "null",
            "id": int(segment.permanent_id),
            "lookalike_link": "2043576",
            "lookalike_value": "1",
            "owner_id": "300049303",
            "segment_type": "lookalike",
            "source_id": "0"
        })


def assure_clean():
    assure_not_exists(conf.paths.new_root)


def _input_data(**kwargs):
    random.seed(44)
    assure_clean()
    n_segments = random.randint(1, 2)

    for segment_type in ["audience"]*n_segments + ["custom", "direct"]:
        n_users = random.randint(10, 50)
        segment = MockSegment(n_users, segment_type, YANDEXUIDS, **kwargs)
        prepare_segment_input(segment)
    create_userdata_table()


@pytest.fixture
def model(prepared_local_yt):
    version = "1500000000"
    yt_node_names = yt_node_names_pb2.TYtNodeNames()

    dssm_model_path = os.path.join(
        lal_config.LOOKALIKE_VERSIONS_DIRECTORY, version, yt_node_names.DssmModelFile)
    with open("dssm_lal_model.applier") as f:
        write_file(dssm_model_path, f)

    segments_dict_path = os.path.join(
        lal_config.LOOKALIKE_VERSIONS_DIRECTORY, version, yt_node_names.SegmentsDictFile)
    with open("segments_dict") as f:
        write_file(segments_dict_path, f)

    user_embeddings_path = os.path.join(
        lal_config.LOOKALIKE_VERSIONS_DIRECTORY, version, yt_node_names.UserEmbeddingsTable)
    create_user_embeddings_table(user_embeddings_path)


@pytest.fixture(scope="function")
def only_input_data(prepared_local_yt):
    _input_data()


@pytest.fixture(scope="function")
def empty_input(prepared_local_yt):
    assure_clean()

    for segment_type in ['audience', 'direct']:
        segment = MockSegment(0, segment_type, YANDEXUIDS)
        prepare_segment_input(segment)
    create_userdata_table()


@pytest.fixture(scope="function")
def non_matching_input(prepared_local_yt):
    assure_clean()
    for segment_type in ['audience', 'direct']:
        segment = MockSegment(10, segment_type, JUNK_YANDEXUIDS)
        prepare_segment_input(segment)
    create_userdata_table()


def assure_empty_directory(path):
    if yt.exists(path):
        yt.remove(path, recursive=True)
    yt.create('map_node', path, recursive=True)


def assure_not_exists(path):
    if yt.exists(path):
        yt.remove(path, recursive=True)


def assure_directory_exists(path):
    if not yt.exists(path):
        yt.create('map_node', path, recursive=True)


def check_segment(segment):
    return (
        segment.last_output_time is not None and
        segment.last_input_time is not None and
        (segment.last_output_time - segment.last_input_time).seconds > 0
    )


def get_tables_list(dir_):
    return set(list(itertools.chain(
        *[map(lambda table: (segment_type, table),
              yt.list(os.path.join(dir_, segment_type)))
          for segment_type in yt.list(dir_)]
    )))


def _test_whole_without_training():

    conf.proto.Options.Lookalike.MaxCoverage = 1000
    input_tables = get_tables_list(conf.paths.lookalike.input)

    test_helpers.execute(tasks.interaction.EnqueueAllSegments())
    assert not get_tables_list(conf.paths.lookalike.input)
    assert yt.list(conf.paths.lookalike.segments.batches)
    test_helpers.execute(tasks.interaction.PrepareEnqueuedSegments())
    assert not yt.list(conf.paths.lookalike.segments.batches)
    assert yt.list(conf.paths.lookalike.segments.waiting)
    test_helpers.execute(tasks.prediction.PredictWaitingSegments())
    allowed_yuid = set([record["yuid"] for record in yt.read_table(conf.paths.lab.data.userdata + "{yuid}")])
    for each in yt.search(conf.paths.lookalike.output, node_type='table'):
        verify_output_table(each, allowed_yuid=allowed_yuid)

    output_tables = get_tables_list(conf.paths.lookalike.output)
    assert input_tables == output_tables
    assert yt.list(conf.paths.storage.queue)
    assert yt.exists(conf.paths.audience.dynamic.states.lookalike)


def test_empty_input(prepared_local_yt, empty_input):
    assert get_tables_list(conf.paths.lookalike.input)
    test_helpers.execute(tasks.interaction.EnqueueAllSegments())
    test_helpers.execute(tasks.interaction.PrepareEnqueuedSegments())
    output_tables = get_tables_list(conf.paths.lookalike.output)
    assert output_tables
    for each in yt.search(conf.paths.lookalike.output, node_type='table'):
        verify_output_table(each)


def test_non_matching_input(prepared_local_yt, non_matching_input, model):
    assert get_tables_list(conf.paths.lookalike.input)
    test_helpers.execute(tasks.interaction.EnqueueAllSegments())
    assert yt.list(conf.paths.lookalike.segments.batches)
    test_helpers.execute(tasks.interaction.PrepareEnqueuedSegments())
    assert get_tables_list(conf.paths.lookalike.output)
    test_helpers.execute(tasks.prediction.PredictWaitingSegments())


def test_whole_without_training(prepared_local_yt, only_input_data, model):
    _test_whole_without_training()
