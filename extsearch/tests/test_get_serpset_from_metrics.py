import pytest
import get_serpset_from_metrics
import models


def _test_main():
    print()
    argv_dict = {
        '--serpset_id': '10681888',
        '--scales': 'RELEVANCE',
        '--depth': '10',
        '--serpset': 'data/10681888.serpset.json'
    }
    argv = [''] + sum(map(list, argv_dict.items()), [])
    get_serpset_from_metrics.main(argv)
    serpset = models.Serpset(filename=argv_dict['--serpset'])
    assert len(serpset.queries) == 2015
    assert sum(1 for query, docs in serpset if docs) == 2014
    assert max(len(docs) for query, docs in serpset) == int(argv_dict['--depth'])
