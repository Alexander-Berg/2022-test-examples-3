import diffState, {prev, current} from './diffState';

describe('diffState', () => {
    it('Разные типы', () => {
        expect(diffState(1, '1')).toEqual({[prev]: 1, [current]: '1'});
    });

    it('Одинаковые объекты', () => {
        expect(diffState({a: 1, b: 2}, {a: 1, b: 2})).toBeUndefined();
    });

    it('Разные объекты', () => {
        expect(diffState({a: 1, b: 2}, {a: 1, b: 3})).toEqual({
            b: {[prev]: 2, [current]: 3},
        });
        expect(
            diffState(
                {
                    a: 1,
                    b: {
                        c: 2,
                    },
                },
                {
                    a: 1,
                    b: {
                        c: 3,
                    },
                },
            ),
        ).toEqual({
            b: {
                c: {[prev]: 2, [current]: 3},
            },
        });
    });
});
