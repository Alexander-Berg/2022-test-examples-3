/* global describe: true, it: true */
'use strict';

function familyFilter(links) {
    return links.reduce(function(acc, link) {
        if (link.family) {
            link.url += link.family;
            acc.push(link);
        }

        return acc;
    }, []);
}

function checkCounterIfExists(browser, item) {
    if (item.counter_suffix) {
        return browser
            .yaCheckBaobabCounter(item.selector, {
                path: `/$page/$main/competitors/link[@type="${item.counter_suffix}"]`
            });
    }
}

function checkLinks(browser, query, links, block) {
    return browser
        .yaOpenSerp(query)
        .yaWaitForVisible(block(), 'Блок должен присутствовать на странице')
        .yaVisibleCount(block.link()).then(length =>
            assert.equal(length, links.length, `В блоке должно быть ${links.length} ссылок`))
        .then(() => Promise.all(
            links.map(item => browser
                .getText(item.selector)
                .then(text => assert.equal(text, item.name,
                    `Ссылка на ${item.name} имеет неправильный текст`))
                .then(() => checkCounterIfExists(browser, item))
                .yaCheckLink(item.selector)
                .then(url => browser.yaCheckURL(url, item.url,
                    `Ссылка на ${item.name} имеет неправильный URL`, { skipProtocol: true }))
            )
        ));
}

module.exports = function(query, block, links) {
    describe('Поиск в других системах' + (query === '' ? ' на пустом запросе' : ''), function() {
        it('в домене ru', function() {
            return checkLinks(this.browser, { text: query, exp_flags: 'hide-popups=1' }, links, block);
        });

        it('с семейным фильтром', function() {
            return checkLinks(this.browser, { text: query, family: 'yes', exp_flags: 'hide-popups=1' }, familyFilter(links), block);
        });
    });
};
