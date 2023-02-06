import {
    makeSuite,
    mergeSuites,
    makeCase,
} from '@yandex-market/ginny';

import {
    checkHrefOnExpressParams,
} from '@self/platform/spec/hermione2/utils/express';

export default makeSuite('Адресная строка', {
    story: mergeSuites(
        {
            'Параметры экспресса': {
                'соответствуют ожидаемому': makeCase({
                    async test() {
                        const {
                            expectedWithUrlExpressParams,
                        } = this.params;

                        /**
                         * В возвращаемом объекте нет поля searchParams,
                         * поэтому извлекаем href и в функции получаем полный URL
                         * ¯\_(ツ)_/¯
                         */
                        const {href} = await this.browser.yaParseUrl();
                        const withUrlExpressParams = checkHrefOnExpressParams(
                            href,
                            expectedWithUrlExpressParams
                        );
                        await this.browser.expect(withUrlExpressParams).to.be.equal(
                            expectedWithUrlExpressParams,
                            'наличие параметров экспресса в адресной строке соответствует ожидаемому'
                        );
                    },
                }),
            },
        }
    ),
});
