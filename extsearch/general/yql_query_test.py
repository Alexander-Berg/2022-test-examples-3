import os

from extsearch.video.robot.cm.library.yql_util import YqlQuery


class Config(object):
    def __init__(self, obj=None):
        self.StrVal = 'Hello world'
        self.FloatVal = 3.14
        self.IntVal = 44
        self.ObjectVal = obj


config_ = Config(Config())


def test_subst1():
    os.environ.clear()
    canonical = 'SELECT "Hello world",3.14,44'
    assert YqlQuery('SELECT "{{config.StrVal}}",{{config.FloatVal}},{{config.IntVal}}').subst(config=config_) == canonical


def test_subst2():
    os.environ.clear()
    canonical = 'SELECT 44,44 FROM `table` ORDER BY 1'
    assert YqlQuery('SELECT {{config.IntVal}},{{config.IntVal}} FROM `{{input1}}` ORDER BY 1').subst(config=config_, input1='table') == canonical


def test_subst3():
    os.environ.clear()
    canonical = 'SELECT 3.14 as value'
    assert YqlQuery('SELECT {{config.ObjectVal.FloatVal}} as value').subst(config=config_) == canonical


def test_headers():
    os.environ.clear()
    os.environ['YT_PROXY'] = 'test'
    os.environ['YT_POOL'] = 'test_pool'
    canonical = """-- GENERATED HEADERS
USE test;
PRAGMA yt.Pool = "test_pool";
-- GENERATED HEADERS
SELECT 3.14 as value"""
    assert YqlQuery('SELECT {{config.ObjectVal.FloatVal}} as value').subst(config=config_) == canonical


def test_dummy():
    os.environ.clear()
    query = 'SELECT 1 as value'
    assert YqlQuery(query).subst() == query


def test_except1():
    try:
        YqlQuery('SELECT {{config.LostVal}}').subst(config=config_)
    except:
        return
    raise Exception('Exception expected')


def test_except2():
    try:
        YqlQuery('SELECT {{lost_val}}').subst(config=config_, passed_val=10)
    except:
        return
    raise Exception('Exception expected')


def test_except3():
    try:
        YqlQuery('SELECT {{}}').subst(config=config_)
    except:
        return
    raise Exception('Exception expected')
