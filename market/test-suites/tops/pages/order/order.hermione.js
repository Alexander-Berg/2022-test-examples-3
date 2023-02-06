import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {commonParams} from '@self/root/src/spec/hermione/configs/params';
import OrdersPage from '@self/root/src/widgets/pages.desktop/OrdersPage/__pageObject';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Заказ', {
    environment: 'testing',
    params: {
        ...commonParams.description,
        pageId: 'Идентификатор страницы',
    },
    defaultParams: {
        ...commonParams.value,
    },
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    ordersLayout: () => this.createPageObject(OrdersPage),
                });
            },
        },
        {
            'Авторизованный пользователь.': mergeSuites(
                prepareSuite(require('@self/root/src/spec/hermione/test-suites/desktop.blocks/myorder'), {
                    params: {
                        isAuthWithPlugin: true,
                        pageId: PAGE_IDS_COMMON.ORDER,
                    },
                })
            ),
        }
    ),
});
