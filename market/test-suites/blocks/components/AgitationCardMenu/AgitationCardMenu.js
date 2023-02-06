import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.components.AgitationCard} agitationCard
 * @param {PageObject.components.AgitationCardMenu} agitationCardMenu
 */
export default makeSuite('Меню карточки агитации.', {
    story: {
        'Кнопка "Удалить".': {
            'По умолчанию': {
                'видна.': makeCase(
                    {
                        id: 'marketfront-4082',
                        async test() {
                            await this.agitationCardMenu.isCancelButtonVisible()
                                .should.eventually.equal(true, 'Кнопка удалить видна');
                        },
                    }),
            },
            'При клике': {
                'прячет карточку.': makeCase({
                    id: 'marketfront-4088',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.agitationCardMenu.clickCancelButton(),
                            valueGetter: () => this.agitationCard.isVisible(),
                        });
                        await this.agitationCard.isVisible()
                            .should.eventually.equal(false, 'Карточка спряталась');
                    },
                }),
            },
        },
    },
});
