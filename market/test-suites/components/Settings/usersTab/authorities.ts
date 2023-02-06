'use strict';

import {scrollToElementBySelector} from 'spec/gemini/helpers';
import TabsB2b from 'spec/page-objects/TabsB2b';
import RestrictedLabel from 'spec/page-objects/RestrictedLabel';
import {Case} from 'spec/gemini/lib/types';

export default <Products extends {name: string} & Record<string, unknown>>(products: Products[]) =>
    ({
        suiteName: 'Authorities',
        before(actions) {
            actions.waitForElementToShow(TabsB2b.root, 10000);
        },
        childSuites: products.map(({name: suiteName}, index) => {
            const selector = `${RestrictedLabel.root}:nth-of-type(${index + 1})`;

            return {
                suiteName,
                selector,
                before(actions) {
                    scrollToElementBySelector.call(actions, selector);
                },
                // eslint-disable-next-line @typescript-eslint/no-empty-function
                capture() {},
            } as Case;
        }),
    } as Case);
