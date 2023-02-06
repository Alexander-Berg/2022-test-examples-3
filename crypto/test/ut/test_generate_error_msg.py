import json

from push_client_check import PushClientChecker


def test_exception():
    checker = PushClientChecker(-1, -1, -1)
    error_msg = checker.generate_error_msg('')
    assert error_msg.startswith('Exception:')


def test_empty_json():
    checker = PushClientChecker(-1, -1, -1)
    error_msg = checker.generate_error_msg('[]')
    assert error_msg == 'empty JSON (check if push-client service is running)'


def test_unknown_error():
    data = [{
        'name': '/var/log/my.log',
        'commit delay': 2042,
        'last commit time': 1499316341,
        'type': 'file',
        'lag': 1000,
        'last send time': 1499318380,
        'file offset': 1584810137,
        'file size': 1584810137,
        'status': 1
    }]
    checker = PushClientChecker(-1, -1, -1)
    error_msg = checker.generate_error_msg(json.dumps(data))
    assert error_msg == 'unknown error'


def test_two_logs():
    data = [
        {
            'name': '/var/log/commit.log',
            'commit delay': 2042,
            'last commit time': 1499316341,
            'type': 'file',
            'lag': 1,
            'last send time': 1499318380,
            'file offset': 1584810137,
            'file size': 1584810137,
            'status': 2
        },
        {
            'name': '/var/log/lag.log',
            'commit delay': 500,
            'last commit time': 1499316341,
            'type': 'file',
            'lag': 1000,
            'last send time': 1499318380,
            'file offset': 1584810137,
            'file size': 1584810137,
            'status': 2
        }
    ]
    checker = PushClientChecker(-1, 1000, 100)
    error_msg = checker.generate_error_msg(json.dumps(data))
    assert error_msg == (
        '[/var/log/commit.log] commit delay above threshold (2042 > 1000), '
        '[/var/log/lag.log] lag is above threshold (1000 > 100)'
    )
