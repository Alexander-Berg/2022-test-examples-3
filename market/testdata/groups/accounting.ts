import {getLinkItem, getGroupGetter} from '../../../../utils/mocks';

// group getter
export const getAccountingGroup = getGroupGetter('common.sidebar.unified:group.accounting');

// links
export const accountDocuments = getLinkItem(
    'common.sidebar.unified:account-documents',
    'market-partner:html:account-documents:get',
);
export const paymentsReport = getLinkItem(
    'common.sidebar.unified:payments',
    'market-partner:html:business-payments-report:get',
);
export const businessPaymentsReport = getLinkItem(
    'common.sidebar.unified:payments',
    'market-partner:html:business-payments-report:get',
    {
        ignorePlatformParams: true,
        urlParamsMap: {
            businessId: 'businessId',
        },
    },
);
export const businessBillingReport = getLinkItem(
    'common.sidebar.unified:stat-billing-orders',
    'market-partner:html:business-billing-report:gets',
);
export const salesDocs = getLinkItem('common.sidebar.unified:sales-documents', 'market-partner:html:sales-docs:get');

export const payoutsFrequency = getLinkItem(
    'common.sidebar.unified:business-payouts-frequency',
    'market-partner:html:business-payouts-frequency:get',
    {
        ignorePlatformParams: true,
        urlParamsMap: {
            businessId: 'businessId',
        },
    },
);
