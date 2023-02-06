from cron import _format_date


def test_fromat_date():
    assert _format_date('2021-02-03T00:00:00+03:00') == '2021-02-03T00:00:00+0300'
    assert _format_date('2021-02-03') == '2021-02-03T00:00:00'
    assert _format_date('2021-02-03T00:00:00.0Z') == '2021-02-03T00:00:00+0000'
