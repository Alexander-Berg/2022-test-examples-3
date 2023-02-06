const testGenerator = require('./runTestGenerator');

const generator = {
    options: {
        flagName: 'aaa_test_generator_flag',
        name: 'AdapterSpecialEvent',
        platforms: ['desktop', 'touch-phone'],
        adapterTypes: [
            '{"platform":"desktop","type":"special-event"}',
            '{"platform":"desktop","type":"special/event","subtype":"football"}',
            '{"platform":"touch-phone","type":"special-event"}',
            '{"platform":"touch-phone","type":"special/event","subtype":"football"}'
        ],
        vteam: 'Architecture',
        fullChangeFeature: false,
        isComponentCommon: false,
        componentType: 'class'
    },
    targetPath: './src/experiments/',
    command: 'create_experiment_feature'
};

testGenerator(generator);
