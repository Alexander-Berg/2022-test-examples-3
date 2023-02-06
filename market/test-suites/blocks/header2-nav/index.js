import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок header2-nav
 * @param {PageObject.Header2Nav} headerNav
 */

export default makeSuite('Боковая навигационная панель пользователя.', {
    environment: 'testing',
    issue: 'MARKETVERSTKA-30421',
    id: 'marketfront-2701',
    severity: 'critical',
    story: {
        'Иконка бокового меню.': {
            'При клике': {
                'открывает боковое меню': makeCase({
                    test() {
                        return this.headerNav.clickOpen()
                            .then(() => this.headerNav.waitForMenuVisible())
                            .should.eventually.equal(true, 'Боковое меню отображается');
                    },
                }),
            },
        },
    },
});
