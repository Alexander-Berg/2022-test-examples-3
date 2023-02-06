'use strict';

const PO = require('../../page-objects/touch-phone/index').PO;

hermione.only.notIn('searchapp', 'фича не актуальна в searchapp');
specs('Старый подвал/сообщение о медленном интернете', function() {
    it('проверка ссылок', function() {
        return this.browser
            .yaOpenSerp({ text: 'test', exp_flags: 'beauty_footer=old' })
            .yaWaitForVisible(PO.footer1(), 'Футер не появился')
            .yaCheckLink(PO.footer1.enterLink(), { target: '' }).then(url => this.browser
                .yaCheckURL(url, 'https://passport.yandex.ru/auth', 'Сломана ссылка Войти', {
                    skipQuery: true
                }))
            .yaMockExternalUrl(PO.footer1.enterLink())
            .yaCheckBaobabCounter(PO.footer1.enterLink(), { path: '/$page/$footer/link[@type="login"]' })

            .yaCheckLink(PO.footer1.allServicesLink(), { target: '' }).then(url => this.browser
                .yaCheckURL(url, 'http://yandex.ru/all', 'Сломана ссылка Все сервисы', {
                    skipProtocol: true
                }))
            .yaMockExternalUrl(PO.footer1.allServicesLink())
            .yaCheckBaobabCounter(PO.footer1.allServicesLink(), { path: '/$page/$footer/link[@type="all"]' })

            .yaCheckLink(PO.footer1.feedbackLink(), { target: '' }).then(url => this.browser
                .yaCheckURL(url,
                    { hostname: 'yandex.ru', pathname: '/support/search/troubleshooting/feedback.html' },
                    'Сломана ссылка Обратная связь',
                    {
                        skipProtocol: true,
                        skipPathnameTrail: true,
                        skipQuery: true
                    }))
            .yaMockExternalUrl(PO.footer1.feedbackLink())
            .yaCheckBaobabCounter(PO.footer1.feedbackLink(), { path: '/$page/$footer/link[@type="feedback"]' })
            .yaCheckLink(PO.footer1.logo(), { target: '' }).then(url => this.browser
                .yaCheckURL(url, 'http://yandex.ru', 'Сломана ссылка на логотипе', {
                    skipProtocol: true
                }))
            .yaMockExternalUrl(PO.footer1.logo())
            .yaCheckBaobabCounter(PO.footer1.logo(), { path: '/$page/$footer/link[@type="morda"]' });
    });

    it('отсутствует переключатель при быстром соединении', function() {
        return this.browser
            .yaOpenSerp({ text: 'test', user_connection: 'slow_connection=0', exp_flags: 'beauty_footer=old' })
            .yaWaitForVisible(PO.footer1(), 'Футер не появился')
            .yaShouldNotExist(PO.footer1.switcherLink())
            .yaShouldNotExist(PO.footer1.switcherTitle());
    });

    hermione.only.notIn('winphone', 'winphone не touch, а smart');
    it('присутствует переключатель при медленном соединении для touch-устройства', function() {
        return this.browser
            .yaOpenSerp({ text: 'test', user_connection: 'slow_connection=1', exp_flags: 'beauty_footer=old' })
            .yaWaitForVisible(PO.footer1(), 'Футер не появился')
            .yaShouldExist(PO.footer1.switcherLink())
            .yaShouldExist(PO.footer1.switcherTitle());
    });

    hermione.only.in('winphone', 'winphone не touch, а smart');
    it('отсутствует переключатель при медленном соединении для smart-устройства', function() {
        return this.browser
            .yaOpenSerp({ text: 'test', user_connection: 'slow_connection=1', exp_flags: 'beauty_footer=old' })
            .yaWaitForVisible(PO.footer1(), 'Футер не появился')
            .yaShouldNotExist(PO.footer1.switcherLink())
            .yaShouldNotExist(PO.footer1.switcherTitle());
    });
});
