import {screen} from '@testing-library/dom';

import {orderConfirmationResolversMockConstructor} from '../common/mockFunctionality';
import {ORDER_ID, MOCKS_DATA} from './mocks';

const widgetPath = '@self/root/src/widgets/parts/OrderConfirmation';

const ESTIMATED_DELIVERY_TEXT_REGEX = /Ориентировочная дата доставки — во? .*?, \d{1,2} [а-я]+. Продавец согласует с вами точную дату и время\./;
const TIME_UNTIL_EXPIRATION_TEXT_REGEX = /Отменить заказ можно до \d{1,2} [а-я]+ включительно\./;
const CANCELATION_DELIVERY_TEXT_REGEX = /Доставка продавца в четверг, 16 июня Отменить заказ можно до \d{1,2} [а-я]+ включительно\./;

async function makeContext(mandrelLayer, {cookies = {}, exps = {}, user = {}}) {
    const UID = '9876543210';
    const yandexuid = '1234567890';

    const cookie = {
        ...cookies,
    };

    return mandrelLayer.initContext({
        user: {
            UID,
            yandexuid,
            ...user,
        },
        request: {
            cookie,
            abt: {
                expFlags: exps || {},
            },
        },
    });
}

export const containsUniqueOffers = async (jestLayer, apiaryLayer, mandrelLayer) => {
    await makeContext(mandrelLayer, {});
    await jestLayer.backend.runCode(orderConfirmationResolversMockConstructor, [MOCKS_DATA.order_containing_unique_offers]);
    await apiaryLayer.mountWidget(widgetPath, {
        orderIds: [ORDER_ID],
        isCertificateCheckout: false,
    });

    await step('Отображается сообщение об ориентировочной дате доставки', async () => {
        expect(screen.getByTestId('estimated-delivery-status')).toHaveTextContent(ESTIMATED_DELIVERY_TEXT_REGEX);
    });

    await step('Отображается сообщение о возможности отменить доставку в установленный срок', async () => {
        expect(screen.getByTestId('estimated-delivery-status')).toHaveTextContent(TIME_UNTIL_EXPIRATION_TEXT_REGEX);
    });
};

export const containsEstimatedOffers = async (jestLayer, apiaryLayer, mandrelLayer) => {
    await makeContext(mandrelLayer, {});
    await jestLayer.backend.runCode(orderConfirmationResolversMockConstructor, [MOCKS_DATA.order_containing_estimated_offers]);
    await apiaryLayer.mountWidget(widgetPath, {
        orderIds: [ORDER_ID],
        isCertificateCheckout: false,
    });

    await step('Отображается сообщение об ориентировочной дате доставки', async () => {
        expect(screen.getByTestId('estimated-delivery-status')).toHaveTextContent(ESTIMATED_DELIVERY_TEXT_REGEX);
    });
};

export const containsCancelationOffers = async (jestLayer, apiaryLayer, mandrelLayer) => {
    await makeContext(mandrelLayer, {});
    await jestLayer.backend.runCode(orderConfirmationResolversMockConstructor, [MOCKS_DATA.order_containing_cancelation_offers]);
    await apiaryLayer.mountWidget(widgetPath, {
        orderIds: [ORDER_ID],
        isCertificateCheckout: false,
    });

    await step('Отображается сообщение о возможности отмены', async () => {
        expect(screen.getByTestId('order-delivery')).toHaveTextContent(CANCELATION_DELIVERY_TEXT_REGEX);
    });
};
