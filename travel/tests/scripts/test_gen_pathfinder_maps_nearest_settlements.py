# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from common.tester.factories import create_pathfinder_maps_nearest_settlement, create_settlement
from travel.rasp.rasp_scripts.scripts.gen_pathfinder_maps_nearest_settlements import build_protobuf


@pytest.mark.dbuser
def test_build_protobuf():
    settlement_from = create_settlement(id=1)
    settlement_to = create_settlement(id=2)

    nearest_settlement = create_pathfinder_maps_nearest_settlement(
        settlement_from=settlement_from,
        settlement_to=settlement_to
    )

    proto = build_protobuf(nearest_settlement)
    assert proto.SettlementFrom == 1
    assert proto.SettlementTo == 2
