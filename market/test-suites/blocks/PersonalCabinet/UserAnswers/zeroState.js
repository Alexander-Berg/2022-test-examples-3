import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.widgets.components.PersonalCabinetZeroState} zeroState
 */

export default makeSuite('Zero стейт страницы.', {
    story: {
        'Если сниппеты отсутствуют': {
            'по умолчанию': {
                'отображается': makeCase({
                    id: 'm-touch-3130',
                    async test() {
                        return this.zeroState.isVisible()
                            .should.eventually.be.equal(true, 'Zero стейт страницы отображается');
                    },
                }),
            },
        },
    },
});
