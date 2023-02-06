import {getLinkItem, getGroupGetter} from '../../../../utils/mocks';

// group getter
export const getReportsGroup = getGroupGetter('common.sidebar.unified:group.reports');

// links
export const reportModelClicks = getLinkItem(
    'common.sidebar.shop:report-model-clicks',
    'market-partner:html:report-model-clicks:get',
);
export const statPlacement = getLinkItem(
    'common.sidebar.shop:stat-placement',
    'market-partner:html:stat-placement:get',
);
export const shopsStatDiscounts = getLinkItem(
    'common.sidebar.shop:shops-stat-discounts',
    'market-partner:html:shops-stat-discounts:get',
);
export const competitorPrices = getLinkItem(
    'common.sidebar.unified:competitor-prices',
    'market-partner:html:competitor-prices:get',
);
export const marketAnalyticsLanding = getLinkItem(
    'common.sidebar.unified:market-analytics-landing',
    'market-partner:html:market-analytics-landing:get',
);
export const marketAnalyticsOpenDemand = getLinkItem(
    'common.sidebar.shop:analytic-tools.open-demand',
    'market-partner:html:analytic-tools.open-demand:get',
);
export const fulfillmentStatSales = getLinkItem(
    'common.sidebar.unified:stat-sales',
    'market-partner:html:fulfillment-stat-sales:get',
);
export const fulfillmentStatCrossdoc = getLinkItem(
    'common.sidebar.unified:stat-crossdoc',
    'market-partner:html:fulfillment-stat-crossdoc:get',
);
export const returnsReport = getLinkItem('common.sidebar.unified:returns', 'market-partner:html:returns-report:get');
export const salesStatistic = getLinkItem(
    'common.sidebar.unified:sales-statistics',
    'market-partner:html:supplier-sales-statistics:get',
);

// Total table
export const totalTable = getLinkItem(
    'common.sidebar.shop:total-table-report',
    'market-partner:html:total-table-report:get',
);

// Total table for business
export const totalTableBusiness = getLinkItem(
    'common.sidebar.shop:total-table-report',
    'market-partner:html:business-total-table-report:get',
    {
        ignorePlatformParams: true,
        urlParamsMap: {
            businessId: 'businessId',
        },
    },
);

// Market category sales
export const marketCategorySales = getLinkItem(
    'common.sidebar.shop:market-category-sales-report',
    'market-partner:html:market-category-sales:get-report',
);
