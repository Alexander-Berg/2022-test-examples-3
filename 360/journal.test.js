'use strict';

const journalFilter = require('./journal.js');

const core = {
    config: {
        USER_IP: 'FAKE_IP'
    },
    time: {
        addUserTZ: jest.fn()
    },
    yasm: {
        sum: jest.fn()
    },
    console: {
        error: jest.fn()
    }
};

describe('journal filter', () => {
    describe('processEntry декодирует email', () => {
        it('обыкновенный', () => {
            const emailFrom = 'example@ya.ru';

            const result = journalFilter.processEntry(core, { emailFrom });

            expect(result.emailFrom).toEqual(emailFrom);
        });

        it('idn', () => {
            const emailFrom = 'example@xn--80a1acny.xn--p1ai';

            const result = journalFilter.processEntry(core, { emailFrom });

            expect(result.emailFrom).toEqual('example@почта.рф');
        });

        it('невалидный idn', () => {
            const emailFrom = 'example@xn--80a1acny.xn--p1ai123';

            const result = journalFilter.processEntry(core, { emailFrom });

            expect(result.emailFrom).toEqual('');
            expect(core.yasm.sum).toBeCalledWith('call_exception');
            expect(core.console.error).toBeCalledWith('CALL_EXCEPTION', {
                method: 'idnaDecode - filter/journal',
                domain: emailFrom
            });
        });
    });

    it('coverage', () => {
        const res1 = journalFilter(core, {
            items: []
        });
        const res2 = journalFilter(core, {
            items: [
                {
                    date: 1,
                    affected: '0'
                },
                {
                    date: 2
                }
            ]
        });
        expect({ res1, res2 }).toMatchSnapshot();
    });
});
