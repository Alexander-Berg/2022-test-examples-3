import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на alternate-url открытой страницы.
 * @property {PageObject.AlternateUrl} this.alternateUrl
 */
export default makeSuite('Alternate-url страницы.', {
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'должен быть равен указанному урлу.': makeCase({
                id: 'marketfront-2331',
                issue: 'MARKETVERSTKA-31330',
                params: {
                    expectedUrl: 'Ожидаемый alternate-url',
                },
                test() {
                    const {expectedUrl} = this.params;

                    return this.alternateUrl
                        .getUrl()
                        .should.eventually.be.link(
                            expectedUrl,
                            {
                                skipProtocol: true,
                            }
                        );
                },
            }),
        },
    },
});
