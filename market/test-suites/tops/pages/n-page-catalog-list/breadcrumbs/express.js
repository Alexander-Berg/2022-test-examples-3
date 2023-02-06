import {mergeSuites, makeSuite} from 'ginny';

import Breadcrumbs from '@self/platform/components/Breadcrumbs/__pageObject';
import {mobilePhonesExpress} from '@self/root/src/spec/hermione/kadavr-mock/cataloger/navigationPathExpress';

export const breadcrumbsExpressSuite = makeSuite('Хлебные крошки', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-56168',
    id: 'marketfront-5092',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.yaSetCookie({
                    name: 'currentRegionId',
                    value: '213',
                });

                await this.browser.setState('Cataloger.tree', mobilePhonesExpress);

                this.setPageObjects({
                    breadcrumbs: () => this.createPageObject(Breadcrumbs),
                });

                return this.browser.yaOpenPage('market:list', {
                    'nid': 23282330,
                    'local-offers-first': 0,
                    'onstock': 1,
                });
            },
        }
    ),
});
