from market.pylibrary.yatestwrap.yatestwrap import source_path

from market.idx.snippets.src import shopsdat2


def test_load_bad_data():
    feeds = shopsdat2.load(source_path('market/idx/snippets/test-data/input/shops-data-bad.xml'))

    assert len(feeds) == 1
    assert feeds[0].id == 10
