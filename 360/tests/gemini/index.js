const { PUBLIC_PDF_FILE_URL } = require('../config').consts;

// basic example
gemini.suite('public', (suite) => {
    suite.setUrl(PUBLIC_PDF_FILE_URL)
        .setCaptureElements('body')
        .ignoreElements({ every: '.direct__iframe' }) // do not capture direct since it may change on each test
        .capture('body');
});
