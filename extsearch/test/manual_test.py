import yatest.common


def _test(uid):
    txt = yatest.common.binary_path(
        'extsearch/geo/tools/similar_orgs/analyze_sessions/test/sessions/session_{}.txt'.format(uid)
    )
    exe = yatest.common.binary_path('extsearch/geo/tools/similar_orgs/analyze_sessions/analyze_sessions')

    return yatest.common.canonical_execute(exe, args=['-f', txt])


def test_jan2018_desktop_1org_bsu():
    return _test('y7324484641516127422')


def test_jan2018_touch_1org_bsuir():
    return _test('y9267941441516570069')


def test_jan2018_touch_orgMn_cafe():
    return _test('y7870475431516610715')


def test_jan2018_touch_orgMn_mcdonalds():
    return _test('y1450194301516614649')


def test_jan2018_touch_orgMn_pharmacy():
    return _test('y1796497451516609162')


def test_feb2018_touch_1org_rash_bseu_deepuse():
    return _test('y8547488071519516161')


def test_feb2018_touch_1org_rash_bseu_similars():
    return _test('y1586970401519516623')
