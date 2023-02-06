import {getLinkItem} from '../../../utils/mocks';

export const wizard = getLinkItem('common.sidebar.unified:onboarding', 'market-partner:html:order-wizard:get');

export const onboarding = getLinkItem('common.sidebar.unified:onboarding', 'market-partner:html:onboarding:get');

export const dashboard = getLinkItem('common.sidebar.unified:dashboard', 'market-partner:html:shops-dashboard:get');

export const fulfillmentSummary = getLinkItem(
    'common.sidebar.unified:dashboard',
    'market-partner:html:fulfillment-summary:get',
);

export const quality = getLinkItem('common.sidebar.unified:quality', 'market-partner:html:quality:get');

export const partnerSupport = getLinkItem(
    'common.sidebar.unified:partner-support',
    'market-partner:html:partner-support:get',
);

export const priceLabsDashboard = getLinkItem('common.sidebar.shop:PriceLabs', 'external:pricelabs-dashboard', {
    urlParams: {
        campaignId: 'id',
    },
});
