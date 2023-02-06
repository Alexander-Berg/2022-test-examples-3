const geobase = require('geobase');
// eslint-disable-next-line import/no-unresolved
const geobaseMockDriver = require('geobase-mock-driver');

// Fake factory
function geobaseLookupFactory(driver, connectionString, driverParams) {
    // eslint-disable-next-line global-require,new-cap
    return new geobaseMockDriver(require('./geobase-response.json'));
}

module.exports = geobaseLookupFactory;
