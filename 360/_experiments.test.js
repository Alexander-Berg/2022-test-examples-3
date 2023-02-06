'use strict';

const experiments = require('./experiments.js');

test('experiments filter works', () => {
    const core = {
        config: { secrets: require('../../../__mocks__/secrets.js') },
        experiments: {
            getExpBoxes: jest.fn().mockReturnValue([ '1,2,3', '4,5,6', '7,8,9' ]),
            getEnabledExpBoxes: jest.fn().mockReturnValue([ '1,2,3', '4,5,6' ]),
            getEnabledExpIds: jest.fn().mockReturnValue([ '1', '2', '3' ]),
            getEnabledExpFeatures: jest.fn().mockReturnValue({ feature1: {}, feature2: {} })
        }
    };

    const res = experiments({ ExpBoxes: '1,2,3' }, core);

    expect(res).toMatchSnapshot();
});
