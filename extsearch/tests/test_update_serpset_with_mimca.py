import pytest
from update_serpset_with_mimca import extract_urls, substitute_urls
import models


def test_extract_urls():
    print()
    argv_dict = {
        '--serpset': 'data/10681888.serpset.json',
        '--mimca': 'data/10681888.mimca_input.json'
    }
    argv = [''] + sum(map(list, argv_dict.items()), [])
    extract_urls.main(argv)
    mimca = models.Mimca(argv_dict['--mimca'])
    # there's at least one document
    assert mimca.data
    # all documents have url
    assert all(doc.url is not None for doc in mimca)


def test_substitute_urls():
    print()
    argv_dict = {
        '--serpset': 'data/10681888.serpset.json',
        '--mimca': 'data/10681888.mimca_output.json',
        '--processed_serpset': 'data/10681888.serpset_with_mimca.json'
    }
    argv = [''] + sum(map(list, argv_dict.items()), [])
    substitute_urls.main(argv)
