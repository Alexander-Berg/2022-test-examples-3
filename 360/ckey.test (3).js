'use strict';

let ckey;
let Ckey;
let mockSuper;
let mockCrypto;
let core;

beforeEach(() => {
    core = {
        req: {
            cookies: { yandexuid: '12345' }
        },
        params: {},
        auth: {
            get: jest.fn(() => ({ uid: '12345345' }))
        },
        yasm: {
            sum: jest.fn()
        },
        console: {
            error: jest.fn()
        }
    };

    jest.resetModules();
    jest.doMock('crypto');
    mockCrypto = require('crypto');
    Ckey = require('./ckey.js');
    mockSuper = require('@yandex-int/duffman').Core.Ckey.prototype;
    mockSuper.core = core;
});

describe('with empty ckey', () => {
    beforeEach(() => {
        ckey = new Ckey(core, { hmacKeys: [ '1sd7s23', '16dsfsf4' ] });
    });

    describe('check()', () => {
        it('sends signal', () => {
            ckey.check();
            expect(core.yasm.sum.mock.calls[0][0]).toBe('ckey_exception_not_exist');
        });

        it('logs error', () => {
            ckey.check();
            expect(core.console.error.mock.calls[0][0]).toBe('CKEY_ERROR');
        });
    });
});

describe('new key format (with "!")', () => {
    beforeEach(() => {
        mockSuper.ckey = 'U1TQEGGbXUzUPDnwzDdlyf4KUec=!jnqa5ezs';
        ckey = new Ckey(core, { hmacKeys: [ '1sd7s23', '16dsfsf4' ] });
    });

    describe('check()', () => {
        let check;

        it('runs check once', () => {
            check = jest.spyOn(Ckey.prototype, '_checkExist').mockImplementation(() => true);
            ckey.check();
            ckey.check();
            expect(check.mock.calls).toHaveLength(1);
        });

        it('passes on token auth without ckey', () => {
            ckey.ckey = null;
            core.auth.get = jest.fn(() => ({ isToken: true }));
            ckey.check();
            expect(ckey.isError).toBe(false);
        });
    });

    describe('ckey constructor', () => {
        it('sets all properties', () => {
            expect(ckey.yandexuid).toBe('12345');
            expect(ckey.isDeadline).toBe(true);
        });

        it('uses default yandexuid', () => {
            const _core = Object.assign({}, core, { req: {} });
            ckey = new Ckey(_core, { hmacKeys: [ '1sd7s23', '16dsfsf4' ] });

            expect(ckey.yandexuid).toBe('0');
            expect(ckey.isDeadline).toBe(true);
        });
    });

    describe('checkUid', () => {
        it('checks all hmacKeys and passes valid ckey', () => {
            mockCrypto.timingSafeEqual.mockReturnValueOnce(false).mockReturnValueOnce(true);

            const result = ckey.checkUid('12345345');

            expect(mockCrypto.createHmac.mock.calls).toHaveLength(2);
            expect(mockCrypto.createHmac.mock.calls[0]).toEqual([ 'sha1', '1sd7s23' ]);
            expect(mockCrypto.createHmac.mock.calls[1]).toEqual([ 'sha1', '16dsfsf4' ]);
            expect(mockCrypto.update.mock.calls[0]).toEqual([ '12345345:12345:1540574281000' ]);
            expect(mockCrypto.update.mock.calls[1]).toEqual([ '12345345:12345:1540574281000' ]);
            expect(mockCrypto.digest.mock.calls).toHaveLength(2);
            expect(result).toBe(true);
        });

        it('checks all hmacKeys and fails invalid ckey', () => {
            mockCrypto.timingSafeEqual.mockReturnValue(false);

            const result = ckey.checkUid('12345345');

            expect(mockCrypto.createHmac.mock.calls).toHaveLength(2);
            expect(mockCrypto.createHmac.mock.calls[0]).toEqual([ 'sha1', '1sd7s23' ]);
            expect(mockCrypto.createHmac.mock.calls[1]).toEqual([ 'sha1', '16dsfsf4' ]);
            expect(mockCrypto.update.mock.calls[0]).toEqual([ '12345345:12345:1540574281000' ]);
            expect(mockCrypto.update.mock.calls[1]).toEqual([ '12345345:12345:1540574281000' ]);
            expect(mockCrypto.digest.mock.calls).toHaveLength(2);
            expect(result).toBe(false);
        });

        describe('when crypto.timingSafeEqual is absent', () => {
            beforeEach(() => {
                jest.resetModules();
                jest.doMock('crypto');
                mockCrypto = require('crypto');
                delete mockCrypto.timingSafeEqual;
                Ckey = require('./ckey.js');
                mockSuper = require('@yandex-int/duffman').Core.Ckey.prototype;
                mockSuper.core = core;
                mockSuper.ckey = 'U1TQEGGbXUzUPDnwzDdlyf4KUec=!jnqa5ezs';
                ckey = new Ckey(core, { hmacKeys: [ '1sd7s23', '16dsfsf4' ] });
            });

            it('checks all hmacKeys and passes valid ckey', () => {
                mockCrypto.digest.mockReturnValue(Buffer.from('U1TQEGGbXUzUPDnwzDdlyf4KUec=', 'base64'));
                const result = ckey.checkUid('12345345');

                expect(result).toBe(true);
            });
            it('checks all hmacKeys and fails invalid ckey', () => {
                mockCrypto.digest.mockReturnValue(Buffer.from('XXX'));
                const result = ckey.checkUid('12345345');

                expect(result).toBe(false);
            });
        });
    });

    describe('renew', () => {

        const origDate = Date.now;
        beforeEach(() => {
            mockCrypto.digest.mockReturnValue(Buffer.from('XXX'));
            Date.now = () => 1540481231000;
        });

        afterEach(() => {
            Date.now = origDate;
        });
        it('generates ckey with first hmacKey', () => {
            const result = ckey.renew();

            expect(result).toBe('WFhY!jnq66vtk');
            expect(mockCrypto.createHmac).toBeCalledWith('sha1', '1sd7s23');
            expect(mockCrypto.createHmac).not.toBeCalledWith('sha1', '16dsfsf4');
            expect(mockCrypto.update).toBeCalledWith('12345345:12345:1540567631000');
            expect(mockCrypto.digest).toBeCalled();
            expect(ckey.isDeadline).toBe(false);
            expect(ckey.ckey).toBe('WFhY!jnq66vtk');
        });

        it('uses default user id', () => {
            core.auth.get.mockReturnValue({});

            const result = ckey.renew();

            expect(result).toBe('WFhY!jnq66vtk');
            expect(mockCrypto.createHmac).toBeCalledWith('sha1', '1sd7s23');
            expect(mockCrypto.createHmac).not.toBeCalledWith('sha1', '16dsfsf4');
            expect(mockCrypto.update).toBeCalledWith('0:12345:1540567631000');
            expect(mockCrypto.digest).toBeCalled();
            expect(ckey.isDeadline).toBe(false);
            expect(ckey.ckey).toBe('WFhY!jnq66vtk');
        });
    });

    describe('_checkContent', () => {
        let checkUid;

        afterEach(() => {
            checkUid.mockRestore();
        });

        it('throws error for invalid uid', () => {
            checkUid = jest.spyOn(Ckey.prototype, 'checkUid').mockImplementation(() => false);

            expect(() => ckey._checkContent()).toThrow();
        });

        it('sends signal for invalid uid', () => {
            checkUid = jest.spyOn(Ckey.prototype, 'checkUid').mockImplementation(() => false);

            try {
                ckey._checkContent();
            } catch (e) {
                expect(core.yasm.sum.mock.calls[0][0]).toBe('ckey_exception_invalid_hmac');
            }
        });

        it('doesn\'t throw error for valid uid', () => {
            checkUid = jest.spyOn(Ckey.prototype, 'checkUid').mockImplementation(() => true);

            expect(() => ckey._checkContent()).not.toThrow();
        });
    });

    describe('_checkExist', () => {
        it('no ckey -> send signal', () => {
            ckey.ckey = null;
            try {
                ckey._checkExist();
            } catch (e) {
                // it's fine
            }
            expect(core.yasm.sum.mock.calls[0][0]).toBe('ckey_exception_not_exist');
        });

        it('ckey exists -> does not send signal', () => {
            ckey._checkExist();
            expect(core.yasm.sum.mock.calls).toHaveLength(0);
        });
    });
});
