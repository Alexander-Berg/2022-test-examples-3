'use strict';

jest.mock('@yandex-int/duffman');
jest.mock('../../middlewares/index.js', () => [ 1, 2, 3, 4, 5 ]);
jest.mock('./uploader.js', () => 13);
jest.mock('./route.js', () => -1);

describe('routes/send/index', () => {
    it('Injects uploader middleware and calls router', () => {
        const route = { post: jest.fn(() => route), all: jest.fn() };
        const mockRouter = { use: jest.fn(), route: jest.fn(() => route) };

        jest.isolateModules(() => {
            const duffman = require('@yandex-int/duffman');
            duffman.express.Router.mockReturnValue(mockRouter);
            duffman.middleware = { yandexuid: 3 };
            require('./index.js');
        });

        // Bump coverage
        const res = { sendStatus: jest.fn() };
        route.all.mock.calls[0][0](1, res);
        mockRouter.use.mock.calls[0][0](1, res);

        expect(mockRouter.route).toHaveBeenCalledWith('/');
        expect(route.post).toHaveBeenCalledWith([ 1, 2, 3, 13, 4, 5 ], -1);
    });

    it('Throws if cannot inject uploader middleware', () => {
        jest.isolateModules(() => {
            const duffman = require('@yandex-int/duffman');
            duffman.middleware = { yandexuid: 100500 };
            expect(() => require('./index.js')).toThrow(/Cannot find yandexuid/);
        });
    });
});
