import pytest
import models
import process_toloka


def test_process_toloka():
    print()
    args = models.TestArgs()
    args.add('serpset', 'data/10681888.serpset_with_mimca_small.json')
    args.add('toloka', 'data/10681888.toloka_small.json')
    args.add('scale', 'offline_images_score')
    args.add('processed_serpset', 'data/10681888.serpset_final.json')
    process_toloka.main(args.to_argv())
    serpset = models.Serpset(args.processed_serpset)
    assert all(all(args.scale in doc.scales
                   and len(doc.scales[args.scale]) == 2
                   and doc.scales[args.scale][0] in [0, 1, 2]
                   and doc.scales[args.scale][1] in [1, 2] for doc in docs) for query, docs in serpset)
