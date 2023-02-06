# coding: utf-8

from StringIO import StringIO

from market.idx.marketindexer.marketindexer import models_opinions


def test():
    data = [
        ('0', '<model-data><reviews total="1"/></model-data>'),
        ('1001', '<model-data><reviews total="1"/></model-data>'),
        ('2001', '<model-data><opinions total="1" positive="1"/></model-data>'),
        ('2002', '<model-data><rating value="4.00" total="5"/><reviews total="2"/></model-data>'),
        ('3001', '<not-model-data><reviews total="2"/></not-model-data>'),
        ('3002', '<model-data><reviews total="3"/></model-data>'),
    ]
    mod2hid = {
        1001: 100,
        2001: 200,
        2002: 200,
        3001: 300,
        3002: 300,
    }
    fsrc = StringIO('\n'.join('{}\t{}'.format(k, v) for k, v in data))
    fdst = StringIO()
    models_opinions._update_opinions(fsrc, fdst, mod2hid)

    fdst.seek(0)
    models = {int(line.split('\t')[0]): line.split('\t')[1].rstrip() for line in fdst}
    assert models[0] == '<model-data><reviews total="1"/></model-data>'
    assert models[1001] == '<model-data hid="100"><reviews total="1"/></model-data>'
    assert models[2001] == '<model-data hid="200"><opinions total="1" positive="1"/></model-data>'
    assert models[2002] == '<model-data hid="200"><rating value="4.00" total="5"/><reviews total="2"/></model-data>'
    assert models[3001] == '<not-model-data><reviews total="2"/></not-model-data>'
    assert models[3002] == '<model-data hid="300"><reviews total="3"/></model-data>'
