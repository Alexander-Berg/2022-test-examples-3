import {
    makeSuite,
    makeCase,
} from 'ginny';

// PageObjects
import DeliveryRemainder from '@self/root/src/components/DeliveryRemainder/__pageObject';
// eslint-disable-next-line max-len
import ExpressNotifier from '@self/root/src/widgets/content/cart/CartDeliveryTermsNotifier/components/ExpressNotifier/__pageObject';
import BusinessGroupsStrategiesSelector
    from '@self/root/src/widgets/content/cart/CartList/components/BusinessGroupsStrategiesSelector/__pageObject';


export default makeSuite('Состояние групп корзины', {
    params: {
        isTresholdVisible: 'Виден ли трешхолд',
        onlyExpress: 'Виден только экспресс трешхолд',
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                strategiesSelector: () => this.createPageObject(BusinessGroupsStrategiesSelector),
                deliveryRemainder: () => this.createPageObject(DeliveryRemainder),
                expressNotifier: () => this.createPageObject(ExpressNotifier),
            });
        },
        'посылки ожидаемо сгруппированы и состояние трешхолда верное': makeCase({
            async test() {
                const {
                    isTresholdVisible,
                    onlyExpress,
                } = this.params;

                if (onlyExpress) {
                    await this.expressNotifier.isExisting()
                        .should.eventually.to.be.equal(
                            onlyExpress,
                            `Экспресс трешхолд должен быть ${onlyExpress ? '' : 'не'}видимым`
                        );
                }

                await this.deliveryRemainder.isExisting()
                    .should.eventually.to.be.equal(
                        isTresholdVisible,
                        `Трешхолд должен быть ${isTresholdVisible ? '' : 'не'}видимым`
                    );
            },
        }),
    },
});
