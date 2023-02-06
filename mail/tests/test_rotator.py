from mail.duty.rotate_maildev_duty.lib.rotator import get_responsibles


def test_get_responsibles():
    assert get_responsibles("dskut", "prez") == ["dskut", "prez", "imdex"]
    assert get_responsibles("dskut", "kremenkov") == ["dskut", "kremenkov", "prez"]
    assert get_responsibles("dskut", "imdex") == ["dskut", "imdex", "prez"]
    assert get_responsibles("prez", "dskut") == ["prez", "dskut", "imdex"]
    assert get_responsibles("kremenkov", "dskut") == ["kremenkov", "dskut", "prez"]
    assert get_responsibles("imdex", "dskut") == ["imdex", "dskut", "prez"]
    assert get_responsibles("imdex", "kremenkov") == ["imdex", "kremenkov", "prez"]
    assert get_responsibles("prez", "imdex") == ["prez", "imdex", "dskut"]
    assert get_responsibles("alexandr21", "alexandr21") == ["alexandr21", "prez", "imdex"]
