import {prepareSuite, makeSuite} from 'ginny';

import GenericBundleTermSuite from '@self/platform/spec/hermione/test-suites/blocks/GenericBundle';
import {prepareKadavrReportStateWithDefaultState} from '@self/project/src/spec/hermione/fixtures/genericBundle';
import PromoBadge from '@self/root/src/components/PromoBadge/__pageObject';

export default makeSuite('Подарок', {
    environment: 'kadavr',
    story: prepareSuite(GenericBundleTermSuite, {
        pageObjects: {
            promoBadge() {
                return this.createPageObject(PromoBadge, {
                    parent: this.snippetCard2,
                });
            },
        },
        meta: {
            id: 'marketfront-4267',
        },
        hooks: {
            async beforeEach() {
                const {
                    stateWithProductOffers,
                    primary,
                } = prepareKadavrReportStateWithDefaultState();

                await this.browser.setState('report', stateWithProductOffers);
                await this.browser.setState('Carter.items', []);

                return this.browser.yaOpenPage('market:list', {
                    nid: primary.offerMock.navnodes[0].id,
                    slug: primary.offerMock.navnodes[0].slug,
                    viewtype: 'list',
                });
            },
        },
    }),
});
