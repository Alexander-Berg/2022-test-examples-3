/**
 * @jest-environment jsdom
 */
import isServerSide from '../isServerSide';

describe('isServerSide', () => {
    it('client', () => {
        expect(isServerSide()).toBe(false);
    });
});
