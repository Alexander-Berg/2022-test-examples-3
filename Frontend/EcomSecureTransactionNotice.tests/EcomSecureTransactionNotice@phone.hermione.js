specs({
    feature: 'EcomSecureTransactionNotice',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=ecomsecuretransactionnotice/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.ecomSecureTransactionNotice())
            .click(PO.ecomSecureTransactionNotice())
            .yaWaitForVisible(PO.ecomSecureTransactionScreen())
            .click(PO.ecomSecureTransactionScreen.breadcrumb.link())
            .yaWaitUntil('Не произошел переход на предыдущую страницу по клику на хлебную крошку', () =>
                this.browser
                    .getUrl() // @TODO [https://st.yandex-team.ru/TURBOUI-1149] при переходе ошибка, нет tpid, темплар не может так подмешать данные в ссылки
                    .then(url => url.includes('ecomsecuretransactionnotice'))
            )
            .click(PO.ecomSecureTransactionNotice())
            .yaWaitForVisible(PO.ecomSecureTransactionScreen())
            .click(PO.ecomSecureTransactionScreen.button())
            .yaWaitUntil('Не произошел переход на предыдущую страницу по клику на кнопку', () =>
                this.browser
                    .getUrl() // @TODO [https://st.yandex-team.ru/TURBOUI-1149] при переходе ошибка, нет tpid, темплар не может так подмешать данные в ссылки
                    .then(url => url.includes('ecomsecuretransactionnotice'))
            );
    });

    hermione.only.notIn('safari13');
    it('Цель метрики', function() {
        return this.browser
            .url('/turbo?stub=ecomsecuretransactionnotice/default.json&exp_flags=analytics-disabled=0')
            .yaWaitForVisible(PO.ecomSecureTransactionNotice(), 'Страница не загрузилась')
            /**
             * Меняем target на _self, чтобы протестировать срабатывание цели метрики
             */
            .execute(function(selector) {
                document.querySelector(selector).setAttribute('target', '_blank');
            }, PO.ecomSecureTransactionNotice())
            .click(PO.ecomSecureTransactionNotice())
            .yaCheckMetrikaGoals({
                '11111111': ['secure-badge-click'],
            });
    });
});
