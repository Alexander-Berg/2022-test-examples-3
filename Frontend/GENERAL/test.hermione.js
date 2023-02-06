const VERSIONS = ['zen', 'browser', 'static'];
const TAB = '\uE004';

function appendStyle(container, action) {
    let css = '*, :before, :after {' +
        'transition-delay: 0s !important;' +
        'transition-duration: 0s !important;' +
        'animation-duration: 0s !important;' +
        'animation-delay: 0s !important;' +
    '}';
    if (action === 'default') {
        css += '.container{ height: 600px;} ';
    } else if (action === 'expand more') {
        css += '.container{ height: 900px; } ';
    } else {
        css += '.container{ height: 600px;} ' + container + '{overflow: scroll; } ';
    }
    let head = document.head;
    let style = document.createElement('style');

    style.type = 'text/css';
    style.appendChild(document.createTextNode(css));

    head.appendChild(style);
}

describe('GNC', () => {
    VERSIONS.forEach(version => {
        if (version === 'browser') {
            hermione.skip.in(['chrome-phone', 'chrome-pad'], 'На тачах отсутствует browser версия');
        }
        describe(version, () => {
            let url = `dist/test/${version}/index.ru.html`;
            let container = version === 'browser' ? '.notifier-container' : '.container';
            let visibleElement = version === 'zen' ? '.gnc-avatar' : '.gnc-notification-header__kebab .gnc-notifications-item__menu-kebab';

            hermione.skip.in(['chrome-phone', 'chrome-pad'], 'На тачах нет hover');
            describe('links', () => {
                it('not-viewed', function() {
                    return this.browser
                        .url(url)
                        .waitForVisible(visibleElement, 10000)
                        .moveToObject('.gnc-notifications-item-wrapper:nth-child(2)')
                        .assertView('plain', ['.gnc-notifications-item-wrapper_not-viewed'])
                        .moveToObject('.gnc-notifications-item-wrapper_not-viewed')
                        .assertView('hovered', ['.gnc-notifications-item-wrapper_not-viewed']);
                });

                it('viewed', function() {
                    return this.browser
                        .url(url)
                        .waitForVisible(visibleElement, 10000)
                        .execute(function() {
                            document.querySelector('.gnc-notifications-item-wrapper:first-child').classList.remove('gnc-notifications-item-wrapper_not-viewed');
                        })
                        .moveToObject('.gnc-notifications-item-wrapper:nth-child(2)')
                        .assertView('plain', ['.gnc-notifications-item-wrapper'])
                        .moveToObject('.gnc-notifications-item-wrapper')
                        .assertView('hovered', ['.gnc-notifications-item-wrapper']);
                });
            });

            if (version !== 'zen') {
                describe('services', () => {
                    it('services popup', function() {
                        return this.browser
                            .url(url)
                            .execute(appendStyle, container, 'default')
                            .waitForVisible(visibleElement, 10000)
                            .moveToObject('.gnc-notification-header__filter')
                            .assertView('hovered filter', [container])
                            .click('.gnc-notification-header__filter')
                            .assertView('opened popup services', [container])
                            .waitForVisible('.gnc-notification-header__filter .gnc-notifications-item__popup-option:nth-child(2)', 10000)
                            .moveToObject('.gnc-notification-header__filter .gnc-notifications-item__popup-option:nth-child(2)')
                            .assertView('popup services hovered', [container])
                            .click('.gnc-notification-header__filter .gnc-notifications-item__popup-option:nth-child(2)')
                            .assertView('service filter', ['.gnc-notification-header__controls'])
                            .moveToObject('.gnc-notification-header__filter')
                            .assertView('hovered service filter', ['.gnc-notification-header__controls']);
                    });
                });
            }
            describe('actions', () => {
                if (version !== 'zen') {
                    it('kebab', function() {
                        return this.browser
                            .url(url)
                            .execute(appendStyle, container, 'default')
                            .waitForVisible(visibleElement, 10000)
                            .moveToObject('.gnc-notification-list .gnc-notifications-item-wrapper:first-child')
                            .moveToObject('.gnc-notification-header__kebab .gnc-notifications-item__menu-kebab')
                            .click('.gnc-notification-header__kebab .gnc-notifications-item__menu-kebab')
                            .assertView('opened', [container]);
                    });

                    it('setting', function() {
                        return this.browser
                            .url(url)
                            .waitForVisible(visibleElement, 10000)
                            .moveToObject('.gnc-notification-header__kebab .gnc-notifications-item__menu-kebab')
                            .click('.gnc-notification-header__kebab .gnc-notifications-item__menu-kebab')
                            .click('.gnc-notification-header__kebab .gnc-notifications-item__popup-option:last-child')
                            .assertView('plain', ['#service-container'])
                            .keys(TAB)
                            .assertView('after-tab', ['#service-container']);
                    });

                    it('mark all as read', function() {
                        return this.browser
                            .url(url)
                            .execute(appendStyle, container, 'default')
                            .waitForVisible(visibleElement, 10000)
                            .moveToObject('.gnc-notification-header__kebab .gnc-notifications-item__menu-kebab')
                            .click('.gnc-notification-header__kebab .gnc-notifications-item__menu-kebab')
                            .click('.gnc-notification-header__kebab .gnc-notifications-item__popup-option:first-child')
                            .moveToObject('body', 0, 0)
                            .assertView('plain', [container]);
                    });

                    it('expand more',
                        version === 'browser' ? function() {
                            return this.browser
                                .url(url)
                                .waitForVisible(visibleElement, 10000)
                                .execute(function() {
                                    document.getElementsByClassName('gnc-notification-list')[0].scrollTop = 500;
                                })
                                .assertView('collapsed body', [container])
                                .click('.gnc-expand-button__content')
                                .pause(200)
                                .execute(function() {
                                    const items = document.getElementsByClassName('gnc-notifications-item-wrapper__expanded-item');
                                    for (let i = 0; i < items.length; i++) {
                                        items[i].classList.remove('gnc-notifications-item-wrapper__expanded-item');
                                    }
                                })
                                .pause(300)
                                .assertView('expanded body', [container]);
                        } : function() {
                            return this.browser
                                .url(url)
                                .waitForVisible(visibleElement, 10000)
                                .moveToObject('.gnc-notification-list .gnc-notifications-item-wrapper:first-child>.gnc-notifications-item')
                                .click('.gnc-expand-button__content')
                                .moveToObject('body', 0, 0)
                                .pause(200)
                                .execute(function() {
                                    const items = document.getElementsByClassName('gnc-notifications-item-wrapper__expanded-item');
                                    for (let i = 0; i < items.length; i++) {
                                        items[i].classList.remove('gnc-notifications-item-wrapper__expanded-item');
                                    }
                                })
                                .pause(300)
                                .assertView('expanded body', [container]);
                        },
                    );
                }

                it('mark first as read', function() {
                    return this.browser
                        .url(url)
                        .execute(appendStyle, container, 'default')
                        .waitForVisible(visibleElement, 10000)
                        .pause(3000)
                        .moveToObject('.gnc-notification-list .gnc-notifications-item-wrapper:first-child>.gnc-notifications-item')
                        .moveToObject('.gnc-notification-list .gnc-notifications-item-wrapper:first-child .gnc-notifications-item__menu-kebab')
                        .click('.gnc-notification-list .gnc-notifications-item-wrapper:first-child .gnc-notifications-item__menu-kebab')
                        .click('.gnc-notification-list .gnc-notifications-item-wrapper:first-child .gnc-notifications-item__popup>.gnc-notifications-item__popup-option')
                        .moveToObject('body', 0, 0)
                        .pause(3000)
                        .assertView('plain', [container]);
                });
            });

            describe('handling errors', () => {
                it('valid notifications', function() {
                    return this.browser
                        .url(url)
                        .execute(appendStyle, container, 'default')
                        .waitForVisible(visibleElement, 10000)
                        .moveToObject('body', 0, 0)
                        .assertView('plain', [container]);
                });

                it('invalid notifications', function() {
                    const urlWithError = `dist/test/${version}/index.ru.html?config=with_errors`;
                    return this.browser
                        .url(urlWithError)
                        .execute(appendStyle, container, 'default')
                        .waitForVisible(visibleElement, 10000)
                        .moveToObject('body', 0, 0)
                        .assertView('plain', [container]);
                });
            });
        });
    });
});
