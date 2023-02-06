import initialState from 'spec/lib/page-mocks/authorities.json';
import {makeKadavrSuite} from 'spec/gemini/lib/kadavr-gemini';
import Tabs from 'spec/page-objects/Tabs';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';
import {User} from 'spec/lib/constants/users/users';

import authorities from './authorities';
import hints from './hints';

type Options = {
    has: (permissions: string | string[]) => boolean;
    url: string;
    user: User;
    testMarketAnalytics: boolean;
};

type Product = {
    name: string;
    permissions: string | string[];
    paid?: boolean;
};

export default ({user, url, has, testMarketAnalytics}: Options) => {
    const PRODUCTS: Product[] = [
        {
            name: 'Admin',
            permissions: [PERMISSIONS.entries.read, PERMISSIONS.offerta.write],
        },
        {
            name: 'Opinions',
            permissions: PERMISSIONS.opinions.read,
        },
        {
            name: 'Questions',
            permissions: PERMISSIONS.questions.read,
        },
        {
            name: 'Special Projects',
            permissions: PERMISSIONS.entries.read,
        },
        {
            name: 'Recommended Shops',
            permissions: PERMISSIONS.recommended.read,
        },
        {
            name: 'Model Bids',
            permissions: PERMISSIONS.modelbids.read,
            paid: true,
        },
        {
            name: 'Incuts',
            permissions: PERMISSIONS.incuts.read,
            paid: true,
        },
        {
            name: 'Brand Zone',
            permissions: PERMISSIONS.brandzone.read,
            paid: true,
        },
        {
            name: 'Marketing Banners',
            permissions: PERMISSIONS.marketingBanners.read,
            paid: true,
        },
        {
            name: 'Marketing Landings',
            permissions: PERMISSIONS.marketingLandings.read,
            paid: true,
        },
        {
            name: 'Marketing Promo',
            permissions: PERMISSIONS.marketingPromo.read,
            paid: true,
        },
        {
            name: 'Marketing Email',
            permissions: PERMISSIONS.marketingEmail.read,
            paid: true,
        },
        {
            name: 'Marketing Shop-in-shop',
            permissions: PERMISSIONS.marketingShopInShop.read,
            paid: true,
        },
        {
            name: 'Marketing Product Placement',
            permissions: PERMISSIONS.marketingProductPlacement.read,
            paid: true,
        },
        {
            name: 'Marketing Logo',
            permissions: PERMISSIONS.marketingLogo.read,
            paid: true,
        },
        {
            name: 'Marketing TV',
            permissions: PERMISSIONS.marketingTv.read,
            paid: true,
        },
        {
            name: 'Marketing External Platforms',
            permissions: PERMISSIONS.marketingExternalPlatforms.read,
            paid: true,
        },
        {
            name: 'Opinions Promotion',
            permissions: PERMISSIONS.paidOpinions.read,
            paid: true,
        },
    ];

    if (testMarketAnalytics) {
        PRODUCTS.splice(7, 0, {
            name: 'Market Analytics',
            permissions: PERMISSIONS.analytics.read,
            paid: true,
        });
    }

    const availableProducts = PRODUCTS.filter(({permissions}) => has(permissions));

    return makeKadavrSuite({
        url,
        user,
        suiteName: 'Users',
        state: {
            vendorsAuthorities: initialState,
        },
        before(actions) {
            actions
                .waitForElementToShow(Tabs.activePane, 10000)
                // Ждём загрузки данных
                .wait(7000);
        },
        childSuites: [authorities(availableProducts), hints(availableProducts)],
    });
};
