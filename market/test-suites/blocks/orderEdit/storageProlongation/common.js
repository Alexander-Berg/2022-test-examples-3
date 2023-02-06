import dayjs from 'dayjs';
import customParseFormat from 'dayjs/plugin/customParseFormat';
import 'dayjs/locale/ru';
import {mergeSuites, makeCase, makeSuite} from 'ginny';

import {ORDER_STATUS} from '@self/root/src/entities/order';
import {DELIVERY_TYPE} from '@self/root/src/entities/delivery';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import ProlongateLink
    from '@self/root/src/containers/Orders/ProlongateLink/__pageObject';
import ProlongationSuggestion
    // eslint-disable-next-line max-len
    from '@self/root/src/widgets/content/orderEdit/OrderStorageProlongation/components/ProlongationSuggestion/__pageObject';
import ProlongationLoader
    from '@self/root/src/widgets/content/orderEdit/OrderStorageProlongation/components/Loader/__pageObject';
import outlet1 from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/outlet1';
import realPickpoint1 from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/real_pickpoint1';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';

dayjs.extend(customParseFormat);
dayjs.locale('ru');

export const ORDER_ID = 42;
const DELIVERY_ID = 43;
const OUTLET_STORAGE_LIMIT_DATE = getDateByDaysFromNow(1);
export const LAST_DATE_TO_PROLONGATE_PICKUP = getDateByDaysFromNow(8);
export const LAST_DATE_TO_PROLONGATE_POST_TERM = getDateByDaysFromNow(4);


