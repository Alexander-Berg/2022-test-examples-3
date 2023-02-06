'use strict';

import initialState from 'spec/lib/page-mocks/products.json';
import {makeKadavrSuite} from 'spec/gemini/lib/kadavr-gemini';
import Products from 'spec/page-objects/Products';
import {User} from 'spec/lib/constants/users/users';

type Options = {user: User; url: string};

export default ({user, url}: Options) =>
    makeKadavrSuite({
        url,
        user,
        suiteName: 'Grid',
        selector: Products.root,
        state: {
            vendorProductsData: initialState,
        },
        before(actions) {
            actions
                .waitForElementToShow(Products.root, 10000)
                // Ждём завершения загрузки катофов
                .wait(10000);
        },
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        capture() {},
    });
