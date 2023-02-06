import {getLinkItem, getGroupGetter} from '../../../../utils/mocks';

// group getter
const getTurboGroup = getGroupGetter('common.sidebar.unified:group.orders');

// links
export const turboServiceSettings = getLinkItem(
    'common.sidebar.shop:turbo-service-settings',
    'market-partner:html:turbo-service-settings:get',
);
export const turboServiceNavigation = getLinkItem(
    'common.sidebar.shop:turbo-service-navigation',
    'market-partner:html:turbo-service-navigation:get',
);
export const turboServiceAnalytics = getLinkItem(
    'common.sidebar.shop:turbo-service-analytics',
    'market-partner:html:turbo-service-analytics:get',
);
export const turboServiceUserAgreement = getLinkItem(
    'common.sidebar.shop:turbo-service-user-agreement',
    'market-partner:html:turbo-service-user-agreement:get',
);

// group
export const turboGroup = getTurboGroup([
    turboServiceSettings,
    turboServiceNavigation,
    turboServiceAnalytics,
    turboServiceUserAgreement,
]);
