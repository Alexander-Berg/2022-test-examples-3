import datetime


def test_index_rule(index_rule, formatter_test_msg_video):
    result = index_rule.check_data(formatter_test_msg_video)
    assert result is True


def test_index_rule_stream(index_rule, formatter_test_msg_stream):
    result = index_rule.check_data(formatter_test_msg_stream)
    assert result is False


def test_http_notification_rule(http_rule, formatter_test_msg_video):
    http_rule.check_data(formatter_test_msg_video)


def test_age_test_video(age_test_video, http_rule, lb_rule):
    now = datetime.datetime.now(datetime.timezone.utc)

    for rule in [http_rule, lb_rule]:
        age_test_video.extended_data.video_data.create_time = now
        assert rule.check_data(age_test_video) is True

        age_test_video.extended_data.video_data.create_time = now - datetime.timedelta(days=31)
        assert rule.check_data(age_test_video) is False


def test_lb_notification(lb_rule, formatter_test_msg_video):
    lb_rule.check_data(formatter_test_msg_video)


def test_rightholder_rule(rightholder_rule, formatter_test_msg_video):
    rightholder_rule.check_data(formatter_test_msg_video)


def test_common_rule(common_rule, common_rule_test_video):
    assert common_rule.check_data(common_rule_test_video) is False
    common_rule_test_video.extended_data.video_data.transcoder_status = 'ETSDone'
    assert common_rule.check_data(common_rule_test_video) is True
