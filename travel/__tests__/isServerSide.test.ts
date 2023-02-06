import isServerSide from '../isServerSide';

describe('isServerSide', () => {
    it('server', () => {
        expect(isServerSide()).toBe(true);
    });
});
