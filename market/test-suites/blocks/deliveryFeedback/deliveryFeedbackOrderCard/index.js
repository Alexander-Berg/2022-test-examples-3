import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import RatingControl from '@self/root/src/uikit/components/RatingControl/__pageObject';
import OrderCardDeliveryFeedback from '@self/root/src/components/OrderCardDeliveryFeedback/__pageObject';

import baseSuite from './baseSuite';

module.exports = makeSuite('Виджет "Заказ у меня" на карточке заказа', {
    feature: 'Виджет "Заказ у меня"',
    environment: 'kadavr',
    params: {
        pageId: 'Идентификатор страницы',
    },
    meta: {
        issue: 'MARKETFRONT-16609',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    widget: () => this.createPageObject(OrderCardDeliveryFeedback),
                    ratingControl: () => this.createPageObject(RatingControl, {
                        root: OrderCardDeliveryFeedback.ratingControl,
                    }),
                    title: () => this.widget.title,
                    description: () => this.widget.description,
                    primaryButton: () => this.widget.primaryButton,
                    secondaryButton: () => this.widget.secondaryButton,
                });
            },
        },
        prepareSuite(baseSuite)
    ),
});

