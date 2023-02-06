import {
    shouldNotifyOverdraft, isOverdraftHard, isOverdraftLite
} from 'redux/store/selectors/space';

const getStateInstance = (usedMB, limitMB, isMobile = false) => ({
    config: { isDocsHost: false },
    environment: {
        agent: { isMobile }
    },
    space: {
        used: usedMB * 1024 * 1024,
        limit: limitMB * 1024 * 1024
    },
    user: {}
});

describe('Overdraft check', () => {
    const cases = [
        { args: [11000, 8800], result: true, test: 'Это овердрафтный пользователь' },
        { args: [9999, 10000], result: false, test: 'Это не овердрафтный пользователь' },
        { args: [5050, 5000], result: false, test: 'На 50мб больше лимита, но еще не овердрафт' },
        { args: [11025, 10000], result: true, test: 'На 1025мб больше лимита, овердрафт' },
        { args: [11025, 10000, true], result: false, test: 'Овердрафтный пользователь, но с мобилы' }
    ];

    cases.forEach((c) => {
        it(c.test, () => {
            const overdrafted = getStateInstance(...c.args);
            expect(shouldNotifyOverdraft(overdrafted)).toBe(c.result);
        });
    });
});

describe('Overdraft new', () => {
    it('должен правильно определить неовердрафтника', () => {
        expect(isOverdraftLite({
            user: {}
        })).toBe(false);
    });

    it('должен правильно определить lite-овердрафтника', () => {
        expect(isOverdraftLite({
            user: { overdraft_status: 1 }
        })).toBe(true);
    });

    it('должен правильно определить hard-овердрафтника', () => {
        expect(isOverdraftHard({
            user: { overdraft_status: 2 }
        })).toBe(true);
    });

    it('должен правильно определить заблокированного, как hard-овердрафтника', () => {
        expect(isOverdraftHard({
            user: { overdraft_status: 3 }
        })).toBe(true);
    });

    it('должен показать экран овердрафтника для hard-овердрафтника', () => {
        expect(shouldNotifyOverdraft({
            user: { overdraft_status: 2 },
            environment: { agent: {} },
            config: {},
            space: {}
        })).toBe(true);
    });

    it('должен показать экран овердрафтника для lite-овердрафтника', () => {
        expect(shouldNotifyOverdraft({
            user: { overdraft_status: 1 },
            environment: { agent: {} },
            config: {},
            space: {}
        })).toBe(true);
    });
});
