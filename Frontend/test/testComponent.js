const testGenerator = require('./runTestGenerator');

const generator = {
    options: {
        name: 'albert',
        platforms: ['desktop', 'touch-phone'],
        isComponentCommon: true,
        componentType: 'class'
    },
    targetPath: './src/components/',
    command: 'create_component'
};

testGenerator(generator);
