const testGenerator = require('./runTestGenerator');

const generator = {
    options: {
        name: 'albert',
        size: 20,
        iconFile: 'promo-search'
    },
    targetPath: './src/components/Icon/_type/',
    command: 'create_icon'
};

testGenerator(generator);
