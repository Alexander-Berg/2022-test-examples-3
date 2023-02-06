'use strict';

const labelsMock = require('../../../test/mock/labels.json');
const labelsFilter = require('./labels.js');

describe('filters meta/labels', () => {
    it('работает', () => {
        expect(labelsFilter(labelsMock)).toMatchSnapshot();
    });
});
