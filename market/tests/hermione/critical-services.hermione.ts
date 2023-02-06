import {expect} from 'chai';

import {getServicePageUrl, MARKET_MAP_INDEX_URL} from '../../../../tsum-front-core/configs/routes';
import NodeCriticalSwitchPO from '../../src/features/market-map/components/NodeCriticalSwitch/__pageObject';
import ServicesTablePO from '../../src/features/market-map/containers/ServicesTable/__pageObject';
import { EXISTING_COMPONENT_DATA } from './constants';

describe.skip('Критичность сервисов', () => {
    it('Проставление критичности сервиса и поиск', async function () {
        const nodeCriticalSwitch = new NodeCriticalSwitchPO(this.browser);
        const servicesTable = new ServicesTablePO(this.browser);
        const {slug} = EXISTING_COMPONENT_DATA;
        const existingServicePage = getServicePageUrl(slug);

        await this.browser.loginAndRedirect(existingServicePage);
        await nodeCriticalSwitch.waitForVisible();

        const isCriticalSwitchActive = await nodeCriticalSwitch.getIsActive();

        if (!isCriticalSwitchActive) {
            await nodeCriticalSwitch.toggle();
        }

        await this.browser.url(MARKET_MAP_INDEX_URL);

        await nodeCriticalSwitch.toggle();
        await this.browser.pause(200);

        const criticalTagElement = await servicesTable.getServiceElementCriticalTag(slug);
        const criticalTagElementVisible = await criticalTagElement.isDisplayed();

        expect(criticalTagElementVisible).to.equal(true);
    });
});
