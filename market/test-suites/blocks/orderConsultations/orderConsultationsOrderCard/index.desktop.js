import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';
import assert from 'assert';

import OrderConsultationButton from '@self/root/src/components/Orders/OrderConsultationButton/__pageObject';
import YandexMessenger from '@self/root/src/widgets/core/YandexMessenger/__pageObject';

import baseSuite from './baseSuite';

module.exports = makeSuite('Арбитраж. Открытие чата с карточки заказа', {
    feature: 'Арбитраж',
    environment: 'kadavr',
    params: {
        pageId: 'Идентификатор страницы',
    },
    meta: {
        issue: 'MARKETFRONT-36441',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                assert(this.params.pageId, 'Param pageId must be defined');
                assert(this.orderCard, 'PageObject.orderCard must be defined');

                this.setPageObjects({
                    orderConsultationButton: () => this.createPageObject(
                        OrderConsultationButton,
                        {parent: this.orderCard}
                    ),
                    yandexMessenger: () => this.createPageObject(YandexMessenger),
                });
            },
        },
        prepareSuite(baseSuite)
    ),
});

