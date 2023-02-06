import {prepareSuite, makeSuite} from 'ginny';
import {createProduct, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import CommentariesSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/Commentaries/twoLevelCommentaries';

import {product as productMock} from './mocks/product.mock';
import {versus} from './mocks/versus.mock';

const product1 = createProduct(productMock, versus.products[0].id);
const product2 = createProduct(productMock, versus.products[1].id);

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница Автосравнений.', {
    environment: 'kadavr',
    story: prepareSuite(CommentariesSuite, {
        params: {
            pageTemplate: 'touch:versus',
            pageParams: {
                'id': versus.id,
                'slug': 'any',
                'no-tests': 1,
            },
            entityId: versus.id,
            defaultLimit: 1,
        },
        hooks: {
            async beforeEach() {
                const schema = {
                    versus: [versus],
                };

                const state = mergeState([product1, product2]);

                await this.browser.setState('report', state);

                this.params = {
                    ...this.params,
                    schema,
                };
            },
        },
    }),
});
