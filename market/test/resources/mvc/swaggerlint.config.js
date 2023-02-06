//Конфиг для линтера сваггера. Нужен для первых итераций.
//дальше либо съедем в ручку либо будем тянуть конфиг по урлу

module.exports = {
    rules: {
        'latin-definitions-only':['', {ignore: ['«', '»']}],
        'no-empty-object-type': true,
        'no-single-allof': true,
        'no-trailing-slash': true,
        'path-param-required-field': true,
        'required-operation-tags': true,
        'no-inline-enums': false,

        'object-prop-casing': [
            'camel',
            {
                ignore: [
                    'accessed-value',
                    'upload_id',
                    'shop-id',
                    'feed_id',
                    'recent-message',
                    'feature-name',
                    'feature-id',
                    'failed-precondition',
                    'can-enable',
                    'show-name',
                    'validation_id',
                    'delivery-cost',
                    'delivery-type',
                    'delivery-service-id',
                    'warehouse-id',
                    'region-to',
                    'region-from',
                ],
            },
        ],
        'parameter-casing': [
            'camel',
            {
                path: 'camel',
                query: 'snake',
                body: 'camel',
                ignore: [
                    'authorityName',
                    'billingServiceType',
                    'categoryId',
                    'categoryIds',
                    'checkerName',
                    'cmId',
                    'completeFeed',
                    'dateFrom',
                    'dateTo',
                    'deliveryServiceId',
                    'deliveryServices',
                    'feature-id',
                    'feedId',
                    'feedIds',
                    'ffShopId',
                    'from-date',
                    'group-type',
                    'hidingTimestamp',
                    'marketSku',
                    'offerId',
                    'order-asc',
                    'orderId',
                    'pageSize',
                    'pagingSpread',
                    'perpageNumber',
                    'regionGroupId',
                    'regionIds',
                    'registrationNumber',
                    'reportDate',
                    'reportId',
                    'reportType',
                    'searchString',
                    'serviceTypes',
                    'shipmentId',
                    'shopId',
                    'shopSku',
                    'shop_sku',
                    'supplierId',
                    'to-date',
                    'warehouseId',
                    'withHiddens',
                    'Юридическая информация',
                    'ADDED_INFO',
                    'paramTypes',
                    'entityId',
                    'userId',
                    'businessId'
                ],
            },
        ],

        'required-parameter-description': false, // 2 errors
        'only-valid-mime-types': false, // 259 errors
        'expressive-path-summary': false, // 101 errors
    },
    ignore: {
        definitions: ['StreamingResponseBody', 'JsonNode', 'LocalTime', 'OptionalInt', 'Cell', 'YearMonth', 'ReportMetaData',
            "AgencyCheckerParam«int»", "AgencyCheckerParam«double»", 'PromoDescriptionResp']
    }
};
