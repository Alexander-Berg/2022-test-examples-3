'use strict';

jest.mock('@yandex-int/duffman');
jest.mock('../../middlewares/index.js', () => 1);
jest.mock('./requestMethods.js', () => 2);
jest.mock('./render.js', () => 3);

const duffman = require('@yandex-int/duffman');

describe('methodsRouter', () => {
    it('calls `post` and `use`', () => {
        const mockRouter = { post: jest.fn(), use: jest.fn() };
        duffman.express.Router.mockReturnValue(mockRouter);

        require('./index.js');

        expect(mockRouter.use).toHaveBeenCalledWith(1);
        expect(mockRouter.post).toHaveBeenCalledWith('/', [ 2, 3 ]);
    });
});
