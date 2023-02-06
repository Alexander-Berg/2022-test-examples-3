const [, , type] = process.argv;

const skipped = require('../skipped.js');

const countSkippedCases = collection => collection.reduce((sum, item) => sum + item.cases.length, 0);

const outputResult = type => console.log(type, countSkippedCases(skipped[type]));

if (process.argv.length < 3) {
    Object.keys(skipped).forEach(type => outputResult(type));
} else {
    const [, , type] = process.argv;

    if (skipped[type]) {
        outputResult(type);
    } else {
        console.error(`Unknown pack type: '${type}'`);
        process.exit(1);
    }
}
