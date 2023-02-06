from sendr_taskqueue.background import BackgroundSchedule, BackgroundTask


def test_background_schedule_init():
    s = BackgroundSchedule(BackgroundTask())

    assert len(s.tasks) == 1
