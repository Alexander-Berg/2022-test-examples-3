from stats.logbrocker_workaround.utils import get_log_lines_since


def test_since_none_timestamp(fan_feedback_logfile, fan_feedback_loglines):
    lines = get_log_lines_since(fan_feedback_logfile, None)
    assert list(lines) == fan_feedback_loglines


def test_since_middle_timestamp(fan_feedback_logfile, fan_feedback_loglines):
    lines = get_log_lines_since(fan_feedback_logfile, "2022-07-19 00:00:00.000002")
    assert list(lines) == fan_feedback_loglines[1:]


def test_since_earlier_timestamp(fan_feedback_logfile, fan_feedback_loglines):
    lines = get_log_lines_since(fan_feedback_logfile, "2022-07-18 00:00:00.000002")
    assert list(lines) == fan_feedback_loglines


def test_since_later_timestamp(fan_feedback_logfile):
    lines = get_log_lines_since(fan_feedback_logfile, "2022-07-20 00:00:00.000002")
    assert list(lines) == []
