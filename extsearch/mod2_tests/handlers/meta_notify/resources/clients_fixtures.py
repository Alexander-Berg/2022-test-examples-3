import pytest
import json

from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.transport_client import (
    IndexNotifier, LBNotifier, SignatureNotifier, SQSTransportClient
)

from .utills import source_path


@pytest.fixture
def ugc_data():
    with open(source_path('resources/video_data/ugc.json'), r'r') as f:
        return json.load(f)


@pytest.fixture
def index_notifier(index_pg_client):
    return IndexNotifier(
        index_pg_client
    )


@pytest.fixture
def lb_notifier(pg_notifier):
    return LBNotifier(
        pq_notifier=pg_notifier
    )


@pytest.fixture
def signature_notifier(robot_pq_client):
    return SignatureNotifier(
        robot_pq_client=robot_pq_client
    )


@pytest.fixture
def sqs_transport(boto_client):
    return SQSTransportClient(
        boto_client, 'http_queue'
    )
