var dataStub = require('./1org-data-stub')();

delete dataStub.snippets.full.data.GeoMetaSearchData.features[0].properties.CompanyMetaData.unreliable;
delete dataStub.snippets.full.data.GeoMetaSearchData.features[0].properties.CompanyMetaData.closed;

module.exports = {
    type: 'snippet',
    data_stub: dataStub
};
