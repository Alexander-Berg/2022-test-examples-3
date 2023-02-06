# -*- coding: utf-8 -*-
from rtcc.core.config import Config
from rtcc.core.config import viewitem
from rtcc.core.session import Session

SESSION_A = Session()
SESSION_B = Session()


class ViewCfg(Config):
    def get_confdata(self):
        return

    def __init__(self, array):
        super(ViewCfg, self).__init__(config_id=None)
        self.array = array

    @viewitem(tags=["template"])
    def view1(self):
        return "\n".join([str(item) for item in self.array]) or ""


def test_none():
    cfg_a = ViewCfg(array=[])
    cfg_b = ViewCfg(array=[])
    assert '' == cfg_a.diff(cfg_b)


def test_diff():
    cfg_a = ViewCfg(array=[1, 2, 3, 4, 5])
    cfg_b = ViewCfg(array=[1, 2, 3, 5, 6])
    assert cfg_a.diff(cfg_b)
