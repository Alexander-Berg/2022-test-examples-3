from os.path import join
from yatest.common import test_source_path
from search.geo.tools.addrsnippet.lib.blacklist import read_host_blacklist


def test_blacklisted_hosts():
    blacklist = read_host_blacklist(
        join(test_source_path('data'), 'black_list.dict'),
        join(test_source_path('data'), 'black_list_record.dict')
    )

    assert isinstance(blacklist, set)
    assert len(blacklist) == 2

    assert 'cikrf.ru' in blacklist
    assert 'example.com' not in blacklist
    assert '1gb.ru' in blacklist
