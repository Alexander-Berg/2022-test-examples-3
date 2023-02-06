'use strict';

const PO = require('../../page-objects/touch-phone/index').PO;

hermione.only.notIn('searchapp', 'фича не актуальна в searchapp');
specs('Подвал/сообщение о медленном интернете', function() {
    it('проверка ссылок', function() {
        return this.browser
            .yaOpenSerp('text=test')
            .yaWaitForVisible(PO.footer2(), 'Футер не появился')
            .yaCheckLink(PO.footer2.enterLink(), { target: '' }).then(url => this.browser
                .yaCheckURL(url, 'https://passport.yandex.ru/auth', 'Сломана ссылка Войти', {
                    skipQuery: true
                }))
            .yaMockExternalUrl(PO.footer2.enterLink())
            .yaCheckBaobabCounter(PO.footer2.enterLink(), {
                path: '/$page/$footer/link[@type="login"]'
            })

            .yaCheckLink(PO.footer2.allServicesLink(), { target: '' }).then(url => this.browser
                .yaCheckURL(url, 'http://yandex.ru/all', 'Сломана ссылка Все сервисы', {
                    skipProtocol: true
                }))
            .yaMockExternalUrl(PO.footer2.allServicesLink())
            .yaCheckBaobabCounter(PO.footer2.allServicesLink(), { path: '/$page/$footer/link[@type="all"]' })

            .yaCheckLink(PO.footer2.feedbackLink(), { target: '' }).then(url => this.browser
                .yaCheckURL(url,
                    { hostname: 'yandex.ru', pathname: '/support/search/troubleshooting/feedback.html' },
                    'Сломана ссылка Обратная связь',
                    {
                        skipProtocol: true,
                        skipPathnameTrail: true,
                        skipQuery: true
                    }))
            .yaMockExternalUrl(PO.footer2.feedbackLink())
            .yaCheckBaobabCounter(PO.footer2.feedbackLink(), { path: '/$page/$footer/link[@type="feedback"]' });
    });

    it('отсутствует переключатель при быстром соединении', function() {
        return this.browser
            .yaOpenSerp({ text: 'test', user_connection: 'slow_connection=0' })
            .yaWaitForVisible(PO.footer2(), 'Футер не появился')
            .yaShouldNotExist(PO.footer2.switcherLink())
            .yaShouldNotExist(PO.footer2.switcherTitle());
    });

    hermione.only.notIn('winphone', 'winphone не touch, а smart');
    it('присутствует переключатель при медленном соединении для touch-устройства', function() {
        return this.browser
            .yaOpenSerp({ text: 'test', user_connection: 'slow_connection=1' })
            .yaWaitForVisible(PO.footer2(), 'Футер не появился')
            .yaShouldExist(PO.footer2.switcherLink())
            .yaShouldExist(PO.footer2.switcherTitle());
    });

    hermione.only.in('winphone', 'winphone не touch, а smart');
    it('отсутствует переключатель при медленном соединении для smart-устройства', function() {
        return this.browser
            .yaOpenSerp({ text: 'test', user_connection: 'slow_connection=1' })
            .yaWaitForVisible(PO.footer2(), 'Футер не появился')
            .yaShouldNotExist(PO.footer2.switcherLink())
            .yaShouldNotExist(PO.footer2.switcherTitle());
    });
});
