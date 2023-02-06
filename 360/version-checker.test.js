'use strict';

const versionChecker = require('./version-checker.js');

let core;
let subject;

beforeEach(() => {
    core = {
        req: {}
    };
    subject = versionChecker(core);
});

describe('#isAndroid', () => {
    it('is true for android', () => {
        core.req.headers = {
            'user-agent': 'ru.yandex.mail/3.26.46428 (samsung SM-G900F; Android 6.0.1)'
        };

        expect(subject.isAndroid()).toBe(true);
    });

    it('is not true for ios', () => {
        core.req.headers = {
            'user-agent': 'ru.yandex.mail/356.435 (iPhone9,3; iOS 11.2.6)'
        };

        expect(subject.isAndroid()).toBe(false);
    });
});

describe('#isIos', () => {
    it('is not true for android', () => {
        core.req.headers = {
            'user-agent': 'ru.yandex.mail/3.26.46428 (samsung SM-G900F; Android 6.0.1)'
        };

        expect(subject.isIos()).toBe(false);
    });

    it('is true for ios', () => {
        core.req.headers = {
            'user-agent': 'ru.yandex.mail/356.435 (iPhone9,3; iOS 11.2.6)'
        };

        expect(subject.isIos()).toBe(true);
    });
});

describe('#isVersionAndUp', () => {
    beforeEach(() => {
        core.req.headers = {
            'user-agent': 'ru.yandex.mail/3.26.46428 (samsung SM-G900F; Android 6.0.1)'
        };
    });

    it('=', () => {
        expect(subject.isVersionAndUp('3.26.1234')).toBe(true);
    });

    it('>', () => {
        expect(subject.isVersionAndUp('3.27')).toBe(false);
    });

    it('<', () => {
        expect(subject.isVersionAndUp('3.25')).toBe(true);
    });

    it('если не распарсили - false', () => {
        core.req.headers = {
            'user-agent': 'OMG'
        };
        expect(subject.isVersionAndUp('3.25')).toBe(false);
    });

    it('>>', () => {
        core.req.headers = {
            'user-agent': 'ru.yandex.mail/111.2.3 (samsung SM-G900F; Android 6.0.1)'
        };
        expect(subject.isVersionAndUp('3.25')).toBe(true);
    });

    describe('кушает всякие UA', () => {
        it('ios beta', () => {
            core.req.headers = {
                'user-agent': 'ru.yandex.mail.beta/356.435 (iPhone9,3; iOS 11.2.6)'
            };
            expect(subject.isVersionAndUp('356')).toBe(true);
        });

        it('android beta', () => {
            core.req.headers = {
                'user-agent': 'ru.yandex.mail.beta/3.26.46428 (samsung SM-G900F; Android 6.0.1)'
            };
            expect(subject.isVersionAndUp('3')).toBe(true);
        });

        it('inhouse', () => {
            core.req.headers = {
                'user-agent': 'ru.yandex.mail.inhouse/356.435 (iPhone9,3; iOS 11.2.6)'
            };
            expect(subject.isVersionAndUp('356')).toBe(true);
        });

        it('debug', () => {
            core.req.headers = {
                'user-agent': 'ru.yandex.mail.debug/356.435 (iPhone9,3; iOS 11.2.6)'
            };
            expect(subject.isVersionAndUp('356')).toBe(true);
        });

        it('whatever', () => {
            core.req.headers = {
                'user-agent': 'ru.yandex.mail.trololo/356.435 (iPhone9,3; iOS 11.2.6)'
            };
            expect(subject.isVersionAndUp('356')).toBe(true);
        });

        it('даблтрабл', () => {
            core.req.headers = {
                'user-agent': 'ru.yandex.mail.inhouse.shareextension/356.435 (iPhone9,3; iOS 11.2.6)'
            };
            expect(subject.isVersionAndUp('356')).toBe(true);
        });
    });
});
