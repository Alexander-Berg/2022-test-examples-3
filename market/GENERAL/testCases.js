import {screen} from '@testing-library/dom';

// PageObjects
import ProductOffersResultsPO from '@self/platform/widgets/content/productOffers/Results/__pageObject';
import OfferSnippetPO from '@self/platform/components/ProductOffers/Snippet/Offer/__pageObject';
import PricePO from '@self/platform/components/Price/__pageObject';

// constants
import {NON_BREAKING_SPACE_CHAR as NBSP} from '@self/root/src/constants/string';

// mocks
import offerMock from './offer';

const widgetPath = '@self/platform/widgets/content/productOffers/Results';

async function initContext(mandrelLayer) {
    await mandrelLayer.initContext();
}

export const showDirectDiscountBadge = async (jestLayer, apiaryLayer, mandrelLayer, params) => {
    await initContext(mandrelLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, params);
    const root = container.querySelector(ProductOffersResultsPO.root);
    expect(root).not.toBeNull();

    const offerSnippet = container.querySelector(OfferSnippetPO.root);
    expect(offerSnippet).toBeTruthy();

    const price = offerSnippet.querySelector(PricePO.price);
    const discountBadge = offerSnippet.querySelector(PricePO.discountBadge);
    const discountBadgeText = offerSnippet.querySelector(PricePO.discountBadgeText);
    const discountPrice = offerSnippet.querySelector(PricePO.discountPrice);

    expect(price).toBeTruthy();
    expect(price.textContent).toBe(`103${NBSP}₽`);
    expect(discountBadge).toBeTruthy();
    expect(discountBadge.textContent).toBe('Скидка:‒24%');
    expect(discountBadgeText.textContent).toBe('‒24%');
    expect(discountPrice.textContent).toBe(`136${NBSP}₽`);
};

export const showSnippetMainInfo = async (jestLayer, apiaryLayer, mandrelLayer, params) => {
    await initContext(mandrelLayer);
    const deliveryInfoText = 'Доставка продавца';
    const offer = offerMock.search.results[0];

    const {container} = await apiaryLayer.mountWidget(widgetPath, params);
    const offerSnippet = container.querySelector(OfferSnippetPO.root);
    expect(offerSnippet).toBeTruthy();

    const price = offerSnippet.querySelector(PricePO.price);
    const gradesCount = screen.getByText(`${offer.shop.newGradesCount} отзывов`);
    const title = screen.getByText(offer.titles.raw);
    const shopName = screen.getByRole('link', {name: offer.shop.name});
    const image = screen.getByRole('img', {name: /изображение товара/i});
    const deliveryInfo = screen.getByText(deliveryInfoText, {exact: false});

    expect(image).toBeVisible();

    expect(price.textContent).toContain(`${offer.prices.value}${NBSP}₽`);
    expect(shopName.textContent).toContain(offer.shop.name);
    expect(title.textContent).toContain(offer.titles.raw);
    expect(gradesCount.textContent).toContain(offer.shop.newGradesCount);

    expect(deliveryInfo.textContent).toContain(deliveryInfoText);
};
