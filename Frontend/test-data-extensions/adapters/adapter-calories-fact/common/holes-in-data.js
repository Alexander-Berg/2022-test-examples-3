var dataStub = require('../data')();

dataStub.construct.data.size = [
    {
        text_i18n: 'calories_100g'
    },
    {
        value: 17
    },
    {
        value: 5,
        text_i18n: 'teaspoon'
    }
];

dataStub.construct.data.attributes = [
    {
        value: 0.52
    },
    {

        text_i18n: 'fat'
    },
    {
        value: 9.8,
        text_i18n: 'carbohydrate'
    }
];

module.exports = {
    type: 'snippet',
    data_stub: dataStub
};
