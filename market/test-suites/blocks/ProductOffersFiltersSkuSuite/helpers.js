import SnippetOffer from '@self/platform/components/ProductOffers/Snippet/Offer/__pageObject';

export const clickOnFilterAndWaitListUpdate = async ({filterColors, browser, snippetList, skuState}) => {
    const initialCardsLength = await snippetList.getSnippetCardsLength(SnippetOffer);
    await browser.setState('report', skuState);
    await filterColors.clickItemByIndex(2);
    await browser.waitUntil(
        async () => {
            const updatedCardsLength = await snippetList.getSnippetCardsLength(SnippetOffer);

            if (initialCardsLength !== updatedCardsLength) {
                return true;
            }
            return false;
        },
        10000,
        'Количество сниппетов не изменилось'
    );
};
