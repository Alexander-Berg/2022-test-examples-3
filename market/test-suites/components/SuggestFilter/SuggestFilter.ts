'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на блок SuggestFilter.
 * @param {PageObject.Suggest} suggest
 */
export default makeSuite('Фильтр suggest.', {
    environment: 'testing',
    story: {
        'При выборе элемента': {
            'переводит на целевую страницу': makeCase({
                params: {
                    text: 'Текст фильтра',
                    url: 'Адрес ссылки',
                },
                async test() {
                    const {url, urlComparsionOptions = {}, text = ''} = this.params;
                    await this.suggest.setText(text);

                    await this.suggest.setFocus();

                    await this.popup.waitForPopupShown();

                    await this.suggest.waitForPopupItemsCount(1);

                    return this.browser
                        .vndWaitForChangeUrl(() => this.suggest.selectItem(0))
                        .should.eventually.be.link(url, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                            ...urlComparsionOptions,
                        });
                },
            }),
        },
    },
});
