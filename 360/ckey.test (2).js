'use strict';

const Ckey = require('./ckey.js');

const ctor = jest.fn();

function MockCkey(core, options) {
    ctor(core, options);
}

jest.mock('@yandex-int/duffman', () => ({
    Core: {
        Ckey: MockCkey
    }
}));

jest.mock('./secrets.js', () => ({
    ckeyHmacKeys: [ 'QUFB' ]
}));

test('ckey with param & yandexuid cookie', () => {
    const core = { params: { _ckey: 'ohmy' }, req: { cookies: { yandexuid: '42' } } };

    // eslint-disable-next-line no-new
    new Ckey(core);

    expect(ctor).toHaveBeenCalledWith(core, {
        ckey: 'ohmy',
        hmacKeys: [ Buffer.from('QUFB', 'base64') ],
        yandexuid: '42'
    });
});

test('ckey without yandexuid cookie', () => {
    const core = { params: { foo: 'bar' }, req: {} };

    // eslint-disable-next-line no-new
    new Ckey(core);

    expect(ctor).toHaveBeenCalledWith(core, {
        ckey: undefined,
        hmacKeys: [ Buffer.from('QUFB', 'base64') ],
        yandexuid: '0'
    });
});
