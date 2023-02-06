import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';
import assert from 'assert';

import {
    ActionLink,
} from '@self/root/src/components/OrderActions/Actions/ActionLink/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';
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
                    orderConsultationActionLink: () => this.createPageObject(
                        ActionLink,
                        {
                            parent: this.orderCard,
                            root: `${ActionLink.root}${ActionLink.orderConsultationLink}`,
                        }
                    ),
                    orderConsultationButton: () => this.createPageObject(
                        Link,
                        {
                            root: this.orderConsultationActionLink.root,
                        }
                    ),
                    yandexMessenger: () => this.createPageObject(YandexMessenger),
                });
            },
        },
        prepareSuite(baseSuite)
    ),
});
