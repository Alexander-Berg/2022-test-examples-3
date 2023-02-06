import pytest
import io
import json
from stand import models


def test_serpset_by_metrics_filename():
    print()
    metrics_filename = 'data/10681888.json.gz'
    scales = ['RELEVANCE']
    serpset = models.Serpset(metrics_filename=metrics_filename, scales=scales)
    assert len(serpset.queries) == 2015
    assert sum(1 for query, docs in serpset if docs) == 2014
    # for all docs there're query, url and pos fields
    assert all((doc.query is not None) and (doc.url is not None) and (doc.pos is not None) for doc in serpset.docs)
    # for every scale there's at least one document containing it
    assert all(any(scale in doc.scales for doc in serpset.docs) for scale in scales)


def test_serpset_by_filename():
    print()
    data = {
        "some_query": [
            {
                "url": "some_url",
                "qid": "1332"
            }
        ]
    }
    data_filename = "data/tmp.json"
    with open(data_filename, "w") as f:
        json.dump(data, f)
    serpset = models.Serpset(filename=data_filename)
    assert len(serpset.docs) == 1
    assert serpset.docs[0].qid == "1332"


def test_serpset_actions():
    print()
    serpset = models.Serpset()
    serpset.add_query('query_1')
    serpset.add_doc('query_2', url='http://fake.url')
    assert len(serpset.queries) == 2
    assert len(serpset['query_1']) == 0
    assert len(serpset['query_2']) == 1
    assert serpset['query_2'][0].query == 'query_2'
    assert serpset['query_2'][0].url == 'http://fake.url'
    assert serpset['query_2'][0].pos == 0
    assert serpset['query_2'][0].scales is None
    assert serpset['query_2'][0].mimca is None
