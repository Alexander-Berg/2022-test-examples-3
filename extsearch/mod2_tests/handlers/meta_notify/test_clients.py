from unittest import mock
import pytest

from botocore.exceptions import BotoCoreError

from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.errors import (
    MessageTransportError
)
from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.message import (
    NotificationServices
)


def test_index_client(index_notifier, ugc_data):
    index_notifier.send_message(NotificationServices.INDEX, ugc_data)


def test_index_client_error(index_notifier, ugc_data):
    index_notifier._index_pq_client.write_doc = mock.Mock(side_effect=ValueError())
    with pytest.raises(MessageTransportError):
        index_notifier.send_message(NotificationServices.INDEX, ugc_data)


def test_lb_notifier(lb_notifier, ugc_data):
    lb_notifier.send_message(NotificationServices.LB, ugc_data)


def test_lb_notifier_error(lb_notifier, ugc_data):
    lb_notifier.pq_notifier.notify_service = mock.Mock(side_effect=RuntimeError)
    with pytest.raises(MessageTransportError):
        return lb_notifier.send_message(NotificationServices.LB, ugc_data)


def test_signature_notifier(signature_notifier, mock_signatures_data):
    signature_notifier.send_message(NotificationServices.SIGNATURE, mock_signatures_data)


def test_signature_notifier_error(signature_notifier, mock_signatures_data):
    signature_notifier.write_signatures = mock.Mock(side_effect=RuntimeError)
    signature_notifier.send_message(NotificationServices.SIGNATURE, mock_signatures_data)


def test_sqs_transport(sqs_transport, http_notify_msg_maps_testing):
    sqs_transport.send_message(NotificationServices.HTTP_CALLBACK, http_notify_msg_maps_testing.to_dict())


def test_sqs_transport_error(sqs_transport, http_notify_msg_maps_testing):
    sqs_transport._boto_client.send_message = mock.Mock(side_effect=BotoCoreError())
    with pytest.raises(MessageTransportError):
        sqs_transport.send_message(NotificationServices.HTTP_CALLBACK, http_notify_msg_maps_testing.to_dict())


def test_sqs_transport_get_queue_error(sqs_transport, http_notify_msg_maps_testing):
    sqs_transport._boto_client.get_queue_url = mock.Mock(side_effect=BotoCoreError())
    with pytest.raises(MessageTransportError):
        sqs_transport.send_message(NotificationServices.HTTP_CALLBACK, http_notify_msg_maps_testing.to_dict())


def test_sqs_transport_empty_queue_error(sqs_transport, http_notify_msg_maps_testing):
    sqs_transport._boto_client.get_queue_url = mock.Mock(return_value={})
    with pytest.raises(MessageTransportError):
        sqs_transport.send_message(NotificationServices.HTTP_CALLBACK, http_notify_msg_maps_testing.to_dict())
