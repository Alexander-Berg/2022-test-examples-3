import { createSelector } from '../createSelector';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function getState(): any {
    return {
        collections: {
            phones: {
                one: { title: 'Nokia', shops: ['2', '3'] },
                two: { title: 'Samsung', shops: ['2'] },

            },
            shops: {
                '2': { name: 'Spectr' },
                '3': { name: 'Eldorado' },
            },
        },
    };
}

type State = ReturnType<typeof getState>;
type Phones = State['collections']['phones']
type Shops = State['collections']['shops'];

describe('createSelector', () => {
    let state: State;
    const selectPhones = (state: State) => state.collections.phones;
    const selectPhonesByIds = (state: State, { phoneIds }: {phoneIds: string[]}) => (
        phoneIds.map(id => state.collections.phones[id])
    );
    const selectShopsByIds = (state: State, { shopIds }: {shopIds: string[]}) => (
        shopIds.map(id => state.collections.shops[id])
    );

    beforeEach(() => {
        state = getState();
    });

    it('корретно мемоизирутся результат, повторные вызовы селектора не приводят к инвалидации кэша', () => {
        const combinator = jest.fn((phones: Phones) => {
            return phones.one;
        });
        const selector = createSelector<State, Phones, Phones['one']>(selectPhones, combinator);

        expect(selector(state)).toEqual(state.collections.phones.one);
        expect(combinator).toHaveBeenCalledTimes(1);

        // проверяем что при повторном вызове результат не вычилсяется в комбинаторе снова
        expect(selector(state)).toEqual(state.collections.phones.one);
        expect(combinator).toHaveBeenCalledTimes(1);
    });

    it('кэш результатта должен инвалидироваться если один из входных селекторов возвращает новый результат', () => {
        const combinator = jest.fn((phones: Phones) => {
            return phones.one;
        });
        const selector = createSelector<State, Phones, Phones['one']>(selectPhones, combinator);

        selector(state);
        state.collections.phones = {
            ...state.collections.phones,
            three: { title: 'Iphone', shops: [] },
        };
        selector(state);
        expect(combinator).toHaveBeenCalledTimes(2);
    });

    it('если передаются новые входные параметры и результат одного из входных селекторов отличается от предыдущего вызова, то кэш должен инвалидироваться', () => {
        type Props = {
            phoneIds: string[],
            shopIds: string[],
        }
        const combinator = jest.fn((phones: Phones[], shops: Shops[]) => {
            return {
                phones,
                shops,
            };
        });

        const selector = createSelector<State, Props, {}, {}, {}>([selectPhonesByIds, selectShopsByIds], combinator);

        selector(state, { shopIds: ['2'], phoneIds: ['one', 'two'] });
        selector(state, { shopIds: ['2'], phoneIds: ['one', 'two'] });
        selector(state, { shopIds: ['2'], phoneIds: ['one', 'two'] });

        // Вхохдные селекторы возвращают один и тот же результат
        expect(combinator).toHaveBeenCalledTimes(1);

        // Изменились входные параметры и возвращаемое значение одного из входных селекторов.
        // Поэтому произошла инвалидация кэша и комбинатор был вызван.
        selector(state, { shopIds: ['2'], phoneIds: ['one'] });

        expect(combinator).toHaveBeenCalledTimes(2);
    });
});
