import {makeSuite, makeCase} from '@yandex-market/ginny';

import {
    checkHrefOnExpressParams,
} from '@self/platform/spec/hermione2/utils/express';

export default makeSuite('Поисковая выдача', {
    story: {
        'При ненайденном товаре': {
            'содержит корректное описание': makeCase({
                async test() {
                    const {
                        expectedTitle,
                        expectedWithLinkExpressParams,
                    } = this.params;

                    await this.emptySearchTitle.waitForVisible();
                    await this.emptySearchTitle.root.getText().should.eventually.be.equal(
                        expectedTitle, 'заголовок соответствует ожидаемому'
                    );

                    await this.errorActions.root.getText().should.eventually.be.equal(
                        'на всём Маркете', 'концовка текста соответствует ожидаемому'
                    );

                    const marketHref = await this.errorActions.getLinkHref();
                    const withUrlExpressParams = checkHrefOnExpressParams(
                        marketHref,
                        expectedWithLinkExpressParams
                    );
                    await this.browser.expect(withUrlExpressParams).to.be.equal(
                        expectedWithLinkExpressParams,
                        'адрес ссылки в тексте соответствует ожидаемому'
                    );
                },
            }),
        },
    },
});
