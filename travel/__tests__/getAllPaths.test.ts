import {getAllPaths} from '../getAllPaths';

describe('getAllPaths', () => {
    test.each`
        object                                            | expected
        ${{a: 1}}                                         | ${['a']}
        ${{a: 1, b: 2}}                                   | ${['a', 'b']}
        ${{a: 0, b: '', c: false, d: null, e: undefined}} | ${['a', 'b', 'c', 'd', 'e']}
        ${{}}                                             | ${[]}
        ${{a: {b: 2}}}                                    | ${['a.b']}
        ${{a: {b: 2}, c: {e: 3}}}                         | ${['a.b', 'c.e']}
        ${{a: {b: [{e: 3}]}}}                             | ${['a.b.0.e']}
        ${{a: {b: [{e: 3}, {d: 4}]}}}                     | ${['a.b.0.e', 'a.b.1.d']}
    `('getAllPaths($object) must be $expected', ({object, expected}) => {
        expect(getAllPaths(object)).toEqual(expected);
    });
});
