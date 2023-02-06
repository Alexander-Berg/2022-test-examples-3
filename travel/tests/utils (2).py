from travel.avia.library.python.proxy_pool.proxy_pool import ProxyHost


def check_proxy_host(host, expected_address, expected_login, expected_password):
    assert isinstance(host, ProxyHost)
    assert host._host == expected_address
    assert host._login == expected_login
    assert host._password == expected_password

    if expected_login and expected_password:
        assert host.get_http_uri() == 'http://{}:{}@[{}]:{}'.format(
            expected_login,
            expected_password,
            expected_address,
            host.HTTP_PORT,
        )
    elif expected_login and not expected_password:
        assert host.get_http_uri() == 'http://{}@[{}]:{}'.format(expected_login, expected_address, host.HTTP_PORT)
    else:
        assert host.get_http_uri() == 'http://[{}]:{}'.format(expected_address, host.HTTP_PORT)

    assert host.get_ftp_host() == expected_address

    if expected_login and expected_password:
        assert host.get_ftp_user('ftp_login', 'ftp.com') == '{}:{}:ftp_login@ftp.com'.format(
            expected_login,
            expected_password,
        )
    elif expected_login and not expected_password:
        assert host.get_ftp_user('ftp_login', 'ftp.com') == '{}:ftp_login@ftp.com'.format(expected_login)
    else:
        assert host.get_ftp_user('ftp_login', 'ftp.com') == 'ftp_login@ftp.com'
