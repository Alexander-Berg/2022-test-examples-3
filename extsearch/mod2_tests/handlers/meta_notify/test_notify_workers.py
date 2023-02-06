

def test_http_worker_handle_msg(http_notify_worker,
                                http_notify_msg_games, http_notify_msg_condition):
    http_notify_msg_condition.next_status()
    assert http_notify_worker.handle(http_notify_msg_games) is True


def test_http_worker_handle_msg_unretriable_code(http_notify_worker,
                                                 http_notify_msg_games_testing, http_notify_msg_condition):
    http_notify_msg_condition.next_status()
    assert http_notify_worker.handle(http_notify_msg_games_testing) is False


def test_http_worker_handle_msg_error(http_notify_worker, http_notify_msg_maps_testing, http_notify_msg_condition):
    http_notify_msg_condition.next_status()
    assert http_notify_worker.handle(http_notify_msg_maps_testing) is False


def test_http_worker_handle_msg_not_in_progress(http_notify_worker,
                                                http_notify_msg_maps_testing, http_notify_msg_condition):
    assert http_notify_worker.handle(http_notify_msg_maps_testing) is False
    assert len(http_notify_worker._deduplicator._boto_client.messages['fake://meta_notify_queue']) == 1
