from extsearch.geo.aspects.py_lib import aspects


def test_pivo():
    assert aspects.CFG[14].Description == "пиво (для подрубрик Кафе)"
    assert aspects.CFG[14].MenuTags == ['pivo_tag']
    assert aspects.CFG[14].Bert['ru'] == 'пиво'
