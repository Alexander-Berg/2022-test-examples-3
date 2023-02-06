import pytest

from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.rules import (
    IndexRule, HTTPNotificationRule, LBNotificationRule, RightHolderRule, CommonRule
)
from extsearch.video.ugc.sqs_moderation.clients.callback_notifier import ServiceDescription


@pytest.fixture
def index_rule():
    return IndexRule()


@pytest.fixture(scope='session')
def http_services(clients_config):
    return {
        cc.Name: ServiceDescription(
            name=cc.Name,
            url_template=cc.UniversalUrl,
            non_retryable_codes=cc.NonRetryableNotificationResponseCode,
        )
        for cc in clients_config.ClientService
    }


@pytest.fixture(scope='session')
def http_rule(http_services):
    return HTTPNotificationRule(services=http_services)


@pytest.fixture
def lb_rule():
    return LBNotificationRule()


@pytest.fixture
def rightholder_rule():
    return RightHolderRule()


@pytest.fixture
def common_rule():
    return CommonRule()
