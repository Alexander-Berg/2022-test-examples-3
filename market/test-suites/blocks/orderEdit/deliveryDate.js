import dayjs from 'dayjs';
import customParseFormat from 'dayjs/plugin/customParseFormat';
import 'dayjs/locale/ru';
import {makeCase, makeSuite} from 'ginny';
import {capitalize} from 'ambar';

import {formatDeliveryDates, formatDeliveryTime, trimSeconds} from '@self/root/src/utils/datetime';

import {ORDER_STATUS} from '@self/root/src/entities/order';
import {ORDER_CHANGE_AVAILABILITY, ORDER_CHANGE_TYPES, ORDER_CHANGE_METHODS} from '@self/root/src/entities/orderEditPossibility';
import {DELIVERY_TYPE} from '@self/root/src/entities/delivery';
import OrderEditDeliveryDate
    from '@self/root/src/widgets/content/orderEdit/OrderEditDeliveryDate/components/View/__pageObject';
import DateIntervalSelect
    from '@self/root/src/widgets/content/orderEdit/OrderEditDeliveryDate/components/DateIntervalSelect/__pageObject';
import TimeIntervalSelect
    from '@self/root/src/widgets/content/orderEdit/OrderEditDeliveryDate/components/TimeIntervalSelect/__pageObject';
import OrderNotifier
    from '@self/root/src/components/OrderNotifier/__pageObject';
import OrderNotifierTitle
    from '@self/root/src/components/OrderNotifier/components/NotifierTitle/__pageObject';
import OrderNotifierContent
    from '@self/root/src/components/OrderNotifier/components/NotifierContent/__pageObject';
import {
    Spin,
} from '@self/root/src/uikit/components/Spin/__pageObject';
import {
    SelectButton,
    SelectPopover,
} from '@self/root/src/components/Select/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import {CASE_ID, TEXT} from './constants';

dayjs.extend(customParseFormat);
dayjs.locale('ru');

const INTERVALS = [
    {fromTime: '10:00:00', toTime: '10:01:00'},
    {fromTime: '10:01:00', toTime: '10:02:00'},
];

const ORDER_ID = 42;

