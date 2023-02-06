# -*- coding: utf-8 -*-
from rtcc.core.session import Session
from rtcc.core.common import ConfigurationID
from rtcc.view.multipleview import MultipleView

SESSION = Session()


def test_none():
    assert MultipleView().view()


def test_multiply():
    view = MultipleView()
    view.add_view(ConfigurationID.build("1", "1", "1", "1", "1"), "view1")
    view.add_view(ConfigurationID.build("2", "2", "2", "2", "2"), "view1")
    view.add_view(ConfigurationID.build("3", "3", "3", "3", "3"), "view3")
    view.add_skipped(ConfigurationID.build("4", "4", "4", "4", "4"))
    assert view.view()
