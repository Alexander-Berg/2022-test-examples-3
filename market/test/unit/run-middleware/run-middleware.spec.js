'use strict';

const middleware = require('./middleware');
const runMiddleware = require('./../../functional/lib/run-middleware');

describe('run middleware', () => {
    it('should run all middlewaries and save side-effects', async () => {
        const req = {};
        const res = {};

        await runMiddleware(middleware, req, res);

        expect(req.data).toEqual(['1-1', '1-2-1', '1-2-2']);
        expect(res.data).toEqual(['1-1', '1-2-1', '1-2-2', '2']);
    });
});
