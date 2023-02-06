'use strict';

const filter = require('./widgets.js');
const getWidgetShowType = require('../../_helpers/widget-show-type');
const modifyWidgetFavicons = require('./helpers/modify-widget-favicons');

jest.mock('../../_helpers/widget-show-type');
jest.mock('./helpers/modify-widget-favicons');

describe('should return object with correct format', function() {
    it('if no widgets passed', function() {
        expect(filter()).toEqual({ widgets: [] });
    });

    it('if widgets.widgets is not array', function() {
        expect(filter({})).toEqual({ widgets: [] });
    });
});

describe('should set showType for all widgets', function() {
    it('if widgets passed', function() {
        getWidgetShowType.mockReturnValue('showTyped');

        const widgets = [
            { info: { mid: 1 } },
            { info: { mid: 2 } },
            { info: { mid: 3 } },
            { info: { mid: 4 } },
            { info: { mid: 5 } }
        ];

        expect(filter({ widgets })).toEqual({
            widgets: widgets.map((widget) => ({ info: { mid: widget.info.mid, showType: 'showTyped' } }))
        });
        expect(getWidgetShowType).toHaveBeenCalledTimes(5);
    });
});

describe('modify widget favicons', function() {
    it('should modify widget favicons if widget has type "url_info"', function() {
        const widgets = [ { info: { type: 'url_info' } } ];

        filter({ widgets });

        expect(modifyWidgetFavicons).toHaveBeenCalledTimes(1);
    });

    it('should not modify widget favicons if widget.info.type !== "url_info"', function() {
        const widgets = [ { info: { type: 'not_url_info' } } ];

        filter({ widgets });

        expect(modifyWidgetFavicons).toHaveBeenCalledTimes(0);
    });
});
