from datetime import timedelta
import pytest
import socket

from market.idx.pylibrary.mindexer_core.generation_checks.generation_checks import GenerationChecksControl


@pytest.fixture
def control(reusable_zk):
    return GenerationChecksControl(zk=reusable_zk)


def test_no_downtime_by_default(control):
    assert not control.get_downtime('unit_sizes')
    assert not control.get_downtime('unit_sizes', detail='any_detail')
    assert not control.get_downtime('unit_sizes', host='any_host')
    assert not control.get_downtime('unit_sizes', detail='any_detail', host='any_host')


def test_downtime_whole_check(control):
    control.set_downtime('unit_sizes', ttl=timedelta(hours=1))

    assert control.get_downtime('unit_sizes')
    assert control.get_downtime('unit_sizes', detail='any_detail')
    assert control.get_downtime('unit_sizes', host=socket.getfqdn())
    assert control.get_downtime('unit_sizes', detail='any_detail', host=socket.getfqdn())


def test_downtime_by_detail(control):
    control.set_downtime('unit_sizes', detail='detail', ttl=timedelta(hours=1))

    assert control.get_downtime('unit_sizes', detail='detail')
    assert not control.get_downtime('unit_sizes')
    assert not control.get_downtime('unit_sizes', detail='other_detail')


def test_downtime_all_hosts(control):
    control.set_downtime('unit_sizes', host='*', ttl=timedelta(hours=1))

    assert control.get_downtime('unit_sizes')
    assert control.get_downtime('unit_sizes', host=socket.getfqdn())
    assert control.get_downtime('unit_sizes', detail='other_detail')
    assert control.get_downtime('unit_sizes', detail='other_detail', host=socket.getfqdn())


def test_downtime_by_short_hostname(control):
    control.set_downtime('unit_sizes', ttl=timedelta(hours=1), host=socket.gethostname())

    assert control.get_downtime('unit_sizes')
    assert control.get_downtime('unit_sizes', host=socket.getfqdn())
