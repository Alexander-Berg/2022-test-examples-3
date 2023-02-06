import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import GenericBundleTermSuite from '@self/platform/spec/hermione/test-suites/blocks/GenericBundle';
import {prepareKadavrReportStateWithDefaultState} from '@self/project/src/spec/hermione/fixtures/genericBundle';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import PromoBadge from '@self/root/src/components/PromoBadge/__pageObject';
import SnippetList from '@self/platform/widgets/content/search/ResultsPaged/components/ResultsPaged/__pageObject';
import SnippetCard from '@self/project/src/components/Search/Snippet/Card/__pageObject';

export default makeSuite('Подарок', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(GenericBundleTermSuite, {
            pageObjects: {
                snippetList() {
                    return this.createPageObject(SnippetList);
                },
                snippetCard() {
                    return this.createPageObject(SnippetCard, {
                        parent: this.snippetList,
                        root: `${SnippetCard.root}:nth-of-type(1)`,
                    });
                },
                promoBadge() {
                    return this.createPageObject(PromoBadge, {
                        parent: this.snippetCard,
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
                    } = prepareKadavrReportStateWithDefaultState();

                    await this.browser.setState('Carter.items', []);
                    await this.browser.setState('report', stateWithProductOffers);

                    return this.browser.yaOpenPage('market:catalog', routes.list.phones);
                },
            },
        })
    ),
});
