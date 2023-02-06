jest.disableAutomock();

const mockConfig = {};

jest.setMock('@yandex-int/yandex-config', () => mockConfig);

const experiments = require.requireActual('../experiments');

import Req from '../../helpers/mocks/req';
import Res from '../../helpers/mocks/res';

let req;
let res;
const next = jest.fn();

const booleanTest = {
    type: Boolean,
    percentage: 100,
};

const stringTest = {
    type: String,
    values: [
        {
            value: 'one',
            percentage: 50,
        },
        {
            value: 'two',
            percentage: 50,
        },
    ],
    defaultValue: 'default',
};

const numberTest = {
    type: Number,
    values: [
        {
            value: 1,
            percentage: 50,
        },
        {
            value: 2,
            percentage: 50,
        },
    ],
    defaultValue: 0,
};

const experimentsWithChild = {
    __test: stringTest,
    __childTest: {
        ...numberTest,
        dependencies: {
            __test: ['two'],
        },
    },
};

describe('Middleware experiments', () => {
    beforeEach(() => {
        req = new Req();
        res = new Res();
        mockConfig.experiments = {};
        jest.clearAllMocks();
    });

    it('Установка эксперимента в куки и установка значений в req.flags', () => {
        mockConfig.experiments = {
            __test: booleanTest,
        };

        return experiments(req, res, next).then(() => {
            expect(next.mock.calls).toEqual([[]]);
            expect(res.cookies).toHaveProperty('experiment__test', '1');
            expect(req.flags).toEqual({__test: true});
        });
    });

    it('Если эксперимент существует, то его значение можно перекрыть через параметры в url', () => {
        mockConfig.experiments = {
            __test: stringTest,
        };

        req.query = {
            __test: 'two',
        };

        return experiments(req, res, next).then(() => {
            expect(next.mock.calls).toEqual([[]]);
            // кука не устанавливается
            expect(res.cookies).toEqual({});
            expect(req.flags).toEqual({__test: 'two'});
        });
    });

    it(
        'Если эксперимент существует, то его значение можно перекрыть через параметры в url. ' +
            'Если значение в url не соответствует возможным из эксперимента, то устанавливается ' +
            'дефолтное значение',
        () => {
            mockConfig.experiments = {
                __test: stringTest,
            };

            req.query = {
                __test: 'someValue',
            };

            return experiments(req, res, next).then(() => {
                expect(next.mock.calls).toEqual([[]]);
                // кука не устанавливается
                expect(res.cookies).toEqual({});
                expect(req.flags).toEqual({__test: 'default'});
            });
        },
    );

    it('Если эксперимент запрещен, то выставляется дефолтное значение вне зависимости от того, что в куках', () => {
        mockConfig.experiments = {
            __test: {
                ...stringTest,
                denied: true,
            },
        };
        req.cookies = {
            experiment__test: 'one',
        };

        return experiments(req, res, next).then(() => {
            expect(next.mock.calls).toEqual([[]]);
            // кука не устанавливается
            expect(res.cookies).toEqual({});
            expect(req.flags).toEqual({__test: 'default'});
        });
    });

    it('Эксперимент с зависимостями. Пользователь не попадает в дочерний эксперимент __childTest', () => {
        mockConfig.experiments = experimentsWithChild;
        req.cookies = {
            experiment__test: 'one',
        };

        return experiments(req, res, next).then(() => {
            expect(next.mock.calls).toEqual([[]]);
            expect(res.cookies).toEqual({});
            expect(req.flags).toEqual({__test: 'one'});
        });
    });

    it('Эксперимент с зависимостями. Пользователь попадает в дочерний эксперимент __childTest', () => {
        mockConfig.experiments = experimentsWithChild;
        req.cookies = {
            experiment__test: 'two',
        };

        return experiments(req, res, next).then(() => {
            expect(next.mock.calls).toEqual([[]]);
            expect(['1', '2'].includes(res.cookies.experiment__childTest)).toBe(
                true,
            );
            expect(req.flags.__test).toBe('two');
            expect([1, 2].includes(req.flags.__childTest)).toBe(true);
        });
    });

    it('Эксепримент в виде функции', () => {
        req.query = {
            testForFunc: '1',
        };
        mockConfig.experiments = {
            __func(reqObject) {
                expect(reqObject.query.testForFunc).toBe('1');

                return new Promise(resolve => {
                    setTimeout(() => resolve(stringTest), 10);
                });
            },
        };

        return experiments(req, res, next).then(() => {
            expect(next.mock.calls).toEqual([[]]);
            expect(['one', 'two'].includes(res.cookies.experiment__func)).toBe(
                true,
            );
            expect(['one', 'two'].includes(req.flags.__func)).toBe(true);
        });
    });

    it('Динамический эксперимент. Высчитывается и записывается пользователю в куку при каждом запросе', () => {
        mockConfig.experiments = {
            __test: {
                ...stringTest,
                dynamic: true,
            },
        };
        req.cookies = {
            experiment__test: 'tree',
        };

        return experiments(req, res, next).then(() => {
            expect(next.mock.calls).toEqual([[]]);
            expect(['one', 'two'].includes(res.cookies.experiment__test)).toBe(
                true,
            );
            expect(['one', 'two'].includes(req.flags.__test)).toBe(true);
        });
    });
});
