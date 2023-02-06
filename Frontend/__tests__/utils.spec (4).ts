import { isGLFilter } from '../utils';

describe('Функция:', () => {
    describe('isGLFilter', () => {
        it('возвращает true если идентификатор фильтра целочисленный', () => {
            // @ts-ignore
            expect(isGLFilter({ id: '3232' })).toEqual(true);
        });

        it('возвращает false если идентификатор фильтра не целочисленный', () => {
            // @ts-ignore
            expect(isGLFilter({ id: 'test' })).toEqual(false);
        });
    });
});
