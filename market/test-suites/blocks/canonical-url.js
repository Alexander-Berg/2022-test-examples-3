import {makeSuite, makeCase} from 'ginny';
import url from 'url';

/**
 * Тесты на canonical-url открытой страницы.
 * @property {PageObject.CanonicalUrl} this.canonicalUrl
 */
export default makeSuite('Canonical-url страницы.', {
    feature: 'Canonical-url страницы',
    story: {
        'По умолчанию': {
            'должен быть равен указанному урлу.': makeCase({
                id: 'marketfront-2404',
                issue: 'MARKETVERSTKA-28691',
                params: {
                    expectedUrl: 'Ожидаемая часть canonical-url\'а',
                },
                test() {
                    const {expectedUrl} = this.params;

                    return this.canonicalUrl
                        .getUrl()
                        .then(canonicalUrl => {
                            const {path} = url.parse(canonicalUrl);

                            return decodeURIComponent(path);
                        })
                        .should.eventually.be.equal(
                            expectedUrl,
                            `Canonical-url без домена должен быть равен "${expectedUrl}".`
                        );
                },
            }),
        },
    },
});