export default function getSuites(additionalCases) {
    return makeSuite('Изменение срока хранения.', {
        feature: 'Изменение срока хранения',
        environment: 'kadavr',
        story: mergeSuites({
            async beforeEach() {
                this.setPageObjects({
                    prolongateLink: () => this.createPageObject(ProlongateLink),
                    prolongationSuggestion: () => this.createPageObject(ProlongationSuggestion),
                    preloader: () => this.createPageObject(ProlongationLoader),
                });
            },

            'ПВЗ.': mergeSuites(
                {
                    async beforeEach() {
                        const orderId = ORDER_ID;
                        const deliveryId = DELIVERY_ID;
                        const outletId = outlet1.id;

                        await this.browser.setState('Checkouter.collections', {
                            order: {
                                [orderId]: {
                                    id: orderId,
                                    status: ORDER_STATUS.PICKUP,
                                    delivery: deliveryId,
                                },
                            },
                            delivery: {
                                [deliveryId]: {
                                    type: DELIVERY_TYPE.PICKUP,
                                    dates: {
                                        fromDate: getDateByDaysFromNow(0),
                                        toDate: getDateByDaysFromNow(3),
                                    },
                                    outletId,
                                    outletStoragePeriod: 5,
                                    outletStorageLimitDate: OUTLET_STORAGE_LIMIT_DATE,
                                    purpose: 'PICKUP',
                                    outlet: outlet1,
                                },
                            },
                            orderEditOptions: {
                                [orderId]: {
                                    orderId,
                                    storageLimitDatesOptions: [
                                        getDateByDaysFromNow(6),
                                        getDateByDaysFromNow(7),
                                        LAST_DATE_TO_PROLONGATE_PICKUP,
                                    ],
                                },
                            },
                        });

                        await this.browser.yaScenario(
                            this,
                            setReportState,
                            {state: {data: {
                                results: [{...outlet1, storagePeriod: 7}],
                                search: {results: []},
                            }}}
                        );
                    },
                },
                {
                    'Автооткрытие попапа с изменением срока доставки': makeCase({
                        id: 'bluemarket-3659',
                        issue: 'MARKETFRONT-51586',
                        async test() {
                            const shouldBeVisible = this.params.pageId === PAGE_IDS_COMMON.ORDER;
                            await this.browser.yaOpenPage(this.params.pageId, {
                                orderId: ORDER_ID,
                            }, undefined, 'changeDateTime');

                            return this.allure.runStep(`Автооткрытие попапа ${shouldBeVisible ? '' : 'не'} происходит`, () => (
                                this.prolongationSuggestion.waitForVisible(1000, !shouldBeVisible)
                            ));
                        },
                    }),

                    'Запрос на изменение сроков хранения сохраняется.': makeCase({
                        id: 'bluemarket-4088',
                        issue: 'MARKETFRONT-51586',

                        async test() {
                            await this.browser.yaOpenPage(this.params.pageId, {
                                orderId: ORDER_ID,
                            });

                            await this.prolongateLink.click();
                            await this.allure.runStep('Попап открылся', () => {
                                this.prolongationSuggestion.waitForVisible(1000, true);
                            });

                            await this.prolongationSuggestion.title.getText().should.eventually.be.equal(
                                `Продлить срок хранения до четверга, ${dayjs(LAST_DATE_TO_PROLONGATE_PICKUP).format('D MMMM')}?`
                            );
                            await this.prolongationSuggestion.message.getText().should.eventually.be.equal(
                                `Или он закончится ${dayjs(OUTLET_STORAGE_LIMIT_DATE).format('D MMMM')}, и заказ вернётся на склад`
                            );


                            await this.prolongationSuggestion.submitClick();

                            try {
                                await this.preloader.waitForVisible(100, false);
                                await this.preloader.waitForVisible(1000, true);
                            } catch (e) {
                                // бывает, что это работает так быстро, что один из шагов падает ошибкой.
                                // ну и ладно
                            }

                            await this.prolongationSuggestion.title.getText().should.eventually.be.equal(
                                `Срок хранения продлён до ${dayjs(LAST_DATE_TO_PROLONGATE_PICKUP).format('D MMMM')}`
                            );
                            await this.prolongationSuggestion.message.getText().should.eventually.be.equal(
                                `Заказ ${ORDER_ID} ждёт вас в пункте самовывоза по адресу ${outlet1.address.fullAddress}`
                            );

                            await this.prolongationSuggestion.exitClick();
                            await this.prolongationSuggestion.waitForVisible(100, true);
                        },
                    }),
                },
                additionalCases('PICKUP')
            ),

            'Постамат.': mergeSuites(
                {
                    async beforeEach() {
                        const orderId = ORDER_ID;
                        const deliveryId = DELIVERY_ID;
                        const outletId = realPickpoint1.id;

                        await this.browser.setState('Checkouter.collections', {
                            order: {
                                [orderId]: {
                                    id: orderId,
                                    status: ORDER_STATUS.PICKUP,
                                    delivery: deliveryId,
                                },
                            },
                            delivery: {
                                [deliveryId]: {
                                    type: DELIVERY_TYPE.PICKUP,
                                    dates: {
                                        fromDate: getDateByDaysFromNow(0),
                                        toDate: getDateByDaysFromNow(1),
                                    },
                                    outletId,
                                    outletStoragePeriod: 4,
                                    outletStorageLimitDate: OUTLET_STORAGE_LIMIT_DATE,
                                    purpose: 'POST_TERM',
                                    outlet: {...realPickpoint1, isMarketBranded: false},
                                },
                            },
                            orderEditOptions: {
                                [orderId]: {
                                    orderId,
                                    storageLimitDatesOptions: [
                                        getDateByDaysFromNow(2),
                                        getDateByDaysFromNow(3),
                                        LAST_DATE_TO_PROLONGATE_POST_TERM,
                                    ],
                                },
                            },
                        });

                        await this.browser.yaScenario(
                            this,
                            setReportState,
                            {state: {data: {
                                results: [{...realPickpoint1, storagePeriod: 3, isMarketBranded: true}],
                                search: {results: []},
                            }}}
                        );
                    },
                },
                additionalCases('POST_TERM')
            ),
        }),
    });
}

function getDateByDaysFromNow(dayDiff) {
    return dayjs()
        .add(dayDiff, 'day')
        .format('YYYY-MM-DD');
}
