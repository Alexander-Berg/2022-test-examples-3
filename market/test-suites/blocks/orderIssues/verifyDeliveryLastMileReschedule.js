import assert from 'assert';
import {
    makeCase,
    makeSuite,
} from 'ginny';
import {
    mergeState,
    createOffer,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {generateRandomId} from '@self/root/src/spec/utils/randomData';

import VerifyDeliveryLastMileReschedulePopup
    from '@self/root/src/widgets/content/orderIssues/VerifyDeliveryLastMileReschedulePopup/__pageObjects';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {ORDER_AGITATION_TYPE} from '@self/root/src/constants/orderAgitation';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as sock from '@self/root/src/spec/hermione/kadavr-mock/report/sock';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {prepareNotificationAgitation} from '@self/root/src/spec/hermione/scenarios/persAuthorResource';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import {NON_BREAKING_SPACE_CHAR as NBSP} from '@self/root/src/constants/string';
import {formatDeliveryDates} from '@self/root/src/utils/datetime';
import {formatDate} from '@self/root/src/spec/utils/formatDate';
import {MSEC_IN_DAY} from '@self/root/src/constants/ttl';

const RESULT_TYPES = {
    me: {
        header: 'Спасибо, что ответили',
        description: 'Пусть покупка вас порадует.',
    },
    notMeWithReschedule: {
        header: 'Перенести доставку?',
        description: `${getDateInterval()}.`,
    },
    notMeWithoutReschedule: {
        header: 'Спасибо, что ответили',
        description: `Заказ доставят ${getDateInterval()}. Разберёмся, почему служба доставки изменила дату.`,
    },
};

const TOTAL_OFFERS = 2;

export default makeSuite('Попап подтверждения переноса доставки службой доставки', {
    environment: 'kadavr',
    feature: 'Подтверждение переноса доставки службой доставки',
    issue: 'MARKETFRONT-50169',
    defaultParams: {
        isAuthWithPlugin: true,
        isAuth: true,
    },
    story: {
        async beforeEach() {
            assert(
                this.params.pageId,
                'Param pageId must be defined in order to run this suite'
            );

            this.setPageObjects({
                verifyDeliveryLastMileReschedulePopup: () =>
                    this.createPageObject(VerifyDeliveryLastMileReschedulePopup),
            });

            this.yaTestData = this.yaTestData || {};
            this.yaTestData.currentOrderId = await prepareState.call(this);
        },
        'При клике': {
            'на крестик': {
                'должен закрыться': makeCase({
                    id: 'bluemarket-4094',
                    async test() {
                        await navigate.call(this, this.yaTestData.currentOrderId);

                        await checkAgitationCompleted.call(this, () =>
                            this.verifyDeliveryLastMileReschedulePopup.clickCloser());

                        return this.verifyDeliveryLastMileReschedulePopup.isPopupVisible()
                            .should.eventually.to.be.equal(
                                false,
                                'Попап подтверждения отмены не отображается'
                            );
                    },
                }),
            },
            'на подтверждение': {
                'должен отобразить результат подтверждения': makeCase({
                    id: 'bluemarket-4095',
                    async test() {
                        await navigate.call(this, this.yaTestData.currentOrderId);
                        await checkAgitationCompleted.call(this, () =>
                            this.verifyDeliveryLastMileReschedulePopup.clickAcceptButton());
                        await checkResultsPopup.call(this, RESULT_TYPES.me);
                        await this.verifyDeliveryLastMileReschedulePopup.clickHideButton();
                        await this.verifyDeliveryLastMileReschedulePopup.isPopupVisible()
                            .should.eventually.to.be.equal(
                                false,
                                'Попап подтверждения отмены не отображается'
                            );
                    },
                }),
            },
        },
    },
});

function getDateInterval() {
    return formatDeliveryDates({
        fromDate: formatDate(),
        // Сегодня + 2 дня
        toDate: formatDate(new Date(Date.now() + (MSEC_IN_DAY * 2))),
    }, true)
        .split(NBSP)
        .join(' ');
}

async function prepareOrderEditOptionsState({orderId, isEmpty = true}) {
    return this.browser.setState('Checkouter.collections.orderEditOptions', {
        [orderId]: {
            orderId,
            deliveryOptions: isEmpty
                ? []
                : [
                    {
                        deliveryServiceId: 1,
                        fromDate: formatDate(),
                        toDate: formatDate(new Date(Date.now() + (MSEC_IN_DAY * 2))),
                    },
                ],
        },
    });
}

async function navigate(orderId) {
    if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
        await this.browser.yaOpenPage(this.params.pageId, {orderId});
    } else {
        await this.browser.yaOpenPage(this.params.pageId, {mock: 1});
        await this.verifyDeliveryLastMileReschedulePopup.waitForVisibleRoot();
    }
    return orderId;
}

async function prepareState(orderId = generateRandomId()) {
    const {browser} = this;

    await browser.yaScenario(
        this,
        prepareOrder,
        {
            region: this.params.region,
            orders: [{
                orderId,
                items: [{
                    wareMd5: kettle.offerMock.wareId,
                    buyerPrice: 200,
                    count: 5,
                }, {
                    wareMd5: sock.offerMock.wareId,
                    buyerPrice: 200,
                    count: 8,
                }],
                deliveryType: 'DELIVERY',
            }],
            paymentType: 'PREPAID',
            paymentMethod: 'YANDEX',
            status: 'PROCESSING',
        }
    );

    const reportState = mergeState([
        createOffer(kettle.offerMock, kettle.offerMock.wareId),
        createOffer(sock.offerMock, sock.offerMock.wareId),
        {
            data: {
                search: {
                    results: [
                        kettle.offerMock.wareId,
                        sock.offerMock.wareId,
                    ].map(id => ({schema: 'offer', id})),
                    totalOffers: TOTAL_OFFERS,
                    total: TOTAL_OFFERS,
                },
            },
        },
    ]);
    await this.browser.yaScenario(this, setReportState, {state: reportState});

    await prepareOrderEditOptionsState.call(this, {orderId});

    await this.browser.yaScenario(this, prepareNotificationAgitation, {
        agitations: [{
            type: ORDER_AGITATION_TYPE.ORDER_DELIVERY_DATE_CHANGED_BY_USER_ON_LAST_MILE,
            entityId: orderId,
        }],
    });

    return orderId;
}


async function checkResultsPopup({header, description}) {
    await this.verifyDeliveryLastMileReschedulePopup.areResultsVisible()
        .should.eventually.to.be.equal(
            true,
            'Экран результатов отображается'
        );

    const headerText = await this.verifyDeliveryLastMileReschedulePopup.getHeaderText();

    await this.expect(headerText)
        .to.be.equal(
            header,
            'Текст заголовка корректный'
        );

    const descriptionText = await this.verifyDeliveryLastMileReschedulePopup.getDescriptionText();

    await this.expect(descriptionText)
        .to.be.equal(
            description,
            'Текст заголовка корректный'
        );

    await this.verifyDeliveryLastMileReschedulePopup.isHideButtonVisible()
        .should.eventually.to.be.equal(
            true,
            'Кнопка "Закрыть" отображается'
        );
}

async function checkAgitationCompleted(action) {
    const log = await this.browser.yaWaitKadavrLogByBackendMethods(
        'PersAuthor', 'stopAgitationByIdAndUserId',
        action
    );

    return this.browser.allure.runStep(
        'Проверяем наличие запроса за завершением агитации',
        () => log.should.have.lengthOf(
            1,
            'В логе кадавра один запрос за завершением агитации'
        )
    );
}
