from ora2pg.tools.imap_pager import ImapPager
from collections import namedtuple

F = namedtuple('F', ('fid', 'next_imap_id'))
M = namedtuple('M', ('mid', 'coords'))
C = namedtuple('C', ('fid', 'imap_id'))


def make_m(mid, fid, imap_id=None):
    return M(mid, C(fid, imap_id))

FID = 1
MID = 42
START_IMAP_ID = 5


def test_generate_ok():
    pager = ImapPager([F(FID, START_IMAP_ID)])
    for i in range(5):
        assert pager(make_m(i, FID)) == START_IMAP_ID + i


def test_prompt_lesser_imap_id():
    pager = ImapPager([F(FID, START_IMAP_ID)])
    imap_id = 3
    assert pager(make_m(MID, FID, imap_id)) == imap_id
    assert pager(make_m(666, FID)) == START_IMAP_ID


def test_raise_on_bigger_imap_id():
    pager = ImapPager([F(FID, START_IMAP_ID)])
    imap_id = 666
    pager(make_m(MID, FID, imap_id))
    assert pager(make_m(MID + 1, FID)) == 667


def test_raise_on_unexisted_fid():
    pager = ImapPager([F(FID, START_IMAP_ID)])
    assert pager(make_m(MID, FID + 1, 1)) == 1
