const testGenerator = require('./runTestGenerator');

const generator = {
    options: {
        flagName: 'aaa_test_css_exp_flag',
        platforms: ['desktop'],
        vteam: 'Architecture'
    },
    targetPath: './src/experiments/',
    command: 'create_experiment_css'
};

testGenerator(generator);
