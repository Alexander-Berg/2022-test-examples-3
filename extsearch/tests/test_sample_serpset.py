import pytest
import sample_serpset
import models
import collections


def test_sample_serpset():
    print()
    args = models.TestArgs()
    args.add('serpset', 'data/10681888.serpset_with_mimca.json')
    args.add('method', 'sample_serpset_uniformly')
    args.add('docs_per_task', 6)
    args.add('doc_shows', 20)
    args.add('toloka', 'data/10681888.toloka_tasks.json')
    sample_serpset.main(args.to_argv())

    toloka = models.Toloka(args.toloka)
    assert [task for task in toloka]
    assert all(task.query is not None
               and all(doc is not None for doc in task.docs)
               and task.choices is None
               for task in toloka)
    doc_shows = collections.defaultdict(lambda: collections.defaultdict(lambda: [0] * args.docs_per_task))
    for task in toloka:
        for pos, doc in enumerate(task.docs):
            doc_shows[task.query][doc.mimca.mds][pos] += 1
    for query in doc_shows:
        for mds in doc_shows[query]:
            shows_by_pos = doc_shows[query][mds]
            shows_sum = sum(shows_by_pos)
            assert shows_sum == args.doc_shows or shows_sum == args.doc_shows + 1
            assert max(shows_by_pos) - min(shows_by_pos) <= 2


def test_sample_serpset_with_refset():
    print()
    args = models.TestArgs()
    args.add('serpset', 'data/10681888.serpset_with_mimca.json')
    args.add('method', 'sample_serpset_refset')
    args.add('refset', 'data/refset_10_01_2017.json')
    args.add('docs_per_task', 6)
    args.add('doc_shows', 12)
    args.add('toloka', 'data/10681888.toloka_tasks.json')
    sample_serpset.main(args.to_argv())
