'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Link} link
 * @deprecated
 */

export default makeSuite('Внешняя ссылка.', {
    environment: 'testing',
    params: {
        url: 'Ссылка',
    },
    story: {
        'Открывается в новой вкладке': makeCase({
            test() {
                return (
                    this.browser
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        .vndWaitForChangeTab(() => this.link.root.click())
                        // @ts-expect-error(TS7006) найдено в рамках VNDFRONT-4580
                        .then(browserUrl =>
                            browserUrl.should.be.link(this.params.url, {
                                mode: 'match',
                                skipQuery: true,
                            }),
                        )
                );
            },
        }),
    },
});
