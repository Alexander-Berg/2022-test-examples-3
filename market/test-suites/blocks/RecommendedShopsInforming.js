import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на блок n-recommended-shops-informing.
 * @param {PageObject.RecommendedShopsInforming} shopsInforming
 */

export default makeSuite('Блок рекомендованных магазинов.', {
    feature: 'Структура страницы',
    story: {
        'Заголовок блока.': {
            'По-умолчанию': {
                'корректно отображается.': makeCase({
                    id: 'marketfront-917',
                    test() {
                        return this.browser
                            .yaSafeAction(this.shopsInforming.waitForTitleVisible(), false)
                            .should.eventually.be.equal(true, 'Заголовок отобразился');
                    },
                }),
            },
        },

        'Значок рекомендованного магазина.': {
            'По-умолчанию': {
                'присутствует в заголовке.': makeCase({
                    id: 'marketfront-918',
                    test() {
                        return this.browser
                            .yaSafeAction(this.shopsInforming.waitForRecommendeImageVisible(), false)
                            .should.eventually.be.equal(true, 'Значок рекомендованного магазина отобразился');
                    },
                }),
            },
        },

        'Текст блока.': {
            'По-умолчанию': {
                'корректно отображается.': makeCase({
                    id: 'marketfront-920',
                    test() {
                        return this.browser
                            .yaSafeAction(this.shopsInforming.waitForTextVisible(), false)
                            .should.eventually.be.equal(true, 'Текст отобразился');
                    },
                }),
            },
        },

        'Ссылка на сайт производителя.': {
            'По-умолчанию': {
                'открывается в новой вкладке.': makeCase({
                    id: 'marketfront-919',
                    test() {
                        return this.browser
                            .yaIsLinkOpener(this.shopsInforming.link, {
                                target: '_blank',
                            })
                            .should.eventually.be.equal(
                                true,
                                'Ссылка откроется в новом окне (атрибут target="_blank")'
                            );
                    },
                }),
            },
        },
    },
});
