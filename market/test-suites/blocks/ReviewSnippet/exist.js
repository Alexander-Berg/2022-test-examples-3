import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок ReviewSnippet
 *
 * @param {PageObject.ReviewSnippet} reviewSnippet
 */
export default makeSuite('Сниппет продукта с отзывом.', {
    feature: 'Лучшие отзывы',
    environment: 'testing',
    issue: 'marketfront-3049',
    story: {
        'По умолчанию': {
            'должен присутствовать на странице': makeCase({
                test() {
                    return this.reviewSnippet.isExisting()
                        .should.eventually.equal(true, 'Сниппет присутствует на странице');
                },
            }),
        },
    },
});
