module.exports = hermione => {
    hermione.startedTests = [];

    hermione.on(hermione.events.TEST_BEGIN, test => {
        hermione.startedTests.push(test.fullTitle());
    });

    hermione.on(hermione.events.TEST_FAIL, test => {
        console.error('ERR', test.err);
    });
};
