from search.mon.canti.back.src.controllers.yt import ExistingCypressNode, NewCypressNode, ManualConfirm, LocksQueue
from search.mon.canti.back.src.models.deploy import DeployConfig, LocksConfig
from random import randint
# import pytest


test_location = '//home/searchmon/dredd/canti/tests/'


def lock_creation(number: int = 0):
    cfg = DeployConfig(
        responsibles=["mrt0rtikize"],
        need_confirm=False,
        locks=LocksConfig(
            path='//home/searchmon/dredd/canti/tests/',
            priority=0
        )
    )
    nl = NewCypressNode(
        path=test_location+str(number),
        flow_id='test_flow',
        tasklet_id='test_tasklet',
        manual_confirm=ManualConfirm(
            need_confirm=False,
            confirmers=['mrt0rtikize']
        ),
        priority=0,
        config=cfg.dict()
    )
    result = nl.__dict__
    result['_client'] = None

    return result


def lock_fetch(number: int = 0):
    nl = ExistingCypressNode(path=test_location+str(number))
    result = nl.__dict__
    result['_client'] = None
    return result


def lock_remove(number: int = 0):
    nl = ExistingCypressNode(path=test_location+str(number))
    nl.remove_node()


def lock_in_queue():
    LocksQueue(path=test_location)


def test_lock_creation():
    number = randint(20, 30)
    nl1 = lock_creation(number)
    nl2 = lock_fetch(number)
    lock_in_queue()
    assert nl1 == nl2
    lock_remove(number)
