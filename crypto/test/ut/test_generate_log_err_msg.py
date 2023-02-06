from push_client_check import PushClientChecker


def test_no_error():
    log_data = {'status': 1}
    checker = PushClientChecker(0, 0, 0)
    assert checker.generate_log_err_msg(log_data) == []


def test_wrong_status_type():
    log_data = {
        'status': 2,
        'type': 'dir',
        'name': '/var/log/my.log'
    }
    checker = PushClientChecker(0, 0, 0)
    assert checker.generate_log_err_msg(log_data) == ['[/var/log/my.log] wrong status type: dir']


def test_commit_delay():
    log_data = {
        'name': '/var/log/my.log',
        'commit delay': 2042,
        'file offset': 1584810137,
        'bytes size': 1584810137,
        'actual name': '/var/log/crypta/ext_dmp_segments/logs-to-lb/dmp-segments.log',
        'last commit time': 1499316341,
        'last names': [
            '/var/log/crypta/ext_dmp_segments/logs-to-lb/dmp-segments.log'
            ],
        'bytes read': 6466657,
        'type': 'file',
        'file size': 1584810137,
        'fileid': 'RsiXHOEdQFekt7QBl6Lhsg',
        'inode': 298487,
        'lag': 1,
        'last send time': 1499318380,
        'status': 2
    }

    checker = PushClientChecker(-1, 100, -1)
    assert checker.generate_log_err_msg(log_data) == ['[/var/log/my.log] commit delay above threshold (2042 > 100)']


def test_send_delay():
    log_data = {
        'name': '/var/log/my.log',
        'commit delay': 2042,
        'last commit time': 1499316341,
        'type': 'file',
        'lag': 1,
        'last send time': 1499318300,
        'file offset': 1584810137,
        'file size': 1584810137,
        'status': 2
    }

    checker = PushClientChecker(10, -1, -1)
    assert checker.generate_log_err_msg(log_data) == ['[/var/log/my.log] send delay above threshold (83 > 10)']


def test_lag_size():
    log_data = {
        'name': '/var/log/my.log',
        'commit delay': 2042,
        'last commit time': 1499316341,
        'type': 'file',
        'lag': 1000,
        'last send time': 1499318380,
        'file offset': 1584810137,
        'file size': 1584810137,
        'status': 2
    }

    checker = PushClientChecker(-1, -1, 100)
    assert checker.generate_log_err_msg(log_data) == ['[/var/log/my.log] lag is above threshold (1000 > 100)']


def test_truncation():
    log_data = {
        'name': '/var/log/my.log',
        'commit delay': 2042,
        'last commit time': 1499316341,
        'type': 'file',
        'lag': 1,
        'last send time': 1499318380,
        'file offset': 1584810137,
        'file size': 1584810130,
        'status': 2
    }

    checker = PushClientChecker(-1, -1, -1)
    assert checker.generate_log_err_msg(log_data) == ['[/var/log/my.log] file has been truncated']


def test_unknown_reason():
    log_data = {
        'name': '/var/log/my.log',
        'commit delay': 2042,
        'last commit time': 1499316341,
        'type': 'file',
        'lag': 0,
        'last send time': 1499318380,
        'file offset': 1584810137,
        'file size': 1584810137,
        'status': 2
    }
    checker = PushClientChecker(-1, -1, -1)
    assert checker.generate_log_err_msg(log_data) == ['[/var/log/my.log] unknown reason']


def test_complex_reason():
    log_data = {
        'name': '/var/log/my.log',
        'commit delay': 2042,
        'last commit time': 1499316341,
        'type': 'file',
        'lag': 1000,
        'last send time': 1499318300,
        'file offset': 1584810137,
        'file size': 1584810130,
        'status': 2
    }
    checker = PushClientChecker(10, 1000, 100)
    assert checker.generate_log_err_msg(log_data) == [
        '[/var/log/my.log] '
        'commit delay above threshold (2042 > 1000), '
        'lag is above threshold (1000 > 100), '
        'send delay above threshold (83 > 10), '
        'file has been truncated'
    ]


def test_errors():
    log_data = {
        'name': '/var/log/my.log',
        'commit delay': 2042,
        'last commit time': 1499316341,
        'type': 'file',
        'lag': 0,
        'last send time': 1499318380,
        'file offset': 1584810137,
        'file size': 1584810137,
        'status': 2,
        'errors': ['error1', 'error2']
    }
    checker = PushClientChecker(-1, -1, -1)
    assert checker.generate_log_err_msg(log_data) == ['[/var/log/my.log] error1, error2']
