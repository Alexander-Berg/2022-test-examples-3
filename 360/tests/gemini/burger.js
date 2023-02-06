const { PUBLIC_PDF_FILE_URL } = require('../config').consts;

gemini.suite('burger', (suite) => {
    suite.setUrl(PUBLIC_PDF_FILE_URL)
        .setCaptureElements('body')
        // do not capture direct since it may change on each test
        .ignoreElements({ every: '.direct__iframe' })
        .capture('burger-opened', (actions, find) => {
            actions.click(find('.burger-sidebar__button'));
        })
        .capture('burger-closed', (actions, find) => {
            actions.click(find('.burger-sidebar__back-button'));
        });
});
