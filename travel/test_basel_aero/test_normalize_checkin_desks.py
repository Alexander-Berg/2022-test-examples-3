from travel.avia.flight_status_fetcher.sources.basel import BaselCheckinDesksNormalizer


def test_normalize_checkin_desks():
    normalizer = BaselCheckinDesksNormalizer()
    assert '' == normalizer.normalize_checkin_desks('')
    assert normalizer.normalize_checkin_desks(None) is None
    # dashes should not occur according to data, so it's not supported
    assert '1-3,4,3' == normalizer.normalize_checkin_desks('1-3,4,3')

    # letters should not occur according to data, so it's not supported
    assert '2A,1A' == normalizer.normalize_checkin_desks('2A,1A')

    assert '1-5' == normalizer.normalize_checkin_desks('1,2,3,4,5')
    assert '1-5' == normalizer.normalize_checkin_desks('5,4,3,1,2')
    assert '1-5' == normalizer.normalize_checkin_desks('5,4,3,1,2,1,1,4,3,5,1,2,2,3')
    assert '1-5' == normalizer.normalize_checkin_desks('5, 4, 3, 1, 2, 1, 1, 4, 3, 5, 1, 2, 2, 3')
    assert '1-5' == normalizer.normalize_checkin_desks('5,     4,3,1, 2, 1,   1,4, 3,5,1,2,2,3')

    assert '1-2, 4-5' == normalizer.normalize_checkin_desks('1,2,4,5')
    assert '1-2, 4-5' == normalizer.normalize_checkin_desks('5,2,4,1')
    assert '1-2, 4-5' == normalizer.normalize_checkin_desks('5,2,4,1,2,4,2,5,4,4,5,1,1')
    assert '1-2, 4-5' == normalizer.normalize_checkin_desks('5,2 ,4 ,1 ,2,4 ,2  ,5 ,4,  4 ,  5,1, 1')

    assert '1, 3, 5, 7-9' == normalizer.normalize_checkin_desks('9, 7, 8, 5, 3, 5,   1 ,   7 ,   3')
