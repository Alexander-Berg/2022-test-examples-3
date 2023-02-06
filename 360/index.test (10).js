'use strict';

const { LIST_TYPE, BODY_TYPE } = require('./types.js');
const getWidgetShowType = require('./index.js');

describe('should return right type', function() {
    it('if passed widget with some type', function() {
        expect(getWidgetShowType({ info: { type: 'some' } })).toBe(LIST_TYPE);
    });

    it('if passed widget with `url_info` type', function() {
        expect(getWidgetShowType({ info: { type: 'url_info' } })).toBe(BODY_TYPE);
    });
});
