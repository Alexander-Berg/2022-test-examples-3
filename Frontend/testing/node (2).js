var ViewerTestingConfig = require('../../configs/testing/node'),
    qloudCommonConfigurator = require('../common'),
    QloudTestingConfig = ViewerTestingConfig.create();

QloudTestingConfig = qloudCommonConfigurator(QloudTestingConfig);

module.exports = QloudTestingConfig;
