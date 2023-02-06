

def test_extend_video_data(data_extender, meta_notify_message):
    data_extender.extend_message_data(meta_notify_message)


def test_extend_stream_data(data_extender, stream_meta_notify_message):
    data_extender.extend_message_data(stream_meta_notify_message)
