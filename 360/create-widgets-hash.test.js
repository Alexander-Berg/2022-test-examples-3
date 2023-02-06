'use strict';

const createWidgetsHash = require('./create-widgets-hash.js');

describe('should return empty object', function() {
    it('if no widgets passed', function() {
        expect(createWidgetsHash()).toEqual({});
    });

    it('if widgets.widgets is not array', function() {
        expect(createWidgetsHash({}, 1)).toEqual({});
    });
});

describe('should return widgets hash with correct format', function() {
    it('if widgets exists', function() {
        const widgets = {
            widgets: [
                {
                    info: {
                        mid: '1',
                        showType: 'showTest1'
                    }
                },

                {
                    info: {
                        mid: '1',
                        showType: 'showTest2'
                    }
                },

                {
                    info: {
                        mid: '2',
                        showType: 'showTest1'
                    }
                }
            ]
        };

        expect(createWidgetsHash(widgets, 1)).toEqual({
            1: [ widgets.widgets[0], widgets.widgets[1] ],
            2: [ widgets.widgets[2] ]
        });
    });

    it('if passed more widgets for message with same showType than in param `limitByShowType`', function() {
        const widgets = {
            widgets: [
                {
                    info: {
                        mid: '1',
                        showType: 'showTest1',
                        type: 'typeTest11'
                    }
                },

                {
                    info: {
                        mid: '1',
                        showType: 'showTest1',
                        type: 'typeTest12'
                    }
                },

                {
                    info: {
                        mid: '1',
                        showType: 'showTest2',
                        type: 'typeTest21'
                    }
                },

                {
                    info: {
                        mid: '2',
                        showType: 'showTest1',
                        type: 'typeTest11'
                    }
                },

                {
                    info: {
                        mid: '2',
                        showType: 'showTest2',
                        type: 'typeTest21'
                    }
                },

                {
                    info: {
                        mid: '2',
                        showType: 'showTest2',
                        type: 'typeTest22'
                    }
                }
            ]
        };

        expect(createWidgetsHash(widgets, 1)).toEqual({
            1: [ widgets.widgets[0], widgets.widgets[2] ],
            2: [ widgets.widgets[3], widgets.widgets[4] ]
        });
    });
});
