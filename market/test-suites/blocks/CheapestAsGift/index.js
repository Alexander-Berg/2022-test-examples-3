import {makeCase, makeSuite} from 'ginny';
import {pluralize} from '@self/project/src/helpers/string';


const createExpectedText = count => {
    const productPluralize = pluralize(count, 'товар', 'товара', 'товаров');

    return `${count} ${productPluralize} по цене ${count - 1}`;
};

export default makeSuite('Акция N=N+1', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-28961',
    params: {
        count: 'Количество N',
    },
    story: {
        'По умолчанию отображается': makeCase({
            async test() {
                await this.cheapestAsGiftTerms.isVisible()
                    .should.eventually.to.be.equal(
                        true,
                        'Акция должна быть видна'
                    );
            },
        }),
        'Содержит правильный текст': makeCase({
            async test() {
                const EXPECTED_TEXT = createExpectedText(
                    this.params.count
                );

                await this.cheapestAsGiftTerms.getText()
                    .should.eventually.to.be.include(
                        EXPECTED_TEXT,
                        `Текст должен содержать ${EXPECTED_TEXT}`
                    );
            },
        }),
    },
});
