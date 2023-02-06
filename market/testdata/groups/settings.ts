import {getLinkItem, getGroupGetter} from '../../../../utils/mocks';

// group getter
export const getSettingsGroup = getGroupGetter('common.sidebar.unified:group.settings');

// links
export const businessSettings = getLinkItem(
    'common.sidebar.unified:business-settings',
    'market-partner:html:business-settings:get',
    {
        ignorePlatformParams: true,
        urlParamsMap: {
            businessId: 'businessId',
        },
    },
);
export const businessAccess = getLinkItem(
    'common.sidebar.unified:business-access',
    'market-partner:html:business-access:get',
    {
        ignorePlatformParams: true,
        urlParamsMap: {
            businessId: 'businessId',
        },
    },
);
export const commonInfo = getLinkItem('common.sidebar.unified:common-info', 'market-partner:html:common-info:get');
export const organizationInfo = getLinkItem(
    'common.sidebar.unified:organization-info',
    'market-partner:html:organization-info:get',
);
export const supplierJurInfo = getLinkItem(
    'common.sidebar.unified:organization-info',
    'market-partner:html:supplier-jur-info:get',
);
export const deliveryOptions = getLinkItem(
    'common.sidebar.unified:delivery-options',
    'market-partner:html:delivery-options:get',
);
export const placementSettings = getLinkItem(
    'common.sidebar.unified:placement-settings',
    'market-partner:html:placement-settings:get',
);
export const processingMethods = getLinkItem(
    'common.sidebar.unified:placement-settings',
    'market-partner:html:processing-methods:get',
);
export const apiSettings = getLinkItem('common.sidebar.unified:api-settings', 'market-partner:html:api-settings:get');
export const apiLog = getLinkItem('common.sidebar.unified:api-log', 'market-partner:html:api-log:get');
export const sandboxNew = getLinkItem('common.sidebar.unified:sandbox', 'market-partner:html:sandbox-new:get');
export const selfCheck = getLinkItem('common.sidebar.unified:self-check', 'market-partner:html:self-check:get');
export const platformOutlets = getLinkItem('common.sidebar.unified:outlets', 'market-partner:html:outlets:get');

export const integrations = getLinkItem('common.sidebar.unified:integrations', 'market-partner:html:integrations:get');

export const notificationSettings = getLinkItem(
    'common.sidebar.unified:notification-settings',
    'market-partner:html:notification-settings:get',
    {
        ignorePlatformParams: true,
        urlParamsMap: {
            businessId: 'businessId',
        },
    },
);

export const transfer = getLinkItem('common.sidebar.unified:transfer', 'market-partner:html:transfer:get', {
    ignorePlatformParams: true,
});
