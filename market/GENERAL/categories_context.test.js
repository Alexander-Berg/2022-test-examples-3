
import {Categories} from '../Categories';

const category = new Categories();

/**
 * Проверяем что корректно сохраняет в store значения контекста
 * так же не путает их при новых вызовах
 *
 * порядок тестов не нужно менять,
 * основываю их на том что jest последовательно выполняет
 */
describe('Тестим ф-ии запоминания/запроса контекста', () => {
    let contextBase = null;
    let contextExp1 = null;
    let contextExp1_change_2_param = null;
    let contextExp2 = null;
    const context_exp1_value = 'get_context1';
    const context_exp2_value = 'get_context2';

    /**
     * Базовый запуск - без указания параметров
     */
    test('1 базовый запуск', async () => {
        const resolveContextAndRange = await category.resolveContextAndRange({useContextMock: true})
        contextBase = resolveContextAndRange.context;

        expect(resolveContextAndRange).toEqual({
            context: contextBase,
            range: 1
        });
    });
    test('2 базовый запуск', async () => {
        const resolveContextAndRange = await category.resolveContextAndRange({useContextMock: true})
        expect(resolveContextAndRange).toEqual({
            context: contextBase,
            range: 2,
        });
    });
    test('3 базовый запуск', async () => {
        const resolveContextAndRange = await category.resolveContextAndRange({useContextMock: true})
        expect(resolveContextAndRange).toEqual({
            context: contextBase,
            range: 3,
        });
    });


    test('1 запуск c context_exp1', async () => {
        const resolveContextAndRange = await category.resolveContextAndRange({
            useContextMock: true,
            context_experiment: context_exp1_value,
            dj_viewer_toloka_experiment: 'test1',
        })
        contextExp1 = resolveContextAndRange.context;

        expect(resolveContextAndRange).toEqual({
            context: contextExp1,
            range: 1,
        });
    });
    test('2 запуск c context_exp1', async () => {
        const resolveContextAndRange = await category.resolveContextAndRange({
            useContextMock: true,
            context_experiment: context_exp1_value,
            dj_viewer_toloka_experiment: 'test1',
        })

        expect(resolveContextAndRange).toEqual({
            context: contextExp1,
            range: 2,
        });
    });

    const dj_viewer_exp = 'test1_awd'
    test('3 запуск c context_exp1, меняем 2й паарметр', async () => {
        const resolveContextAndRange = await category.resolveContextAndRange({
            useContextMock: true,
            context_experiment: context_exp1_value,
            dj_viewer_toloka_experiment: dj_viewer_exp,
        })
        contextExp1_change_2_param = resolveContextAndRange.context;

        expect(resolveContextAndRange).toEqual({
            context: contextExp1_change_2_param,
            range: 1,
        });
    });
    test('4 запуск c context_exp1, меняем 2й паарметр', async () => {
        const resolveContextAndRange = await category.resolveContextAndRange({
            useContextMock: true,
            context_experiment: context_exp1_value,
            dj_viewer_toloka_experiment: dj_viewer_exp,
        })

        expect(resolveContextAndRange).toEqual({
            context: contextExp1_change_2_param,
            range: 2,
        });
    });

    test('1 запуск c context_exp2', async () => {
        const resolveContextAndRange = await category.resolveContextAndRange({
            useContextMock: true,
            context_experiment: context_exp2_value,
            dj_viewer_toloka_experiment: 'test1',
        })
        contextExp2 = resolveContextAndRange.context;

        expect(resolveContextAndRange).toEqual({
            context: contextExp2,
            range: 1,
        });
    });
    test('2 запуск c context_exp2', async () => {
        const resolveContextAndRange = await category.resolveContextAndRange({
            useContextMock: true,
            context_experiment: context_exp2_value,
            dj_viewer_toloka_experiment: 'test1',
        })

        expect(resolveContextAndRange).toEqual({
            context: contextExp2,
            range: 2,
        });
    });

    test('4 базовый запуск', async () => {
        const resolveContextAndRange = await category.resolveContextAndRange({useContextMock: true})
        expect(resolveContextAndRange).toEqual({
            context: contextBase,
            range: 4,
        });
    });
    test('5 базовый запуск', async () => {
        const resolveContextAndRange = await category.resolveContextAndRange({useContextMock: true})
        expect(resolveContextAndRange).toEqual({
            context: contextBase,
            range: 5,
        });
    });

    test('все контексты долны быть разные (если рандом не выдаст одинакого)', async () => {
        const arrOfContext = [
            contextBase,
            contextExp1,
            contextExp1_change_2_param,
            contextExp2,
        ].sort((a, b) => a - b);

        const result = arrOfContext.reduce((prev, current) => {
            if (prev === false) return false;
            if (prev === current) return false;
            return current;
        }, true);

        expect(Boolean(result)).toBe(true);
    });

});
