'use strict';

import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Таб, смена url.', {
    story: {
        'Меняет параметр url': makeCase({
            test() {
                const task = () => this.tabs.getTabByTitle(this.params.tabsTitle).click();

                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                return this.browser.yaWaitForChangeUrl(task).then(
                    // @ts-expect-error(TS7006) найдено в рамках VNDFRONT-4580
                    url =>
                        url.should.be.link(
                            {
                                query: this.params.query,
                            },
                            {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                                skipPathname: true,
                            },
                        ),
                );
            },
        }),
    },
});
