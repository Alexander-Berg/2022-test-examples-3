import { getPathInTrendbox } from './trendbox';

describe('getPathInTrendbox', () => {
    beforeAll(() => {
        process.env.YENV = 'testing';
    });
    afterAll(() => {
        delete process.env.checkout_config;
        delete process.env.YENV;
    });

    it('should build path by commits', () => {
        process.env.TRENDBOX_PULL_REQUEST_NUMBER = '123456';
        process.env.checkout_config = JSON.stringify({ base: [{ commit: 'base' }], head: { commit: 'head' } });

        expect(getPathInTrendbox()).toEqual('123456/base/head');
    });
});
