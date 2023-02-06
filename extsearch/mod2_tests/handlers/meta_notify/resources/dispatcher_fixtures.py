import pytest

from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.dispatcher import (
    SQSDispatcher, DispatcherDirective
)
from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.message import (
    NotifySQSMessage, NotificationServices
)


@pytest.fixture
def http_directive(sqs_transport, http_rule, http_callback_formatter):
    return DispatcherDirective(rule=http_rule, client=sqs_transport, formatter=http_callback_formatter)


@pytest.fixture
def index_directive(index_rule, index_notifier, ugc_doc_formatter):
    return DispatcherDirective(rule=index_rule, client=index_notifier, formatter=ugc_doc_formatter)


@pytest.fixture
def directives(index_directive, http_directive):
    return {
        NotificationServices.INDEX: index_directive,
        NotificationServices.HTTP_CALLBACK: http_directive
    }


@pytest.fixture
def dispatcher(deduplicator, data_extender, directives):
    return SQSDispatcher(
        directives=directives,
        message_data_extender=data_extender,
        deduplicator=deduplicator
    )


@pytest.fixture
def dispatcher_test_message(meta_notify_message):
    meta_notify_message.notification_services = [NotificationServices.INDEX, NotificationServices.HTTP_CALLBACK]
    return NotifySQSMessage(meta_notify_message)


@pytest.fixture
def dispatcher_test_message_full_services(meta_notify_message):
    return NotifySQSMessage(meta_notify_message)

