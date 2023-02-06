from contextvars import ContextVar
from unittest import mock
from logging.config import dictConfig
import logging
from threading import Thread
from requests import Response

from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.utills import (
    create_meta_notify_dispatcher_handler, create_http_notify_handler
)
from extsearch.video.ugc.sqs_moderation.clients.client_manager import ClientManager
from extsearch.video.ugc.sqs_moderation.models.messages import (
    MetaNotifyMessage, HTTPNotifyMessage, NotificationServices
)
from extsearch.video.ugc.sqs_moderation.models.parsers import meta_notify_parser, http_notify_parser
from extsearch.video.ugc.sqs_moderation.mod2.sqs import SQSWatcherWithMetrics, SQSMetricsCollection
from extsearch.video.ugc.sqs_moderation.logcollector.handler import (
    setLogRecordFactory, LogRecordFactory, YdModerationFormatter
)
from extsearch.video.ugc.sqs_moderation.models.metrics import MetricsCollection


id_var: ContextVar = ContextVar("id_var", default="")
META_NOTIFY_QUEUE = 'meta_notify_test'
HTTP_NOTIFY_QUEUE = 'http_notification_test'


def response(status_code):
    resp = Response()
    resp.status_code = status_code
    return resp


def configure_log():
    dictConfig({
        'version': 1,
        'disable_existing_loggers': False,
        'formatters': {
            'standard': {
                'format': '%(asctime)s [%(levelname)s] %(name)s: %(message)s'
            },
        },
        'handlers': {
            'default': {
                'class': 'logging.StreamHandler',
                'formatter': 'standard',
                'level': 'DEBUG',
                'stream': 'ext://sys.stdout',
            },
        },
        'loggers': {
            '': {
                'handlers': ['default'],
                'level': 'DEBUG',
                'propagate': False
            },
            '__main__': {
                'handlers': ['default'],
                'level': 'DEBUG',
                'propagate': False
            },
        }
    })
    only_warn_loggers = ["boto3", "botocore", "urllib3", "ydb", "yt", "kikimr"]
    for logger_name in only_warn_loggers:
        logging.getLogger(logger_name).setLevel(logging.WARNING)

    setLogRecordFactory(LogRecordFactory(id_var))
    console_handler = logging.FileHandler('log.log', encoding='utf8')
    console_handler.setFormatter(YdModerationFormatter())
    root = logging.getLogger()
    root.addHandler(console_handler)


# ENV:
#   MOCK_TVM
#   CLIENTS_CONFIG
#   SQS_MODERATION_ACCOUNT
#   SQS_MODERATION_TOKEN
#   UGC_ADMIN_API_URLNotificationServices
#   REDIS_HOST
#   REDIS_PORT
def dispatcher_watcher(metrics):
    configure_log()
    client_manager = ClientManager()
    pg_mock = mock.Mock()
    pg_mock.topic_url = 'topic'
    create_mock = mock.Mock(return_value=pg_mock)

    with mock.patch.multiple(
        'extsearch.video.ugc.sqs_moderation.clients.client_manager.ClientManager',
        make_robot_pq_client=create_mock,
        make_index_pq_client=create_mock,
        make_pq_notifier=create_mock
    ):
        meta_notification_q = META_NOTIFY_QUEUE
        handler = create_meta_notify_dispatcher_handler(
            client_manager,
            http_notification_queue=HTTP_NOTIFY_QUEUE,
            meta_notify_queue=meta_notification_q,
            metrics=MetricsCollection()
        )
        handler.dispatcher.directives.pop(NotificationServices.RIGHT_HOLDER)
    watcher = SQSWatcherWithMetrics[MetaNotifyMessage](
        boto_client=client_manager.make_boto_client(),
        queue_name=meta_notification_q,
        handler=handler,
        message_parser=meta_notify_parser,
        id_var=id_var,
        delete_on_parse_error=True,
        metrics=metrics,
        empty_delay=1
    )
    return watcher


def run_dispatcher(watcher):
    with mock.patch(
        'extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.data_extender.ThumbInfoMaker.get_thumb_info',
        side_effect=lambda s: {}
    ):
        watcher.run()


def get_http_notificator_watcher(metrics):

    configure_log()
    client_manager = ClientManager()
    handler = create_http_notify_handler(
        client_manager, META_NOTIFY_QUEUE, metrics=MetricsCollection()
    )
    watcher = SQSWatcherWithMetrics[HTTPNotifyMessage](
        boto_client=client_manager.make_boto_client(),
        queue_name=HTTP_NOTIFY_QUEUE,
        handler=handler,
        message_parser=http_notify_parser,
        id_var=id_var,
        delete_on_parse_error=True,
        metrics=metrics,
        empty_delay=1
    )
    return watcher


def run_http_notificator(watcher):
    with mock.patch(
        'requests.Session.post',
        side_effect=mock.Mock(return_value=response(200))
    ):
        watcher.run()


if __name__ == '__main__':
    threads = []

    dispatchers_number = 2
    http_notificator_number = 1

    metrics = SQSMetricsCollection('time_')

    for _ in range(dispatchers_number):
        dispatcher = dispatcher_watcher(metrics)
        threads.append(Thread(target=run_dispatcher, args=(dispatcher, )))

    for _ in range(http_notificator_number):
        http_notificator = get_http_notificator_watcher(metrics)
        threads.append(Thread(target=run_http_notificator, args=(http_notificator,)))

    for thread in threads:
        thread.start()

    for thread in threads:
        thread.join()
