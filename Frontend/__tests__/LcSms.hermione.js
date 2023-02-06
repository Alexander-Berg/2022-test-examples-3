specs({
    feature: 'LcSms',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычный блок смс', function() {
        return this.browser
            .url('/turbo?stub=lcsms/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcSms(), {
                screenshotDelay: 3000,
                allowViewportOverflow: true,
            });
    });

    hermione.only.notIn('safari13');
    it('Блок смс с заголовком (выравнивание по левому краю)', function() {
        return this.browser
            .url('/turbo?stub=lcsms/withTitleAlignLeft.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcSms());
    });

    hermione.only.notIn('safari13');
    it('Блок смс с заголовком (выравнивание по центру)', function() {
        return this.browser
            .url('/turbo?stub=lcsms/withTitleAlignCenter.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcSms());
    });

    hermione.only.notIn('safari13');
    it('Блок смс с заголовком (выравнивание по правому краю)', function() {
        return this.browser
            .url('/turbo?stub=lcsms/withTitleAlignRight.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcSms());
    });

    hermione.only.notIn('safari13');
    it('Отправка смс', function() {
        return this.browser
            .url('/turbo?stub=lcsms/with-mock.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcSms(), 'Секция смс не загрузилась')
            .yaMockFetch({
                status: 200,
                urlDataMap: {
                    '/lc-captcha': JSON.stringify({
                        src: 'https://avatars.mds.yandex.net/get-lpc/1220100/5fec77c0-0070-482b-b77e-aa68842bd448/orig',
                        key: 'hello',
                    }),
                    '/lc-sms': JSON.stringify({
                        status: 'success',
                    })
                },
            })
            .click(PO.lcInput.phone.input())
            .keys('79261234567'.split(''))
            .yaWaitForVisible(PO.lcInput.number.input(), 'Инпут капчи не загрузился')
            .click(PO.lcInput.number.input())
            .keys('123456'.split(''))
            .click(PO.lcButton())
            .yaWaitForVisible('.lc-sms-success-modal', 'Модальное окно не загрузилось')
            .assertView('plain', PO.page(), {
                allowViewportOverflow: true,
            });
    });
});
