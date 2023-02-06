module.exports = hermione => {
    hermione.on(hermione.events.TEST_FAIL, () => {
        // Uncomment for debug
        // console.error('ERR', test.err);
    });

    hermione.on(hermione.events.AFTER_TESTS_READ, testCollection => {
        if (hermione.isWorker()) {
            return;
        }

        testCollection.eachTest(test => {
            const title = test.fullTitle();

            if (title.includes('disable me')) {
                testCollection.disableTest(title);
            }
        });
    });
};
