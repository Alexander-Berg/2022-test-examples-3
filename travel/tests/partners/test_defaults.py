from travel.avia.ticket_daemon.ticket_daemon.partners import generate_marker


def test_generate_marker():
    splitted = generate_marker().split('-')
    assert 'a5e969' == splitted[1]
    assert '1' == splitted[-1][-1]
