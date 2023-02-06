const testGenerator = require('./runTestGenerator');

const generator = {
    options: {
        name: 'albert',
        platforms: ['desktop', 'touch-phone'],
        isAdapterCommon: true,
        isComponentCommon: true,
        adapterType: 'abobaType',
        adapterSubtype: 'abobaSubtype',
        componentType: 'class',
        vteam: 'Architecture'
    },
    targetPath: './src/features/',
    command: 'create_feature'
};

testGenerator(generator);
