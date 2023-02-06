# coding: utf-8

import pytest
import datetime

from hamcrest import assert_that, has_entries, has_items, equal_to
from yt.wrapper import ypath_join
from market.idx.generation.yatf.envs.region_cache import RegionCacheBuilderTestEnv
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.proto.delivery.delivery_calc.delivery_calc_pb2 import (
    BucketInfo,
    OutletDimensions,
    OutletGroup,
    PickupDeliveryRegion,
    PickupOptionsBucket,
    ProgramType,
)
from market.proto.delivery.region_cache_pb2 import (
    ERegionCacheRecordType,
    TBucketInfoVector,
    TBucketVector,
    TRegionCacheRecord,
)

GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'


@pytest.yield_fixture(scope="module")
def bucket_vectors(yt_server):
    yt_dir_prefix = ypath_join(get_yt_prefix(), 'mi3', MI3_TYPE, GENERATION)
    return YtTableResource(
        yt_stuff=yt_server,
        path=ypath_join(yt_dir_prefix, 'bucket_vectors'),
        data=[{
            'mbi_delivery_buckets': TBucketVector(Buckets=[2]).SerializeToString(),
            'mbi_delivery_type': ERegionCacheRecordType.COURIER_OLD,
        } for i in range(2)] + [
            {
                'mbi_delivery_buckets_info': TBucketInfoVector(
                    BucketInfos=[
                        BucketInfo(bucket_id=1467, is_new=True),
                        BucketInfo(bucket_id=1468, is_new=True),
                    ]
                ).SerializeToString(),
                'mbi_delivery_type': ERegionCacheRecordType.PICKUP,
            },
        ]
    )


@pytest.yield_fixture(scope="module")
def pickup_buckets_table(yt_server):
    yt_dir_prefix = ypath_join(get_yt_prefix(), 'mi3', MI3_TYPE, GENERATION)
    table = YtTableResource(
        yt_stuff=yt_server,
        path=ypath_join(yt_dir_prefix, 'pickup_options_buckets'),
        data=[
            {
                'id': 1467,
                'data': PickupOptionsBucket(
                    bucket_id=1467,
                    currency='rur',
                    program=ProgramType.REGULAR_PROGRAM,
                    pickup_delivery_regions=[
                        PickupDeliveryRegion(region_id=172, option_group_id=333, outlet_groups=[
                            OutletGroup(dimensions=OutletDimensions(width=1., height=1., length=1., dim_sum=1.),
                                        outlet_id=[1, 2])
                        ])
                    ]
                ).SerializeToString(),
            },
            {
                'id': 1468,
                'data': PickupOptionsBucket(
                    bucket_id=1468,
                    currency='rur',
                    program=ProgramType.REGULAR_PROGRAM,
                    pickup_delivery_regions=[
                        PickupDeliveryRegion(region_id=213, option_group_id=333, outlet_groups=[
                            OutletGroup(dimensions=OutletDimensions(width=1., height=1., length=1., dim_sum=1.),
                                        outlet_id=[3, 4])
                        ])
                    ]
                ).SerializeToString(),
            },
        ]
    )
    table.dump()
    return table.table_path


@pytest.yield_fixture(scope="module")
def region_cache(yt_server, bucket_vectors, pickup_buckets_table):
    bucket_vectors.create()
    assert_that(yt_server.get_yt_client().exists(bucket_vectors.get_path()),
                "Table {} doesn\'t exist".format(bucket_vectors.get_path()))

    yt_dir = ypath_join(get_yt_prefix(), 'mi3', MI3_TYPE, GENERATION)
    with RegionCacheBuilderTestEnv(yt_server, yt_dir, pickup_options_table=pickup_buckets_table) as rc:
        rc.verify()
        rc.execute()
        yield rc


def test_region_cache(region_cache):
    result = region_cache.result
    result.load()
    assert_that(len(result.data), equal_to(2))

    assert_that(result.data, has_items(
        has_entries({
            'cache_record': IsSerializedProtobuf(TRegionCacheRecord, {
                'Type': ERegionCacheRecordType.COURIER_OLD,
                'Buckets': [12345],
                'Regions': [2],
            }),
        })
    ))
    assert_that(result.data, has_items(
        has_entries({
            'cache_record': IsSerializedProtobuf(TRegionCacheRecord, {
                'Type': ERegionCacheRecordType.PICKUP,
                'Regions': [172, 213],
            }),
        }),
    ))
