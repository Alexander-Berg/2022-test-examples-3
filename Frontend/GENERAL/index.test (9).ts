import { sum } from './index';

describe('test', () => {
    it('sum', () => {
        expect(sum(2, 2)).toStrictEqual(4);
    });
});
