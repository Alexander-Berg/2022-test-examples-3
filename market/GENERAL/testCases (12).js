import GroupedParcels
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcels/components/View/__pageObject';

export const hasGroupExpectedTitle = async (widgetContainer, {value, tag}) => {
    const editableCard = widgetContainer.querySelector(GroupedParcels.title);
    await step(`текст заголовка ${value}`, () => expect(editableCard.textContent).toContain(value));
    await step(`тег в заголовке ${tag}`, () => expect(editableCard.tagName).toBe(tag));
};
