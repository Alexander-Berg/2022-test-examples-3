var ViewerTestingConfig = require('../../configs/testing/node'),
    qloudCommonConfigurator = require('../common');

var QloudTestingConfig = qloudCommonConfigurator(ViewerTestingConfig);

QloudTestingConfig.staticHost.catalogResults = '//site.test.common.yandex.ru/search/site/catalog/';

module.exports = QloudTestingConfig;
