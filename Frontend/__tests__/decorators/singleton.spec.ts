import * as DOMUtils from '@yandex-turbo/core/canUseDOM';
import { singleton } from '../../decorators/singleton';

jest.mock('@yandex-turbo/core/canUseDOM');

const BaseCls = class {};

// @ts-ignore
const state: { BaseCls?: Class} = global.__GLOBAL_STATE__ = {};

describe('singleton', () => {
    it('Вернет функцию с одним аргументом', () => {
        const createSingleton = singleton();

        expect(createSingleton.length).toBe(1);
    });

    it('Которая возвращает новый класс, отнаследованный от переданного', () => {
        const createSingleton = singleton();
        const SingletonCls = createSingleton(BaseCls);

        expect(SingletonCls.prototype).toBeInstanceOf(BaseCls);
    });

    it('Который инстаниируется только один раз', () => {
        const createSingleton = singleton();
        const SingletonCls = createSingleton(BaseCls);
        const inst1 = new SingletonCls();
        const inst2 = new SingletonCls();

        expect(inst1).toBe(inst2);
    });

    describe('Уммеет хранить инстансы в глобальном пространсве', () => {
        afterEach(() => {
            delete state.BaseCls;
        });

        it('Когда DOM доступен', () => {
            // @ts-ignore
            DOMUtils.canUseDOM = true;

            const createSingleton = singleton('BaseCls');
            const SingletonCls = createSingleton(BaseCls);

            const inst1 = new SingletonCls();
            const inst2 = new SingletonCls();

            expect(state.BaseCls).toBe(inst1);
            expect(inst1).toBe(inst2);
        });

        it('И не умеет когда DOM не доступен', () => {
            // @ts-ignore
            DOMUtils.canUseDOM = false;

            const createSingleton = singleton('BaseCls');
            const SingletonCls = createSingleton(BaseCls);

            const inst1 = new SingletonCls();
            const inst2 = new SingletonCls();

            expect(state.BaseCls).toBeUndefined();
            expect(inst1).toBe(inst2);
        });
    });
});
