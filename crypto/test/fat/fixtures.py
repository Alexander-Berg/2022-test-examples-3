import random

import numpy
import yt.wrapper as yt

from identifiers import (
    YANDEXUIDS,
    JUNK_YANDEXUIDS,
    OTHER_YANDEXUIDS,
)

import crypta.lib.python.bt.conf.conf as conf
import crypta.audience.lib.tasks  # noqa
from crypta.audience.lib.tasks.audience.tables import (
    Attributes,
    Input,
    Output,
)
from crypta.lab.lib.tables import (
    UserData,
)
from crypta.lib.proto.user_data.math_pb2 import (
    TVectorType,
)
from crypta.lib.proto.user_data.user_data_pb2 import (
    TUserData,
)
from crypta.lib.proto.user_data.enums_pb2 import (
    TAge,
    TCity,
    TCountry,
    TDevice,
    TIncome,
    TGender,
)
from crypta.lib.python.native_yt import (
    run_native_map_reduce_with_combiner,
)
from crypta.lib.python.yt import (
    schema_utils,
)
from crypta.lookalike.proto.user_embedding_pb2 import TUserEmbedding


STATS_FIELDS = (
    Output.Stats.SEX,
    Output.Stats.AGE,
    Output.Stats.REGION,
    Output.Stats.DEVICE,
    Output.Stats.UNIQ_YUID,
    Output.Stats.UNIQ_ID_VALUE,
)

OUTPUT_FIELDS = (
    Output.Fields.ID_VALUE,
    Output.Fields.SEND,
)


def create_related_goals_tables(related_goals):
    def _data():
        return [{Input.Fields.ID_VALUE: id_value}
                for id_value in random.sample(YANDEXUIDS + JUNK_YANDEXUIDS, 5)]

    for goal in related_goals:
        goal_path = yt.ypath_join(conf.paths.audience.related_goals, str(int(goal) % 10), goal)
        if not yt.exists(goal_path):
            yt.create('table', goal_path, recursive=True)
            yt.write_table(goal_path, _data())


def create_userdata_table(seed=42):

    def _record(yuid):
        userdata = TUserData()
        userdata.Yandexuid = yuid

        vectors = userdata.Vectors
        vectors.Vector.CopyFrom(TVectorType(Data=numpy.random.randn(512)))

        attributes = userdata.Attributes
        attributes.Gender = random.choice(TGender.values())
        attributes.Region = random.choice([0, 1, 217, 213, 2])
        attributes.Age = random.choice(TAge.values())
        attributes.Device = random.choice(TDevice.values())
        attributes.Country = random.choice(TCountry.values())
        attributes.City = random.choice(TCity.values())
        attributes.HasCryptaID = random.choice([True, False])

        segments = userdata.Segments.Segment
        for _segment in random.sample(Output.CRYPTA_INTERESTS, 5) + random.sample(Output.CRYPTA_SEGMENTS, 5):
            segments.add().CopyFrom(_segment)

        result = dict(
            yuid=userdata.Yandexuid,
            Vectors=userdata.Vectors.SerializeToString(),
            Attributes=userdata.Attributes.SerializeToString(),
            Segments=userdata.Segments.SerializeToString(),
        )

        return result

    def _records():
        known = [_record(yuid) for yuid in YANDEXUIDS]
        unknown = [_record(yuid) for yuid in OTHER_YANDEXUIDS]
        return sorted(known + unknown, key=lambda record: record[Output.Fields.YUID])

    if seed is not None:
        random.seed(seed)
    userdata = conf.paths.lab.data.userdata
    userdata_stats = conf.paths.lab.data.crypta_id.userdata_stats
    if not yt.exists(userdata):
        yt.create('table', userdata, recursive=True, attributes={Attributes.SCHEMA: UserData.SCHEMA})
    yt.write_table(userdata, _records())
    yt.run_sort(userdata, userdata, sort_by=Output.Fields.YUID)

    with yt.Transaction() as transaction:
        proxy = yt.config['proxy']['url']
        run_native_map_reduce_with_combiner(
            "TTransformUserDataToUserDataStats",
            "TMergeUserDataStats",
            "TMergeUserDataStats",
            userdata,
            userdata_stats,
            reduce_by=UserData.Fields.GROUP_ID,
            sort_by=UserData.Fields.GROUP_ID,
            proxy=proxy,
            transaction=str(transaction.transaction_id),
            pool=conf.yt.pool,
            title=str("ComputeUserDataStats"),
            spec={}
        )
    return userdata


def create_user_embeddings_table(user_embeddings_path, seed=42):

    def _record(yuid):
        result = dict(
            user_id=int(yuid),
            embedding=[float(elem) for elem in numpy.random.randn(400)],
            attributes={
                'Gender': random.choice(TGender.keys()),
                'Age': random.choice(TAge.keys()),
                'Income': random.choice(TIncome.keys()),
                'Region': random.choice([None, 1, 217, 213, 2]),
                'Device': random.choice(TDevice.keys()),
            },
        )

        return result

    def _records():
        known = [_record(yuid) for yuid in YANDEXUIDS]
        unknown = [_record(yuid) for yuid in OTHER_YANDEXUIDS]
        return sorted(known + unknown, key=lambda record: 'user_id')

    if seed is not None:
        random.seed(seed)

    if not yt.exists(user_embeddings_path):
        yt.create(
            'table',
            user_embeddings_path,
            recursive=True,
            attributes={Attributes.SCHEMA: schema_utils.get_schema_from_proto(TUserEmbedding)},
        )
    yt.write_table(user_embeddings_path, _records())


def attribute(path, which):
    value = yt.get_attribute(path, which, default=None)
    return value


def verify_output_table(path, allowed_yuid=None):
    row_count = attribute(path, 'row_count')
    overall_stats = attribute(path, Output.Attributes.OVERALL_STATS)
    for field in STATS_FIELDS:
        assert field in overall_stats
    assert overall_stats[Output.Stats.UNIQ_YUID] <= row_count
    assert overall_stats[Output.Stats.UNIQ_ID_VALUE] <= row_count

    if allowed_yuid is None:
        allowed_yuid = set(YANDEXUIDS)
    for row in yt.read_table(path):
        assert row[Output.Fields.YUID] in allowed_yuid
        assert row[Output.Fields.ID_VALUE]
        for field in OUTPUT_FIELDS:
            assert field in row
