'use strict';

const status = require('./status.js');

let core;
let subject;

beforeEach(() => {
    core = {
        res: {}
    };
    core.status = status(core);
});

describe('#ok', () => {
    it('отдает status: 1', () => {
        subject = status();
        expect(subject.ok()).toEqual({ status: { status: 1 } });
    });

    it('записывает 1 в core.res.mmapiStatus, если есть core', () => {
        subject = status(core);
        subject.ok();
        expect(core.res.mmapiStatus).toEqual(1);
    });

    it('добавляет данные', () => {
        subject = status(core);
        expect(subject.ok({ foo: 'bar' })).toEqual({
            status: { status: 1 },
            foo: 'bar'
        });
    });
});

describe('#tmpFail', () => {
    it('отдает status: 2', () => {
        subject = status();
        expect(subject.tmpFail()).toEqual({
            status: {
                status: 2,
                phrase: 'TMP_FAIL'
            }
        });
    });

    it('добавляет phrase', () => {
        subject = status();
        expect(subject.tmpFail('blabla').status).toContainKey('phrase');
        expect(subject.tmpFail('blabla').status.phrase).toEqual('blabla');
    });

    it('записывает 2 в core.res.mmapiStatus', () => {
        subject = status(core);
        subject.tmpFail();
        expect(core.res.mmapiStatus).toEqual(2);
    });
});

describe('#permFail', () => {
    it('отдает status: 3', () => {
        subject = status();
        expect(subject.permFail()).toEqual({
            status: {
                status: 3,
                phrase: 'PERM_FAIL'
            }
        });
    });

    it('добавляет phrase', () => {
        subject = status();
        expect(subject.permFail('blabla').status).toContainKey('phrase');
        expect(subject.permFail('blabla').status.phrase).toEqual('blabla');
    });

    it('записывает 3 в core.res.mmapiStatus', () => {
        subject = status(core);
        subject.permFail();
        expect(core.res.mmapiStatus).toEqual(3);
    });
});

describe('#_withErrorCode', () => {
    it('добавляет в ответ поле "code" и преобразует его к Number', () => {
        subject = status(core);

        const response = { status: { status: 'some status' } };
        expect(subject._withErrorCode(response, '999')).toEqual({
            status: {
                status: 'some status',
                code: 999
            }
        });
    });

    it('может быть вызвана и без errorCode', () => {
        subject = status(core);

        const response = { status: { status: 'some status' } };
        expect(subject._withErrorCode(response)).toEqual({
            status: {
                status: 'some status',
                code: undefined
            }
        });
    });
});
