'use strict';

const {
    AUTH_ERROR,
    EXTERNAL_ERROR,
} = require('../helpers/errors/index.js');

describe('error AUTH_ERROR', function() {
    describe('#constructor', function() {
        it('должен вернуть AUTH_ERROR c кодом UNKNOWN_ERROR, если передана не EXTERNAL_ERROR', function() {
            const err = new AUTH_ERROR();
            expect(err).toBeInstanceOf(AUTH_ERROR);
            expect(err.error).toEqual({ code: 'AUTH_UNKNOWN' });
        });

        it('должен вернуть EXTERNAL_ERROR, если передана EXTERNAL_ERROR c не правильным кодом', function() {
            const err1 = new AUTH_ERROR(new EXTERNAL_ERROR({ code: 0 }));
            const err2 = new AUTH_ERROR(new EXTERNAL_ERROR({}));

            expect(err1).toBeInstanceOf(EXTERNAL_ERROR);
            expect(err1.error).toEqual({ code: 0 });

            expect(err2).toBeInstanceOf(EXTERNAL_ERROR);
            expect(err2.error).toEqual({});
        });

        it('должен вернуть AUTH_ERROR с правильным кодом, если передана EXTERNAL_ERROR c правильным кодом',
            function() {
                const err1 = new AUTH_ERROR(new EXTERNAL_ERROR({ code: 2001 }));
                const err2 = new AUTH_ERROR(new EXTERNAL_ERROR({ code: 2012 }));

                expect(err1).toBeInstanceOf(AUTH_ERROR);
                expect(err1).toBeInstanceOf(AUTH_ERROR.AUTH_NO_AUTH);
                expect(err1.error).toEqual({ code: 'AUTH_NO_AUTH' });


                expect(err2).toBeInstanceOf(AUTH_ERROR);
                expect(err2).toBeInstanceOf(AUTH_ERROR.AUTH_USER_BLOCKED);
                expect(err2.error).toEqual({ code: 'AUTH_USER_BLOCKED' });
            }
        );

        it('должен правильно создавать ошибки', function() {
            expect(new AUTH_ERROR()).toBeInstanceOf(AUTH_ERROR.AUTH_UNKNOWN);
            expect(new AUTH_ERROR({ code: 2001 })).toBeInstanceOf(AUTH_ERROR);
            expect(new AUTH_ERROR({ code: 2003 })).toBeInstanceOf(AUTH_ERROR);
            expect(new AUTH_ERROR({ code: 2004 })).toBeInstanceOf(AUTH_ERROR);
            expect(new AUTH_ERROR({ code: 2008 })).toBeInstanceOf(AUTH_ERROR);
            expect(new AUTH_ERROR({ code: 2011 })).toBeInstanceOf(AUTH_ERROR);
            expect(new AUTH_ERROR({ code: 2012 })).toBeInstanceOf(AUTH_ERROR);
            expect(new AUTH_ERROR({ code: 2021 })).toBeInstanceOf(AUTH_ERROR);
        });

        it('Должен вернуть AUTH_ERROR с правильным кодом, если передали EXTERNAL_ERROR с ' +
            'названием ошибки вместо кода',
        function() {
            const err = new AUTH_ERROR(new EXTERNAL_ERROR({ code: 'AUTH_NO_AUTH', info: { test: 'test' } }));

            expect(err).toBeInstanceOf(AUTH_ERROR.AUTH_NO_AUTH);
            expect(err.error).toEqual({ code: 'AUTH_NO_AUTH', info: { test: 'test' } });
        }
        );

        it('должен вернуть AUTH_ERROR с info, если есть поле info у переданной ошибки', function() {
            const err = new AUTH_ERROR(new EXTERNAL_ERROR({ code: 2001, info: { test: 'test' } }));

            expect(err).toBeInstanceOf(AUTH_ERROR.AUTH_NO_AUTH);
            expect(err.error).toEqual({ code: 'AUTH_NO_AUTH', info: { test: 'test' } });
        });
    });
});
