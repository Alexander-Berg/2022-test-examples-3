'use strict';

const render = require('./render.js');

describe('render', () => {
    it('calls res.json', () => {
        const res = {
            json: jest.fn(),
            locals: { methods: 1 }
        };
        render(null, res);
        expect(res.json).toHaveBeenCalledWith(res.locals);
    });
});
