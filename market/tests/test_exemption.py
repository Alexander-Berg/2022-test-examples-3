# coding: utf-8

import datetime
from market.idx.devtools.dutygen.lib.resources import Exemption


def test_exemption0():
    e = Exemption()
    assert(e.role is None)
    assert(e.egroup is None)
    assert(e.dayofweek is None)
    assert(e.sdate is None)
    assert(e.edate is None)


def test_exemption1():
    e = Exemption(role='Shadow')
    assert(e.role == 'Shadow')
    assert(e.egroup is None)
    assert(e.dayofweek is None)
    assert(e.sdate is None)
    assert(e.edate is None)


def test_exemption_from_str_1():
    e = Exemption.from_string('Shadow')
    assert(e.role == 'Shadow')
    assert(e.egroup is None)
    assert(e.dayofweek is None)
    assert(e.sdate is None)
    assert(e.edate is None)


def test_exemption_from_str_2():
    e = Exemption.from_string('Shadow/Fri')
    assert(e.role == 'Shadow')
    assert(e.dayofweek == 4)
    assert(e.egroup is None)
    assert(e.sdate is None)
    assert(e.edate is None)


def test_exemption_from_str_3():
    '''
    Cheching for a specific date
    '''
    e = Exemption.from_string('Shadow/2019-03-20')
    assert(e.role == 'Shadow')
    assert(e.sdate == datetime.date(2019, 03, 20))
    assert(e.edate == datetime.date(2019, 03, 20))
    assert(e.dayofweek is None)
    assert(e.egroup is None)


def test_exemption_from_str_4():
    '''
    Checking for dates interval
    '''
    e = Exemption.from_string('Shadow/2019-03-20..2019-03-30')
    assert(e.role == 'Shadow')
    assert(e.sdate == datetime.date(2019, 03, 20))
    assert(e.edate == datetime.date(2019, 03, 30))
    assert(e.dayofweek is None)
    assert(e.egroup is None)


def test_exemption_from_str_5():
    '''
    Exception group named '^red'
    '''
    e = Exemption.from_string('^red')
    assert(e.role is None)
    assert(e.sdate is None)
    assert(e.edate is None)
    assert(e.dayofweek is None)
    assert(e.egroup == '^red')


def test_exemption_from_str_6():
    e = Exemption.from_string('Shadow/^red/Fri/2019-03-20')
    assert(e.role == 'Shadow')
    assert(e.sdate == datetime.date(2019, 03, 20))
    assert(e.edate == datetime.date(2019, 03, 20))
    assert(e.dayofweek == 4)
    assert(e.egroup == '^red')


def test_exemption_from_str_7():
    e = Exemption.from_string('Shadow/2019-03-20..')
    assert(e.role == 'Shadow')
    assert(e.sdate == datetime.date(2019, 03, 20))
    assert(e.edate is None)
    assert(e.dayofweek is None)
    assert(e.egroup is None)


def test_exemption_from_str_8():
    e = Exemption.from_string('2019-03-20..')
    assert(e.sdate == datetime.date(2019, 03, 20))
    assert(e.role is None)
    assert(e.edate is None)
    assert(e.dayofweek is None)
    assert(e.egroup is None)


def test_exemption_from_str_reason():
    '''
    Reason is set to 'Reserved'
    '''
    e = Exemption.from_string('Reserved/2019-06-01')
    assert(e.role is None)
    assert(e.sdate == datetime.date(2019, 06, 1))
    assert(e.edate == datetime.date(2019, 06, 1))
    assert(e.dayofweek is None)
    assert(e.egroup is None)
    assert(e.reason == 'Reserved')
