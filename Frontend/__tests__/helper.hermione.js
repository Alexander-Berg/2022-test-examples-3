const PO = require('../../../../hermione/page-objects');

function checkEmbedLoaded(embedName, browser) {
    const timeout = 60 * 1000;

    return browser
        .yaWaitForVisible(
            PO.embed(),
            timeout,
            `Эмбеда ${embedName} нет на странице`
        )
        .yaWaitForVisible(
            PO.embedPreloaded(),
            timeout,
            `Эмбед ${embedName} не подгрузился`
        )
        .yaWaitForVisible(
            PO.embedLoaded(),
            timeout,
            `Эмбед ${embedName} не загрузился`
        );
}

function checkEmbedNonlazy(embedName, browser) {
    const timeout = 60 * 1000;

    return browser
        .yaWaitForVisible(
            PO.embed(),
            timeout,
            `Эмбеда ${embedName} нет на странице`
        )
        .yaWaitForVisible(
            PO.embedNonlazy(),
            timeout,
            `Эмбед ${embedName} не ленивый`
        );
}

function checkEmbedNoSandbox(embedName, browser) {
    const timeout = 60 * 1000;

    return browser
        .yaWaitForVisible(
            PO.embedNotNonlazyIFrameVH(),
            timeout,
            `Первый эмбед ${embedName} на странице без песочницы и не ленивый`
        )
        .yaWaitForVisible(
            PO.embedNonlazyIFrameVH(),
            timeout,
            `Второй эмбед ${embedName} на странице без песочницы и ленивый`
        );
}

function checkEmbedError(embedName, browser, timeout = 3000) {
    return browser
        .yaWaitForHidden(
            PO.embed(),
            timeout,
            `Эмбед ${embedName} не скрылся`
        );
}

function checkEmbedSize(expectedSize, browser) {
    return browser
        .execute(function(selector) {
            var embed = document.querySelector(selector);
            if (!embed) {
                return {};
            }

            return {
                width: embed.clientWidth,
                height: embed.clientHeight,
            };
        }, PO.embed())
        .then(({ value }) => {
            assert.deepEqual(value, expectedSize, 'Эмбед неправильного размера');
        });
}

module.exports = { checkEmbedLoaded, checkEmbedError, checkEmbedSize, checkEmbedNonlazy, checkEmbedNoSandbox };
