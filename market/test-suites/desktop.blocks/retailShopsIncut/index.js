import {prepareSuite} from 'ginny';

// page-objects
import RetailShopsIncut from '@self/root/src/components/RetailShopsIncut/__pageObject';
import RetailShopSnippet from '@self/root/src/components/RetailShopSnippet/__pageObject/index.desktop';
import SrcScrollBox from '@self/root/src/components/ScrollBox/__pageObject';

import RetailShopsIncutSuite from '../../blocks/retailShopsIncut';

export default prepareSuite(RetailShopsIncutSuite, {
    meta: {
        id: 'marketfront-5282',
    },
    pageObjects: {
        retailShopsIncut() {
            return this.createPageObject(RetailShopsIncut);
        },
        shopSnippet1() {
            return this.createPageObject(
                RetailShopSnippet,
                {
                    parent: `${SrcScrollBox.snippet}:nth-of-type(1)`,
                }
            );
        },
        shopSnippet2() {
            return this.createPageObject(
                RetailShopSnippet,
                {
                    parent: `${SrcScrollBox.snippet}:nth-of-type(2)`,
                }
            );
        },
        shopSnippet3() {
            return this.createPageObject(
                RetailShopSnippet,
                {
                    parent: `${SrcScrollBox.snippet}:nth-of-type(3)`,
                }
            );
        },
    },
});
