from load.projects.cloud.loadtesting.server.pager import Pager


def test_pager():
    pager = Pager('', 12)
    assert pager.offset == 0
    assert pager.page_size == 12
    pager.set_shift(12)
    token = pager.next_page_token

    pager1 = Pager(token, 11)
    assert pager1.offset == 12
    assert pager1.page_size == 11
    pager1.set_shift(11)
    token1 = pager1.next_page_token

    pager2 = Pager(token1, 10)
    assert pager2.offset == 23
    assert pager2.page_size == 10
    pager2.set_shift(2)
    assert pager2.next_page_token == ''
