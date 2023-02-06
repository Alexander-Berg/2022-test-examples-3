import {makeCase, makeSuite} from 'ginny';

import Coin from '@self/root/src/uikit/components/Coin/__pageObject';
import Image from '@self/root/src/uikit/components/Image/__pageObject';

module.exports = makeSuite('Купон.', {
    params: {
        promo: 'Проверяемая промо-акция',
    },
    id: 'bluemarket-2797',
    issue: 'BLUEMARKET-6631',
    story: {
        beforeEach() {
            this.setPageObjects({
                bonus: () => this.createPageObject(Coin, {parent: this.bindBonus}),
                bonusImage: () => this.createPageObject(Image, {parent: this.bonus}),
            });
        },

        'Отображается корректно': makeCase({
            async test() {
                await this.bonus.isVisible()
                    .should.eventually.be.equal(true, 'Купон должен быть видимым.');

                await this.bonus.getTitle()
                    .should.eventually.be.equal(
                        this.params.promo.title,
                        'Купон должен иметь заголовок, получаемый из loyalty'
                    );

                return this.bonusImage.getSrc()
                    .should.eventually.be.match(
                        new RegExp(this.params.promo.images.standard),
                        'Купон должен отображаться с картинкой, получаемой из loyalty'
                    );
            },
        }),
    },
});
