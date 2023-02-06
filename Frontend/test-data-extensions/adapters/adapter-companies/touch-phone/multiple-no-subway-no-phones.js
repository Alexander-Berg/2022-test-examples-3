var dataStub = require('./multiple-data-stub')();

dataStub.snippets.full.data.GeoMetaSearchData.features.forEach(function (feature) {
    feature.properties.CompanyMetaData.Phones = null;
    feature.properties.Stops = null;
});

module.exports = {
    type: 'snippet',
    extensions: {
        time: '2016-02-11T21:00:00+0300'
    },
    data_stub: dataStub
};
