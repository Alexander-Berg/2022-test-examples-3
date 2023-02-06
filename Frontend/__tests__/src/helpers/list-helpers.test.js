import { shuffleNotifications } from '../../../src/helpers/list-helpers';

describe('shuffleNotifications tests', () => {
    test('empty array', () => {
        const array = [];
        expect(shuffleNotifications(array)).toStrictEqual([]);
    });

    test('Array length < 7', () => {
        const array = [1, 2, 3];
        expect(shuffleNotifications(array).length).toBe(3);
    });

    test('Array length === 7', () => {
        const array = [1, 2, 3, 4, 5, 6, 7];
        expect(shuffleNotifications(array).length).toBe(7);
    });

    test('Array length > 7', () => {
        const array = [1, 2, 3, 4, 5, 6, 7, 8, 9, 0];
        const shuffledArray = shuffleNotifications(array);
        expect(shuffledArray.length).toBe(10);
        expect(shuffledArray.splice(7, 3)).toStrictEqual([8, 9, 0]);
    });
});
