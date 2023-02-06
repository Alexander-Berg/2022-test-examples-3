# -*- coding: utf-8 -*-
import pytest

from rtcc.core.session import Session
from rtcc.dataprovider.topology import TopologyProvider
from rtcc.view.topology import TopologyDiffView

SESSION_A = Session()
SESSION_B = Session()


@pytest.mark.long
def test_complete():
    SESSION_A.get(TopologyProvider)().get(type="cms", expression="C@ONLINE", args=())
    SESSION_B.get(TopologyProvider)().get(type="cms", expression="C@ONLINE", args=())
    SESSION_B.get(TopologyProvider)().get(type="cms", expression="C@HEAD", args=())

    assert TopologyDiffView(SESSION_A, SESSION_B).view()
