export const baseMockFunctionality = ({
    page, result, collections, productResult, offer, offerShowPlace,
}) => {
    jest.spyOn(require('@self/root/src/resolvers/user'), 'resolveCurrentUserRegionSync')
        .mockReturnValue({
            result: page.regionId,
        });

    jest.spyOn(require('@self/project/src/resolvers/offer'), 'resolveDefaultOfferForOfferPage')
        .mockResolvedValue({
            result: offerShowPlace.id,
            collections,
        });

    jest.spyOn(require('@self/root/src/resolvers/financialProducts'), 'resolveFinancialProductsActive')
        .mockReturnValue(result.financialProductsActive);


    jest.spyOn(require('@self/project/src/entities/offer/selectors'), 'selectOfferCpaByVisibleSearchResultId')
        .mockResolvedValue({
            visibleSearchResultId: productResult.visibleSearchResultId,
            defaultOfferShowPlaceId: offerShowPlace.id,
            offer,
        });

    jest.spyOn(require('@self/root/src/resolvers/cartService/addItemsToCart'), 'addItemsToCart')
        .mockResolvedValue(null);
};

export const mockRouter = page => {
    const {mockRouterFabric} = require('@self/root/src/helpers/testament/mock');
    mockRouterFabric()({
        [page.id.checkout]: ({bnplConstructor, bnplSelected}) => (
            `${page.routes.checkout}?bnplConstructor=${bnplConstructor}&bnplSelected=${bnplSelected}`
        ),
    });
};

export const bnplMockFunctionality = bnplInfo => {
    jest.spyOn(require('@self/root/src/resolvers/bnpl'), 'resolveBnplPlan2')
        .mockResolvedValue({result: bnplInfo});
};

export const bnplErrorMockFunctionality = ({
    FINANCIAL_PRODUCTS_BNPL, financialProducts,
}) => {
    jest.spyOn(require('@self/root/src/resolvers/bnpl'), 'resolveBnplPlan2')
        .mockRejectedValue(null);

    jest.spyOn(require('@self/root/src/entities/financialProducts/utils'), 'getActiveFinancialProducts')
        .mockReturnValue([
            FINANCIAL_PRODUCTS_BNPL,
            ...financialProducts,
        ]);
};
