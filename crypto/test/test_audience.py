import random

import numpy as np
from pytest import approx, fixture

import crypta.lib.python.bt.conf.conf as conf
from crypta.audience.lib.tasks.audience import (
    _output_stats,
)
from crypta.audience.lib.tasks.audience.tables import (
    Output,
)
from crypta.audience.lib.tasks.math import (
    _similarity,
)
from crypta.audience.proto.config_pb2 import (
    TAudienceConfig,
)
from crypta.lab.lib.tables import (
    UserDataStats,
)
from crypta.lib.proto.user_data.enums_pb2 import (
    TAge,
    TGender,
    TDevice,
)
from crypta.lib.proto.user_data.math_pb2 import (
    TVectorType,
)


def _ages():
    return {str(age - 1): random.randint(0, 10) for age in (
        TAge.Value('FROM_0_TO_17'),
        TAge.Value('FROM_18_TO_24'),
        TAge.Value('FROM_25_TO_34'),
        TAge.Value('FROM_35_TO_44'),
        TAge.Value('FROM_45_TO_54'),
        TAge.Value('FROM_55_TO_99'),
    )}


def _genders():
    return {str(gender - 1): random.randint(0, 10) for gender in (
        TGender.Value('MALE'),
        TGender.Value('FEMALE'),
    )}


def _devices():
    return {str(device - 1): random.randint(0, 10) for device in (
        TDevice.Value('DESKTOP'),
        TDevice.Value('PHONE'),
        TDevice.Value('TABLET'),
    )}


def _regions():
    return {str(random.randint(1, 255)): random.randint(0, 10) for _ in xrange(10)}


@fixture(scope="module")
def config():
    import crypta.lib.python.bt.conf.resource_conf as resource_conf
    conf.use(resource_conf.find('/crypta/audience/config'))
    conf.use_proto(TAudienceConfig())


def test_output_stats_from_proto():
    attributes_stats = UserDataStats.Proto.TAttributesStats()
    ages = _ages()
    for (age, count) in ages.iteritems():
        each = attributes_stats.Age.add()
        each.Age = int(age) + 1
        each.Count = count
    genders = _genders()
    for (gender, count) in genders.iteritems():
        each = attributes_stats.Gender.add()
        each.Gender = int(gender) + 1
        each.Count = count
    devices = _devices()
    for (device, count) in devices.iteritems():
        each = attributes_stats.Device.add()
        each.Device = int(device) + 1
        each.Count = count
    regions = _regions()
    for (region, count) in regions.iteritems():
        each = attributes_stats.Region.add()
        each.Region = int(region)
        each.Count = count

    stratum_stats = UserDataStats.Proto.TStratumStats()
    distributions = UserDataStats.Proto.TDistributions()
    stats = UserDataStats.Proto()
    stats.Attributes.CopyFrom(attributes_stats)
    stats.Stratum.CopyFrom(stratum_stats)
    stats.Distributions.CopyFrom(distributions)

    total = 10
    stats.Counts.Total = total
    stats.Counts.WithData = total / 2
    stats.Counts.UniqIdValue = total
    stats.Counts.UniqYuid = total

    output_stats = _output_stats(stats)
    assert output_stats[Output.Stats.AGE] == ages
    assert output_stats[Output.Stats.SEX] == genders
    assert output_stats[Output.Stats.DEVICE] == devices
    assert output_stats[Output.Stats.REGION] == regions
    assert output_stats[Output.Stats.UNIQ_YUID] == total
    assert output_stats[Output.Stats.UNIQ_ID_VALUE] == total


def test_similarity_when_empty():
    stats = UserDataStats.Proto()

    stats.Distributions.Main.Mean.CopyFrom(TVectorType(Data=np.ones(10)))
    stats.Distributions.Main.Count = 1
    empty_stats = UserDataStats.Proto()

    assert _similarity(stats, stats) == approx(1.0)
    assert _similarity(empty_stats, stats) == 0.0
    assert _similarity(stats, empty_stats) == 0.0
    assert _similarity(empty_stats, empty_stats) == 0.0
