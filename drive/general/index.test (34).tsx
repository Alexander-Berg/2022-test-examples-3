/*eslint-disable*/
import { combineRequests } from './index';

describe('Requests', () => {

    let request1 = {
        ['a']: {a1: 1, a2: 2},
        ['b']: {b1: 3, b2: 4}
    };

    let request2 = {
        ['c']: {c1: 1, c2: 2},
    };

    let expected = {
        ['a']: {a1: 1, a2: 2},
        ['b']: {b1: 3, b2: 4},
        ['c']: {c1: 1, c2: 2},
    };

    test('combine requests', () => {
        let temp = combineRequests(request1, request2);

        expect(JSON.stringify(temp)).toBe(JSON.stringify(expected));

        expect(Object.keys(request2).length).toBe(1);
        expect(Object.keys(request1).length).toBe(2);
        expect(Object.keys(temp).length).toBe(3);
    });
});
