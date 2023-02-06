'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на отображение бейджа вендора
 * @param {PageObject.VendorBadge} badge
 */
export default makeSuite('Бейдж производителя.', {
    story: {
        'При отправке официальным представителем': {
            'отображается галочка': makeCase({
                async test() {
                    await this.badge.isVisible().should.eventually.be.equal(true, 'Бейдж вендора отображается');

                    await this.browser.vndScrollToBottom();

                    await this.badge.click();

                    await this.badge.hint.isVisible().should.eventually.be.equal(true, 'Подсказка отображается');

                    await this.badge
                        .getHintText()
                        .should.eventually.be.equal('Официальный представитель бренда', 'Текст подсказки корректный');
                },
            }),
        },
    },
});
