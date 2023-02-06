const testGenerator = require('./runTestGenerator');

const generator = {
    options: {
        flagName: 'aaa_test_generator_flag',
        name: 'AbobaExp',
        registryId: 'afishaItemCn',
        platforms: ['desktop', 'touch-phone'],
        adapterTypes: [],
        vteam: 'Architecture'
    },
    targetPath: './src/experiments/',
    command: 'create_experiment_component'
};

testGenerator(generator);
