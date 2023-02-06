from google.protobuf import json_format
import pytest

from crypta.lib.proto.user_data import enums_pb2
from crypta.siberia.bin.custom_audience.common.proto import (
    ca_rule_pb2,
    extended_ca_rule_pb2,
)

pytest_plugins = [
    "crypta.lib.python.yt.test_helpers.fixtures",
]


def test_custom_audience(ca_client):
    result = ca_client.get_stats(ca_rule_pb2.TCaRule(
        Gender=enums_pb2.TGender.FEMALE,
    ))

    return _user_data_stats_to_dict(result)


@pytest.mark.parametrize("rule", [
    pytest.param(
        extended_ca_rule_pb2.TExtendedCaRule(
            Gender=enums_pb2.TGender.FEMALE,
            Kernel=extended_ca_rule_pb2.TKernel(
                AggregateByOr=True,
                Phrases=[
                    extended_ca_rule_pb2.TGroup(
                        Negative=False,
                        Items=[" конь  fire "],
                    ),
                    extended_ca_rule_pb2.TGroup(
                        Negative=True,
                        Items=[" конь  огонь "],
                    )
                ],  # crypta_id_6
                Apps=[
                    extended_ca_rule_pb2.TGroup(
                        Negative=True,
                        Items=["{}.app.com".format(i) for i in range(1, 10)],
                    )
                ],  # crypta_id_0
                Hosts=[
                    extended_ca_rule_pb2.TGroup(
                        Negative=False,
                        Items=["4.yandex.local"],
                    ),
                    extended_ca_rule_pb2.TGroup(
                        Negative=True,
                        Items=["6.yandex.local", "8.yandex.local"],
                    ),
                ],  # crypta_id_4
            )
        ),
        id="full-or",
    ),
    pytest.param(
        extended_ca_rule_pb2.TExtendedCaRule(
            Gender=enums_pb2.TGender.FEMALE,
            Kernel=extended_ca_rule_pb2.TKernel(
                AggregateByOr=False,
                Phrases=[
                    extended_ca_rule_pb2.TGroup(
                        Negative=False,
                        Items=["air"],
                    ),
                ],  # crypta_id_0
                Apps=[
                    extended_ca_rule_pb2.TGroup(
                        Negative=True,
                        Items=["1.app.com"],
                    )
                ],  # crypta_id_0
                Hosts=[
                    extended_ca_rule_pb2.TGroup(
                        Negative=True,
                        Items=["{}.yandex.local".format(i) for i in range(1, 10)],
                    )
                ],  # crypta_id_0
            )
        ),
        id="full-and",
    ),
    pytest.param(
        extended_ca_rule_pb2.TExtendedCaRule(
            Gender=enums_pb2.TGender.FEMALE,
            Kernel=extended_ca_rule_pb2.TKernel(
                AggregateByOr=True,
                Phrases=[
                    extended_ca_rule_pb2.TGroup(
                        Negative=False,
                        Items=[" ddd пламя "],
                    ),
                ],
                Apps=[
                    extended_ca_rule_pb2.TGroup(
                        Negative=False,
                        Items=["unknown.app.com"],
                    )
                ],
                Hosts=[
                    extended_ca_rule_pb2.TGroup(
                        Negative=False,
                        Items=["unknown.yandex.local"],
                    ),
                ],
            )
        ),
        id="full-unknown",
    ),
    pytest.param(
        extended_ca_rule_pb2.TExtendedCaRule(
            Gender=enums_pb2.TGender.FEMALE,
            Kernel=extended_ca_rule_pb2.TKernel(
                AggregateByOr=False,
                Phrases=[
                    extended_ca_rule_pb2.TGroup(
                        Negative=True,
                        Items=[" ddd пламя "],
                    ),
                    extended_ca_rule_pb2.TGroup(
                        Negative=False,
                        Items=["рама"],
                    ),
                ],
                Apps=[
                    extended_ca_rule_pb2.TGroup(
                        Negative=True,
                        Items=["unknown.app.com"],
                    )
                ],
                Hosts=[
                    extended_ca_rule_pb2.TGroup(
                        Negative=True,
                        Items=["unknown.yandex.local"],
                    ),
                ],
            )
        ),
        id="full-unknown-negative",
    ),
])
def test_get_stats_by_extended_rule(ca_client, rule):
    result = ca_client.get_stats_by_extended_rule(rule)
    return _user_data_stats_to_dict(result)


def test_get_ids(ca_client):
    result = ca_client.get_ids(ca_rule_pb2.TCaRule(
        Gender=enums_pb2.TGender.FEMALE,
    ))

    result = json_format.MessageToDict(result)
    result["Ids"].sort()
    return result


def test_ping(ca_client):
    response = ca_client.ping()
    assert "OK" == response.Message


def _user_data_stats_to_dict(proto):
    def key_func(x):
        return (x["Token"], x["Count"], x["Weight"])

    result = json_format.MessageToDict(proto)
    for aff in (result["Affinities"]["Words"], result["Affinities"]["Hosts"], result["Affinities"]["Apps"]):
        if "Token" in aff:
            aff["Token"].sort(key=key_func)

    return result
