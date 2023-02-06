'use strict';

const prepareBunkerParams = require('./prepare-bunker-params.js');

const core = {
    params: {}
};

test('without params', () => {
    core.params = {};
    expect(() => {
        prepareBunkerParams(core);
    }).toThrow('invalid client param');
});

[ undefined, 'mobilemail', 'mobilepayment' ].forEach((handler) => {
    [ 'iphone', 'ipad', 'aphone', 'apad' ].forEach((client) => {
        test(`handler: ${handler}, client: ${client}`, () => {
            core.params = { handler, client };
            expect(prepareBunkerParams(core)).toMatchSnapshot();
        });
    });
});
