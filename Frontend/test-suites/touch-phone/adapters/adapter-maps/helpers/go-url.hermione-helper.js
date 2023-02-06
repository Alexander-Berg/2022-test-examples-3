'use strict';

const PO = require('../../../../../page-objects/touch-phone/index').PO;

module.exports = function(browser, query) {
    return browser
        .yaOpenSerp(query)
        .yaWaitForVisible(PO.toponym(), 'Не появился колдунщик топонимов');
};
