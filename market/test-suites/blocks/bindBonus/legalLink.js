import {makeCase, makeSuite} from 'ginny';

import {LegalLink} from '@self/root/src/components/BindBonus/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';

module.exports = makeSuite('Ссылка на условия акции.', {
    params: {
        promo: 'Проверяемая промо-акция',
    },
    id: 'bluemarket-2801',
    issue: 'BLUEMARKET-6631',
    story: {
        beforeEach() {
            this.setPageObjects({
                legalLinkBlock: () => this.createPageObject(LegalLink, {parent: this.bindBonus}),
                link: () => this.createPageObject(Link, {parent: this.legalLinkBlock}),
            });
        },

        'Отображается корретно и ведет на страницу, указанную в лоялти': makeCase({
            async test() {
                await this.link.isVisible()
                    .should.eventually.be.equal(true, 'Ссылка должна быть видимма');

                await this.legalLinkBlock.getText()
                    .should.eventually.be.equal(
                        'Подробнее об условиях',
                        'Блок с ссылкой должен содержать текст "Подробнее об условиях"'
                    );

                await this.link.getText()
                    .should.eventually.be.equal(
                        'условиях',
                        'Ссылка должна содержать текст "условиях"'
                    );

                const targetLink = this.params.promo.promoOfferAndAcceptance;

                return this.link.getHref()
                    .should.eventually.be.equal(
                        targetLink,
                        `Ссылка должна вести на страницу ${targetLink}, указанную в лоялти`
                    );
            },
        }),
    },
});
