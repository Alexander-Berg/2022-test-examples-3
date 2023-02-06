
from google.protobuf import text_format
import extsearch.video.ugc.service.protos.service_pb2 as service_pb2

from unittest.mock import Mock


class TransactionMock(object):
    def __enter__(self):
        return 0

    def __exit__(self, exc_type, exc_val, exc_tb):
        return 0


class ClientManagerMock:

    PLAIN_MOCKED = [
        'make_boto_client', 'make_rtt_boto_client',
        'make_issue_processor',
        'publishing_services', 'services_moderation_configs',
        'make_db_client',
        'make_rtx_client', 'make_index_pq_client', 'make_robot_pq_client', 'make_pq_notifier', 'make_image_converter',
        'make_session', 'make_callback_notifier', 'make_rightholder_boto_client', 'notify_configs',
        'make_cmnt_notifier', 'make_pq_writer',
    ]

    def __init__(self):

        self.clients_config = service_pb2.TVHUploadLinkFactoryConfig()
        with open('clients_config/config_dummy.cfg', "rb") as fd:
            text_format.Parse(fd.read(), self.clients_config)

    def notification_client(self):
        client = Mock()
        client.need_notification = Mock(return_value=True)
        return client

    def make_rtt_client(self):
        rtt_client = Mock()
        rtt_client.create_task = Mock(return_value={})
        return rtt_client

    def make_rtt_sqs_client(self):
        return self.make_rtt_client()

    def make_stream_generator(self):
        stream_generator = Mock()
        stream = {"FormatStr": "", "Url": ""}
        stream_generator.fix_stream = Mock(return_value=stream)
        return stream_generator

    def make_zen_yt_client(self):
        yt_client = Mock()
        yt_client.exists = Mock(return_value=True)
        yt_client.remove = Mock()
        yt_client.create = Mock()
        yt_client.write_table = Mock()
        yt_client.Transaction = TransactionMock
        return yt_client

    def __getattr__(self, item):
        if item in self.PLAIN_MOCKED:
            return Mock()
        raise ValueError(f'Attribute {item} not implemented in client manager mock')
