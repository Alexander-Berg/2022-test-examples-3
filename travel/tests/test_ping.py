import pytest

from travel.library.python.avia_mdb_replica_info.avia_mdb_replica_info import ping

_ya_ru_ping = """
PING ya.ru(ya.ru (2a02:6b8::2:242)) 56 data bytes
64 bytes from ya.ru (2a02:6b8::2:242): icmp_seq=1 ttl=60 time=34.2 ms
64 bytes from ya.ru (2a02:6b8::2:242): icmp_seq=2 ttl=60 time=37.7 ms
64 bytes from ya.ru (2a02:6b8::2:242): icmp_seq=3 ttl=60 time=37.8 ms
64 bytes from ya.ru (2a02:6b8::2:242): icmp_seq=4 ttl=60 time=37.7 ms

--- ya.ru ping statistics ---
4 packets transmitted, 4 received, 0% packet loss, time 3005ms
rtt min/avg/max/mdev = 34.273/36.881/37.817/1.512 ms
"""


def test_ping_success(mocker):
    class PopenMock(mocker.Mock):
        def communicate(self):
            return _ya_ru_ping, ''

    mocker.patch('subprocess.Popen', PopenMock())
    assert ping.ping_hosts(['ya.ru']) == {'ya.ru': 36.881}


def test_ping_failure(mocker):
    class PopenMock(mocker.Mock):
        def communicate(self):
            return '', 'ping: non.existing.domain: Name or service not known'

    mocker.patch('subprocess.Popen', PopenMock())

    with pytest.raises(Exception):
        ping.ping_hosts(['non.existing.domain'])


def test_create_ping_command(mocker):
    mocker.patch('os.uname', mocker.Mock(return_value=('Linux', '', '', '', '')))
    ping_cmd = ping.create_ping_command('yandex.ru', 41, 42, 43)
    assert ping_cmd == ['ping6', '-c', '41', '-i', '43', '-w', '42', 'yandex.ru']

    mocker.patch('os.uname', mocker.Mock(return_value=('Darwin', '', '', '', '')))
    ping_cmd = ping.create_ping_command('yandex.ru', 41, 42, 43)
    assert ping_cmd == ['ping6', '-c', '41', '-i', '43', 'yandex.ru']