export default makeSuite('Изменение сроков доставки.', {
    feature: 'Изменение сроков доставки',
    story: {
        async beforeEach() {
            this.setPageObjects({
                orderEditDeliveryDate: () => this.createPageObject(OrderEditDeliveryDate),
                orderNotifier: () => this.createPageObject(OrderNotifier),
                orderNotifierTitle: () => this.createPageObject(OrderNotifierTitle, {
                    parent: this.orderNotifier,
                }),
                orderNotifierContent: () => this.createPageObject(OrderNotifierContent, {
                    parent: this.orderNotifier,
                }),
                preloader: () => this.createPageObject(Spin, {
                    parent: this.orderEditDeliveryDate,
                }),

                dateIntervalSelectContainer: () => this.createPageObject(DateIntervalSelect, {
                    parent: this.orderEditDeliveryDate,
                }),
                dateIntervalSelectButton: () => this.createPageObject(SelectButton, {
                    parent: this.dateIntervalSelectContainer,
                }),
                // для popup не указывем parent, так как в DOM он рендерится через портал
                dateIntervalSelectOptionsPopup: () => this.createPageObject(SelectPopover),

                timeIntervalSelectContainer: () => this.createPageObject(TimeIntervalSelect, {
                    parent: this.orderEditDeliveryDate,
                }),
                timeIntervalSelectButton: () => this.createPageObject(SelectButton, {
                    parent: this.timeIntervalSelectContainer,
                }),
                // для popup не указывем parent, так как в DOM он рендерится через портал
                timeIntervalSelectOptionsPopup: () => this.createPageObject(SelectPopover),
            });

            const orderId = ORDER_ID;
            const deliveryId = 43;
            await this.browser.setState('Checkouter.collections', {
                order: {
                    [orderId]: {
                        id: orderId,
                        status: ORDER_STATUS.PROCESSING,
                        delivery: deliveryId,
                    },
                },
                delivery: {
                    [deliveryId]: {
                        type: DELIVERY_TYPE.DELIVERY,
                        dates: {
                            fromDate: getDateByDaysFromNow(0),
                            toDate: getDateByDaysFromNow(3),
                        },
                    },
                },
                orderEditPossibilities: {
                    [orderId]: [{
                        method: ORDER_CHANGE_METHODS.PARTNER_API,
                        type: ORDER_CHANGE_TYPES.DELIVERY_DATES,
                        availability: ORDER_CHANGE_AVAILABILITY.ENABLED,
                    }],
                },
                orderEditOptions: {
                    [orderId]: {
                        deliveryOptions: [
                            {
                                deliveryServiceId: 1,
                                fromDate: getDateByDaysFromNow(0),
                                toDate: getDateByDaysFromNow(1),
                                timeIntervalOptions: INTERVALS,
                            },
                        ],
                    },
                },
            });
        },

        'Автооткрытие попапа с изменением срока доставки': makeCase({
            id: 'bluemarket-3659',
            issue: 'BLUEMARKET-15067',
            async test() {
                const shouldBeVisible = this.params.pageId === PAGE_IDS_COMMON.ORDER;
                await this.browser.yaOpenPage(this.params.pageId, {
                    orderId: ORDER_ID,
                }, undefined, 'changeDateTime');


                return this.allure.runStep(`Автооткрытие попапа ${shouldBeVisible ? '' : 'не'} происходит`, () => (
                    this.orderEditDeliveryDate.waitForVisible(1000, !shouldBeVisible)
                ));
            },
        }),

        'Запрос на изменение сроков доставки сохраняется.': makeCase({
            id: CASE_ID,
            issue: 'BLUEMARKET-8690',

            async test() {
                await this.browser.yaOpenPage(this.params.pageId, {
                    orderId: ORDER_ID,
                });

                const selectedDeliveryDate = prepareText(formatDeliveryDates({
                    fromDate: getDateByDaysFromNow(0),
                    toDate: getDateByDaysFromNow(3),
                }));
                const anotherDeliveryDate = prepareText(formatDeliveryDates({
                    fromDate: getDateByDaysFromNow(0),
                    toDate: getDateByDaysFromNow(1),
                }));

                const firstTimeInterval = prepareText(formatDeliveryTime(
                    trimTimeInterval(INTERVALS[0])
                ));
                const secondTimeInterval = prepareText(formatDeliveryTime(
                    trimTimeInterval(INTERVALS[1])
                ));

                await this.changeDeliveryDateLink.getText()
                    .should.eventually.equal(
                        TEXT,
                        `Присутствует ссылка на изменение даты доставки с текстом “${TEXT}”`
                    );

                await this.changeDeliveryDateLink.click();
                await this.orderEditDeliveryDate.waitForVisible(500);
                await this.preloader.waitForHidden(5000);

                await this.browser.allure.runStep('Кликаем по селектору выбора даты', () =>
                    this.dateIntervalSelectButton.click()
                );
                await this.dateIntervalSelectOptionsPopup.options.getText()
                    .should.eventually.eql([
                        anotherDeliveryDate,
                        selectedDeliveryDate,
                    ]);
                await this.browser.allure.runStep('Выбираем другую дату', () =>
                    this.dateIntervalSelectOptionsPopup.clickOptionByText(anotherDeliveryDate)
                );
                await this.browser.allure.runStep('Ждем применения даты', () =>
                    this.browser.waitUntil(
                        async () => !(await this.dateIntervalSelectOptionsPopup.isVisible()),
                        5000
                    )
                );

                await this.browser.allure.runStep('Кликаем по селектору выбора интервала', () =>
                    this.timeIntervalSelectButton.click()
                );
                await this.timeIntervalSelectOptionsPopup.options.getText()
                    .should.eventually.eql([
                        firstTimeInterval,
                        secondTimeInterval,
                    ]);
                await this.browser.allure.runStep('Выбираем интервал', () =>
                    this.timeIntervalSelectOptionsPopup.clickOptionByText(secondTimeInterval)
                );
                await this.browser.allure.runStep('Ждем применения интервала', () =>
                    this.browser.waitUntil(
                        async () => !(await this.timeIntervalSelectOptionsPopup.isVisible()),
                        5000
                    )
                );

                await this.orderEditDeliveryDate.clickSave();
                await this.orderEditDeliveryDate.clickComplete();

                await this.orderNotifier.isVisible()
                    .should.eventually.equal(true, 'Должно появится уведомление о переносе даты доставки');
                await this.orderNotifierTitle.root.getText()
                    .should.eventually.equal('Переносим дату доставки');
                await this.orderNotifierContent.root.getText()
                    .should.eventually.match(
                        new RegExp(`${anotherDeliveryDate}.*${secondTimeInterval}`, 'i'),
                        'В тексте уведомления должны быть правильные дата и интервал'
                    );
            },
        }),
    },
});

const getDateByDaysFromNow = dayDiff =>
    dayjs()
        .add(dayDiff, 'day')
        .format('DD-MM-YYYY');

const prepareText = text => {
    const textWithoutNbsp = text.replace(/\u00a0/g, ' ');
    return capitalize(textWithoutNbsp);
};

const trimTimeInterval = interval => ({
    fromTime: trimSeconds(interval.fromTime),
    toTime: trimSeconds(interval.toTime),
});

