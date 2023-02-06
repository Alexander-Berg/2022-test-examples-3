import {getLinkItem, getGroupItem, getGroupGetter} from '../../../utils/mocks';

export const wizard = getLinkItem('common.sidebar.shop:order-wizard', 'market-partner:html:order-wizard:get');

export const onboarding = getLinkItem('common.sidebar.shop:onboarding', 'market-partner:html:onboarding:get');

export const dashboard = getLinkItem('common.sidebar.shop:shops-dashboard', 'market-partner:html:shops-dashboard:get');

// customers

export const getCustomersGroup = getGroupGetter('common.sidebar.shop:group.customers');

export const reviews = getLinkItem('common.sidebar.shop:shops-reviews', 'market-partner:html:shops-reviews:get');

export const questions = getLinkItem('common.sidebar.shop:questions', 'market-partner:html:questions:get');

export const customersGroup = getCustomersGroup([reviews, questions]);

// quality

export const quality = getLinkItem('common.sidebar.shop:quality', 'market-partner:html:quality:get');

export const integrations = getLinkItem('common.sidebar.unified:integrations', 'market-partner:html:integrations:get');

export const checksGroup = getGroupItem('common.sidebar.shop:group.checks', [quality]);

// stats

export const getStatsGroup = getGroupGetter('common.sidebar.shop:group.stats');

export const costsAndSales = getLinkItem(
    'common.sidebar.shop:costs-and-sales',
    'market-partner:html:costs-and-sales:get',
);

export const modelClicks = getLinkItem(
    'common.sidebar.shop:report-model-clicks',
    'market-partner:html:report-model-clicks:get',
);

export const statPlacement = getLinkItem(
    'common.sidebar.shop:stat-placement',
    'market-partner:html:stat-placement:get',
);

export const statDiscounts = getLinkItem(
    'common.sidebar.shop:shops-stat-discounts',
    'market-partner:html:shops-stat-discounts:get',
);

export const statDate = getLinkItem('common.sidebar.shop:shops-stat-date', 'market-partner:html:shops-stat-date:get');

export const statOperations = getLinkItem(
    'common.sidebar.shop:shops-stat-operations',
    'market-partner:html:shops-stat-operations:get',
);

export const statStocks = getLinkItem(
    'common.sidebar.shop:fulfillment-stat-stocks',
    'market-partner:html:fulfillment-stat-stocks:get',
);

export const statDaily = getLinkItem(
    'common.sidebar.shop:fulfillment-stat-daily',
    'market-partner:html:fulfillment-stat-daily:get',
);

export const statOrders = getLinkItem('common.sidebar.shop:stat-orders', 'market-partner:html:stat-orders:get');

export const ffStatOrders = getLinkItem(
    'common.sidebar.shop:fulfillment-stat-orders',
    'market-partner:html:fulfillment-stat-orders:get',
);

export const paymentsReport = getLinkItem(
    'common.sidebar.shop:payments-report',
    'market-partner:html:payments-report:get',
);

export const billingOrders = getLinkItem(
    'common.sidebar.shop:fulfillment-stat-billing-orders',
    'market-partner:html:fulfillment-stat-billing-orders:get',
);

export const salesStatistics = getLinkItem(
    'common.sidebar.supplier:supplier-sales-statistics',
    'market-partner:html:supplier-sales-statistics:get',
);

// assortment

export const getAssortmentGroup = getGroupGetter('common.sidebar.shop:group.assortment');

export const offerList = getLinkItem('common.sidebar.shop:offer-list', 'market-partner:html:shop-offer-list:get');

export const assortment = getLinkItem('common.sidebar.shop:assortment', 'market-partner:html:assortment:get');

export const priceList = getLinkItem('common.sidebar.shop:product-sources', 'market-partner:html:product-sources:get');

export const contentXls = getLinkItem('common.sidebar.shop:content-xls', 'market-partner:html:content-xls:get');

export const feedsList = getLinkItem('common.sidebar.shop:feeds-list', 'market-partner:html:feeds-list:get');

export const stocks = getLinkItem('common.sidebar.shop:stocks', 'market-partner:html:stocks:get');

export const indexingFeed = getLinkItem('common.sidebar.shop:indexing-feed', 'market-partner:html:shops-indexing:get');

export const hiddenOffers = getLinkItem('common.sidebar.shop:hidden-offers', 'market-partner:html:hidden-offers:get');

export const offers = getLinkItem('common.sidebar.shop:offers', 'market-partner:html:offers:get');

export const marketOffers = getLinkItem('common.sidebar.shop:market-offers', 'external:market-offers', {
    urlParams: {
        tld: 'tld',
    },
    urlParamsMap: {
        shopId: 'shopId',
    },
});

export const popular = getLinkItem(
    'common.sidebar.shop:popular-assortment',
    'market-partner:html:popular-assortment:get',
);

export const newModels = getLinkItem('common.sidebar.shop:new-models', 'market-partner:html:new-models:get');

