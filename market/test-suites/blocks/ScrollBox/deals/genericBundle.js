import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import {prepareKadavrReportStateWithDefaultState} from '@self/project/src/spec/hermione/fixtures/genericBundle';
import GenericBundleSuite from '@self/platform/spec/hermione/test-suites/blocks/GenericBundle';
// page-objects
import PromoBadge from '@self/root/src/components/PromoBadge/__pageObject';

// mocks
import indexPageMock from '../fixtures/index-page';


export default makeSuite('Подарок', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(GenericBundleSuite, {
            pageObjects: {
                promoBadge() {
                    return this.createPageObject(PromoBadge);
                },
            },
            meta: {
                id: 'marketfront-4273',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        'Tarantino.data.result',
                        [indexPageMock]
                    );

                    const {stateWithProductOffers} = prepareKadavrReportStateWithDefaultState();
                    await this.browser.setState('report', stateWithProductOffers);

                    await this.browser.yaOpenPage('touch:index');
                },
            },
        })
    ),
});
