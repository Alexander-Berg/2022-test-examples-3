from hamcrest import assert_that, calling, raises, is_not

import six
if six.PY2:
    from StringIO import StringIO
else:
    from io import StringIO

from market.pylibrary.loyalty import check_loyalty

import yatest.common


def test_invalid_format():
    stream = StringIO('lalala')
    assert_that(
        calling(check_loyalty).with_args(stream),
        raises(Exception, pattern='#01 Invalid pbuf.sn stream: expected: "SNAP", got "lala"')
    )


def test_real_file():
    filename = yatest.common.source_path(
        'market/pylibrary/loyalty/tests/data/loyalty_delivery_discount.pbuf.sn'
    )
    assert_that(
        calling(check_loyalty).with_args(filename),
        is_not(raises(Exception))
    )