export const disountsOffers = getLinkItem(
    'common.sidebar.shop:discount-offers',
    'market-partner:html:discount-offers:get',
);

export const competitorPrices = getLinkItem(
    'common.sidebar.shop:competitor-prices',
    'market-partner:html:competitor-prices:get',
);

export const pricelabsDeattachedCard = getLinkItem(
    'common.sidebar.shop:pricelabs-broker-deattached-card',
    'external:pricelabs-broker-deattached-card',
    {
        urlParams: {
            campaignId: 'id',
        },
    },
);

export const pricelabsDashboardAll = getLinkItem(
    'common.sidebar.shop:pricelabs-dashboard-all',
    'external:pricelabs-dashboard-all',
);

// bets

export const getBetsGroup = getGroupGetter('common.sidebar.shop:group.bets');

export const auction = getLinkItem('common.sidebar.shop:auction', 'market-partner:html:auction:get');

export const autostrategies = getLinkItem(
    'common.sidebar.shop:autostrategies',
    'market-partner:html:autostrategies:get',
);

export const businessCashback = getLinkItem(
    'common.sidebar.shop:business-cashback-redirect',
    'market-partner:html:business-cashback-redirect:get',
);

export const pricelabsBroker = getLinkItem('common.sidebar.shop:pricelabs-broker', 'external:pricelabs-broker');

export const betsGroup = getBetsGroup([auction, autostrategies, pricelabsBroker]);

// loyalty

export const loyalty = getLinkItem(
    'common.sidebar.supplier:fulfillment-loyalty',
    'market-partner:html:fulfillment-loyalty:get',
);

// promos

export const promos = getLinkItem(
    'common.sidebar.supplier:fulfillment-promos',
    'market-partner:html:fulfillment-promos:get',
);

// turbo settings

export const getTurboGroup = getGroupGetter('common.sidebar.shop:group.turbo');

export const turboServiceSettings = getLinkItem(
    'common.sidebar.shop:turbo-service-settings',
    'market-partner:html:turbo-service-settings:get',
);

export const turboServiceNavigation = getLinkItem(
    'common.sidebar.shop:turbo-service-navigation',
    'market-partner:html:turbo-service-navigation:get',
);

export const turboServiceUserAgreement = getLinkItem(
    'common.sidebar.shop:turbo-service-user-agreement',
    'market-partner:html:turbo-service-user-agreement:get',
);

// settings

export const getSettingsGroup = getGroupGetter('common.sidebar.shop:group.settings');

export const commonInfo = getLinkItem('common.sidebar.shop:common-info', 'market-partner:html:common-info:get');

export const orgInfo = getLinkItem(
    'common.sidebar.shop:organization-info',
    'market-partner:html:organization-info:get',
);

export const placementSettings = getLinkItem(
    'common.sidebar.shop:placement-settings',
    'market-partner:html:placement-settings:get',
);

export const yaPayReg = getLinkItem('common.sidebar.shop:ya-pay-reg', 'market-partner:html:supplier-jur-info:get');

export const deliveryOptions = getLinkItem(
    'common.sidebar.shop:delivery-options',
    'market-partner:html:delivery-options:get',
);

export const processingMethods = getLinkItem(
    'common.sidebar.shop:processing-methods',
    'market-partner:html:processing-methods:get',
);

export const apiSettings = getLinkItem('common.sidebar.shop:api-settings', 'market-partner:html:api-settings:get');

export const apiLog = getLinkItem('common.sidebar.shop:api-log', 'market-partner:html:api-log:get');

export const sandbox = getLinkItem('common.sidebar.shop:sandbox', 'market-partner:html:sandbox-new:get');

export const selfCheck = getLinkItem('common.sidebar.shop:self-check', 'market-partner:html:self-check:get');

export const credits = getLinkItem('common.sidebar.shop:credits', 'market-partner:html:credits:get');

export const priceLabsApiLogs = getLinkItem('common.sidebar.shop:pricelabs-log', 'external:pricelabs-log');

export const priceLabs = getLinkItem('common.sidebar.shop:PriceLabs', 'external:pricelabs-dashboard');

// orders

export const ordersList = getLinkItem('common.sidebar.shop:orders-list', 'market-partner:html:orders-list:get');

export const ordersGroup = getGroupItem('common.sidebar.shop:group.orders', [ordersList]);

// arbiter conversations

export const arbiterConversationList = getLinkItem(
    'common.sidebar.shop:arbiter-conversation-list',
    'market-partner:html:arbiter-conversations:get',
);

// pricelabs

export const pricelabsDashboard = getLinkItem('common.sidebar.shop:PriceLabs', 'external:pricelabs-dashboard', {
    urlParams: {
        campaignId: 'id',
    },
});

// accountDocuments

export const accountDocuments = getLinkItem(
    'common.sidebar.supplier:account-documents',
    'market-partner:html:account-documents:get',
);
