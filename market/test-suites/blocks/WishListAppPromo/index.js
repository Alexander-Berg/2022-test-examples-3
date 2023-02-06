import {makeSuite, makeCase} from 'ginny';

const EXPECTED_STORE_LINK = 'https://redirect.appmetrica.yandex.com/serve/1107475960938196812';

/**
 * Тесты на блок AppPromo
 * @property {PageObject.AppPromo} topAppPromo
 */
export default makeSuite('Промо баннер приложения.', {
    story: {
        'По умолчанию:': {
            'Содержит корректную ссылку': makeCase({
                async test() {
                    const link = await this.topAppPromo.getUrl();
                    return this.expect(link).to.be.equal(EXPECTED_STORE_LINK);
                },
            }),
        },
    },
});
