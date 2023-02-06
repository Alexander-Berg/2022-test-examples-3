from six.moves import range

from crypta.lib.python.lb_pusher import storage
from crypta.lib.python.logbroker.test_helpers import consumer_utils


def test_lb_pusher(logbroker_config, logbroker_client):
    lb_pusher = storage.LBPusher(
        logbroker_host=logbroker_config.host,
        logbroker_port=logbroker_config.port,
        logbroker_topic=logbroker_config.topic,
        tvm_id=None,
        max_inflight=10,
        batch_size=100,
    )

    records = [str(i) for i in range(201)]

    assert [
        {'count': 3, 'status': 'ok'},
        {'count': 0, 'status': 'error'},
        {'count': 0, 'status': 'skipped'},
        {'count': 201, 'status': 'taked'},
    ] == list(lb_pusher(records))

    consumer = logbroker_client.create_consumer()
    assert records == consumer_utils.read_all(consumer)
    consumer.stop().result()
