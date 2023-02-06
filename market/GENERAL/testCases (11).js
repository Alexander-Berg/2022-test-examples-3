import {screen, within} from '@testing-library/dom';

import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
import DeliverySupplier
    from '@self/root/src/components/Checkout/DeliverySupplier/__pageObject';
import ParcelWarning
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/ParcelWarning/__pageObject';
import LiftingToFloor from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/LiftingToFloor/__pageObject';

import {DeliveryIntervals} from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/DeliveryIntervals/__pageObject';

import {uniqueOrderMockParams} from '@self/root/src/widgets/content/checkout/common/__spec__/mockData/uniqueOrder';

import {TRYING_INFORMER_BUTTON_TEXT, WITH_TRYING, WITHOUT_TRYING} from '@self/root/src/constants/partialFashion';
import {emitBrowserEventActualizeSuccess} from './utils';

const ESTIMATED_DELIVERY_TITLE_REGEX = /^.*?(с \d{1,2} [а-я]+).*?$/;

export const hasExpectedTitle = async (widgetContainer, {value, tag}) => {
    const editableCard = screen.getByTestId('editableCardTitle');
    await step(`текст заголовка ${value}`, () => expect(editableCard.textContent).toContain(value));
    await step(`тег в заголовке ${tag}`, () => expect(editableCard.tagName).toBe(tag));
};

export const hasSelectedAddress = async (widgetContainer, selectedAddress) => {
    const address = widgetContainer.querySelector(AddressCard.root);
    await step(`выбранный адрес ${selectedAddress}`, () => expect(address.textContent).toContain(selectedAddress));
};

export const hasExpectedTryingText = async (widgetContainer, tryingText) => {
    const tryingInfo = screen.getByTestId('tryingInfo');
    await step(`текст примерки - ${tryingText}`, () => expect(tryingInfo.textContent).toBe(tryingText));
};

export const hasWithoutTryingIcon = async () => {
    const icon = screen.getByRole('img', {name: /без примерки/i});
    await step('проверяем видимость иконки', () => expect(icon).toBeInTheDocument());
};

export const hasTryingInformer = async () => {
    const tryingInformer = screen.queryByTestId('tryingInformer');
    await step('Отображается информер примерки.', () => expect(tryingInformer).toBeInTheDocument());

    if (process.env.PLATFORM !== 'touch') {
        const tryingBtn = within(tryingInformer).getByRole('button', {name: TRYING_INFORMER_BUTTON_TEXT});
        await step('На информере есть кнопка "Выбрать".', () => expect(tryingBtn).toBeInTheDocument());
    }
};

export const hasTryingPresetInfo = async presetName => {
    const button = screen.getByRole('button', {name: presetName});
    const tryingInfo = within(button).queryByTestId('tryingInfo');
    expect(tryingInfo.textContent).toBe(WITH_TRYING);
};

export const hasTryingPresetInfoTouch = addressId => {
    const popup = screen.queryByTestId('addressBottomDrawer');
    const address = within(popup).queryByTestId(addressId);
    const tryingInfo = within(address).queryByTestId('tryingInfo');
    expect(tryingInfo.textContent).toBe(WITH_TRYING);
};

export const hasWithoutTryingPresetInfo = async presetName => {
    const button = screen.getByRole('button', {name: presetName});
    const tryingInfo = within(button).queryByTestId('tryingInfo');
    const icon = within(button).getByRole('img', {name: /без примерки/i});
    expect(tryingInfo.textContent).toBe(WITHOUT_TRYING);
    expect(icon).toBeInTheDocument();
};

export const hasWithoutTryingPresetInfoTouch = addressId => {
    const popup = screen.queryByTestId('addressBottomDrawer');
    const address = within(popup).queryByTestId(addressId);
    const tryingInfo = within(address).queryByTestId('tryingInfo');
    const icon = within(popup).getByRole('img', {name: /без примерки/i});
    expect(tryingInfo.textContent).toBe(WITHOUT_TRYING);
    expect(icon).toBeInTheDocument();
};

export const hasSupplierDeliveryInfo = async (widgetContainer, delivery) => {
    const supplier = widgetContainer.querySelector(DeliverySupplier.root);
    await step(`доставка содержит ${delivery}`, () => expect(supplier.textContent).toContain(delivery));
};

export const hasExpectedWarning = async (widgetContainer, warningText) => {
    const parcelWarn = widgetContainer.querySelector(ParcelWarning.root);
    if (warningText) {
        await step(`предупреждение содержит текст ${warningText}`, () => expect(parcelWarn.textContent).toContain(warningText));
        return;
    }
    await step('не содержит предупреждение', () => expect(parcelWarn).toBeNull());
};

export const checkLiftSelectedParams = async (widgetContainer, price, prefix) => {
    // на таче нет checkbox, но есть content
    const liftToFloor = process.env.PLATFORM === 'touch' ? widgetContainer.querySelector(LiftingToFloor.content) : widgetContainer.querySelector(LiftingToFloor.checkbox);
    await step('проверяем что компонент отображается', () => expect(liftToFloor).not.toBeNull());
    if (price) {
        await step('проверяем что отображается верная цена', () => expect(liftToFloor.textContent).toContain(price));
    }
    await step('проверяем что отображается правильный префикс', () => expect(liftToFloor.textContent).toContain(prefix));
};

export const containsUniqueOffers = async widgetContainer => {
    await step('В тексте заголовка срок доставки указан в формате "... с D M"', async () => {
        emitBrowserEventActualizeSuccess();
        screen.getByRole('heading', {name: ESTIMATED_DELIVERY_TITLE_REGEX});
    });

    await step('Текст информационного сообщения соответствует формату', async () => {
        screen.getByRole('heading', {name: uniqueOrderMockParams.uniqueLabel});
    });

    const deliveryIntervals = widgetContainer.querySelector(DeliveryIntervals.root);

    await step('Отсутствует выбор даты и времени доставки', async () => {
        expect(deliveryIntervals).toBeNull();
    });
};

export const containsEstimatedOffers = async widgetContainer => {
    await step('В тексте заголовка срок доставки указан в формате "... с D M"', async () => {
        emitBrowserEventActualizeSuccess();
        screen.getByRole('heading', {name: ESTIMATED_DELIVERY_TITLE_REGEX});
    });

    await step('Текст информационного сообщения соответствует формату', async () => {
        screen.getByRole('heading', {name: uniqueOrderMockParams.longTermLabel});
    });

    const deliveryIntervals = widgetContainer.querySelector(DeliveryIntervals.root);

    await step('Отсутствует выбор даты и времени доставки', async () => {
        expect(deliveryIntervals).toBeNull();
    });
};

export const checkActiveTab = async deliveryType => {
    const heading = 'Мои способы доставки';
    const title = screen.getByRole('heading', {name: heading});
    await step(`Открылся попап "Мои способы доставки", активный таб - ${deliveryType}`, () => {
        expect(title.textContent).toBe(heading);

        if (deliveryType === 'delivery') {
            const courier = screen.getByRole('radio', {name: /курьер/i});
            expect(courier).toBeChecked();
        } else if (deliveryType === 'pickup') {
            const pickup = screen.getByRole('radio', {name: /самовывоз/i});
            expect(pickup).toBeChecked();
        }
    });
};
