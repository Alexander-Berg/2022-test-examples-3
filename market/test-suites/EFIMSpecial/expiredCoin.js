import {makeCase, makeSuite} from 'ginny';

import {bindBonus, TARGET_STEP} from '@self/project/src/spec/hermione/scenarios/EFIMSpecial';

module.exports = makeSuite('Неактивный купон', {
    story: makeSuite('Открытие попапа с купоном.', {
        story: {
            beforeEach() {
                return this.browser.yaScenario(this, bindBonus, TARGET_STEP.FUTURE_COIN_POPUP);
            },
            'Кнопка привязки не должна отображаться': makeCase({
                id: 'bluemarket-3378',
                issue: 'BLUEMARKET-10249',
                test() {
                    return this.popupBindButton.isExisting()
                        .should.eventually.to.be.equal(
                            false,
                            'Кнопка привязки купона не должна отобржатся!'
                        );
                },
            }),
        },
    }),
});
