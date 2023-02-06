import {makeCase, makeSuite} from 'ginny';
import MiniCard from '@self/platform/components/PageCardTitle/MiniCard/__pageObject';
import ClickoutButton from '@self/platform/spec/page-objects/components/ClickoutButton';
import {stateProductWithDO} from '@self/platform/spec/hermione/configs/seo/mocks';

import productOptionsFixture from '../fixtures/productOptions';

const ENCRYPTED_URL = '/redir/encrypted';

export default makeSuite('Мини Дефолтный оффер', {
    environment: 'kadavr',
    id: 'marketfront-4205',
    issue: 'MARKETFRONT-25074',
    story: {
        async beforeEach() {
            const productId = 12345;
            const state = stateProductWithDO(productId);

            await this.setPageObjects({
                miniCard: () => this.createPageObject(MiniCard),
                clickoutButton: () =>
                    this.createPageObject(ClickoutButton, {
                        root: this.miniCard,
                    }),
            });

            await this.browser.setState('report', state);

            return this.browser.yaOpenPage('market:product-offers', {
                slug: productOptionsFixture.slug,
                productId,
            });
        },
        'Кнопка "В магазин"': {
            'Имеет корректную ссылку': makeCase({
                async test() {
                    const url = await this.clickoutButton.getLinkUrl();
                    return this.expect(url.path)
                        .to.equal(ENCRYPTED_URL, 'Кликаут ссылка корректна');
                },
            }),
        },
    },
});
