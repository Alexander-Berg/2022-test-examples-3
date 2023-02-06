'use strict';

const { readYmlFile, updateYmlFile } = require('./file-yml');
const { updateTestFiles } = require('./file-test');
const { updateAssetFiles } = require('./file-asset');
const { updateMetricsFiles } = require('./file-metrics');

module.exports = {
    readYmlFile,
    updateYmlFile,
    updateTestFiles,
    updateAssetFiles,
    updateMetricsFiles
};
