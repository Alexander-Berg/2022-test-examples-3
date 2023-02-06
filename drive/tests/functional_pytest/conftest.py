import logging

import pytest
import validators
import yatest.common


@pytest.fixture(scope="class", autouse=True)
def endpoint():
    endpoint = _get_endpoint_name()
    if endpoint == "testing":
        return "https://testing.carsharing.yandex.net"
    if endpoint == "qa":
        return "https://testing.carsharing.yandex.net?backend_cluster=qa"
    if endpoint == "prestable":
        return "https://prestable.carsharing.yandex.net"
    if endpoint == "stable":
        return "https://stable.carsharing.yandex.net"
    else:
        url = validators.url(endpoint)
        if url:
            return endpoint
        TypeError("Endpoint is not url")


def _get_endpoint_name():
    endpoint_name = yatest.common.get_param("endpoint", "qa")
    return endpoint_name


@pytest.fixture(scope='class', autouse=True)
def secret():
    from utils.yav import get_version
    return get_version()


@pytest.fixture
def account():
    from utils.tus import get_tus_account, create_tus_account
    try:
        account = get_tus_account()
    except:
        create_tus_account()
        account = get_tus_account()
    logging.info(f'account {account.login}')
    return account


@pytest.fixture
def helper_account():
    from utils.tus import get_tus_account, create_tus_account
    try:
        account = get_tus_account()
    except:
        create_tus_account()
        account = get_tus_account()
    logging.info(f'account {account.login}')
    return account


@pytest.fixture(autouse=True)
def exclude_test_classes(request):
    disable_suites = yatest.common.get_param("disable_suites")
    if disable_suites and request.node.parent.name in disable_suites:
        pytest.skip(f'suite {request.node.parent.name} was disabled')
