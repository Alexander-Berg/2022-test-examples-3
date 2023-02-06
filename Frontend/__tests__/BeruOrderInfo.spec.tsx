import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruPriceInterval } from '@yandex-turbo/components/BeruPriceInterval/BeruPriceInterval';
import { BeruDeliveryInterval } from '@yandex-turbo/components/BeruDeliveryInterval/BeruDeliveryInterval';
import { LinkLayoutDefault as Link } from '@yandex-turbo/components/Link/_layout/Link_layout_default';
import { BeruText } from '@yandex-turbo/components/BeruText/BeruText';
import { BeruOrderInfoItem } from '../Item/BeruOrderInfoItem';
import { BeruOrderInfo } from '../BeruOrderInfo';

const data = {
    anyOption: {
        minDays: 1,
        maxDays: 1,
        minPrice: 100,
        maxPrice: 200,
    },
    merchantName: 'Беру!',
    deliveryUrl: 'https://test.ru/path/to',
};

describe('BeruOrderInfo', () => {
    it('должен правильно отрисовываться с курьерской опцией', () => {
        const wrapper = shallow(<BeruOrderInfo courier={data.anyOption} merchantName={data.merchantName} deliveryUrl={data.deliveryUrl} />);

        expect(wrapper.find(BeruOrderInfoItem).at(0).props()).toMatchObject({ title: 'Доставка' });
        expect(wrapper.find(BeruOrderInfoItem).at(1).props()).toMatchObject({ title: 'Продавец товара', type: 'tag' });

        expect(wrapper.find(BeruDeliveryInterval).props()).toMatchObject({
            minDays: 1,
            maxDays: 1,
        });
        expect(wrapper.find(BeruPriceInterval).props()).toMatchObject({
            minPrice: 100,
            maxPrice: 200,
        });
        expect(wrapper.find(Link).props()).toMatchObject({
            url: 'https://test.ru/path/to',
            className: 'beru-order-info__condition-link',
        });
    });

    it('должен правильно отрисовываться с почтовой опцией', () => {
        const wrapper = shallow(<BeruOrderInfo post={data.anyOption} merchantName={data.merchantName} deliveryUrl={data.deliveryUrl} />);

        expect(wrapper.find(BeruOrderInfoItem).at(0).props()).toMatchObject({ title: 'Доставка почтой' });
        expect(wrapper.find(BeruOrderInfoItem).at(1).props()).toMatchObject({ title: 'Продавец товара', type: 'tag' });

        expect(wrapper.find(BeruDeliveryInterval).props()).toMatchObject({
            minDays: 1,
            maxDays: 1,
        });
        expect(wrapper.find(BeruPriceInterval).props()).toMatchObject({
            minPrice: 100,
            maxPrice: 200,
        });
        expect(wrapper.find(Link).props()).toMatchObject({
            url: 'https://test.ru/path/to',
            className: 'beru-order-info__condition-link',
        });
    });

    it('должен правильно отрисовываться с опцией самовывоза', () => {
        const wrapper = shallow(<BeruOrderInfo pickup={data.anyOption} merchantName={data.merchantName} deliveryUrl={data.deliveryUrl} />);

        expect(wrapper.find(BeruOrderInfoItem).at(0).props()).toMatchObject({ title: 'Самовывоз', type: 'pickup' });
        expect(wrapper.find(BeruOrderInfoItem).at(1).props()).toMatchObject({ title: 'Продавец товара', type: 'tag' });

        expect(wrapper.find(BeruDeliveryInterval).props()).toMatchObject({
            minDays: 1,
            maxDays: 1,
        });
        expect(wrapper.find(BeruPriceInterval).props()).toMatchObject({
            minPrice: 100,
            maxPrice: 200,
        });
        expect(wrapper.find(Link).props()).toMatchObject({
            url: 'https://test.ru/path/to',
            className: 'beru-order-info__condition-link',
        });
    });

    it('должен правильно отрисовываться с опцией "только самовывоз"', () => {
        const wrapper = shallow(<BeruOrderInfo onlyPickup={data.anyOption} merchantName={data.merchantName} />);

        expect(wrapper.find(
            BeruOrderInfoItem).at(0).props()
        ).toMatchObject({ title: 'Выкупить в торговом зале', type: 'onlyPickup' });
        expect(wrapper.find(
            BeruOrderInfoItem).at(1).props()
        ).toMatchObject({ title: 'Продавец товара', type: 'tag' });

        expect(wrapper.find(BeruDeliveryInterval).props()).toMatchObject({
            minDays: 1,
            maxDays: 1,
        });
        expect(wrapper.find(BeruPriceInterval).props()).toMatchObject({
            minPrice: 100,
            maxPrice: 200,
        });
        expect(wrapper.find(Link)).not.toHaveLength(1);
    });

    it('должен правильно отрисовываться с опцией предзаказа', () => {
        const wrapper = shallow(<BeruOrderInfo preorder={data.anyOption} />);

        expect(wrapper.find(BeruOrderInfoItem).at(0).props()).toMatchObject({ title: 'Доставка' });
        expect(wrapper.find(
            BeruOrderInfoItem
        ).at(1).props()).toMatchObject({ title: 'Оформление предзаказа', type: 'card', children: 'доступно только после оплаты' });
        expect(wrapper.find(
            BeruOrderInfoItem
        ).at(2).props()).toMatchObject({ title: 'Товар доставит', type: 'tag' });

        expect(wrapper.find(BeruDeliveryInterval).props()).toMatchObject({
            minDays: 1,
            maxDays: 1,
        });
        expect(wrapper.find(BeruPriceInterval).props()).toMatchObject({
            minPrice: 100,
            maxPrice: 200,
        });
        expect(wrapper.find(Link)).not.toHaveLength(1);
    });

    it('не должен выводить почтовую опцию если еще передана курьерская', () => {
        const wrapper = shallow(<BeruOrderInfo courier={data.anyOption} post={data.anyOption} merchantName={data.merchantName} />);

        expect(wrapper.find(BeruOrderInfoItem)).toHaveLength(2);
    });

    it('должен выводить почтовую опцию если не передана курьерская', () => {
        const wrapper = shallow(<BeruOrderInfo post={data.anyOption} merchantName={data.merchantName} />);

        expect(wrapper.find(BeruOrderInfoItem)).toHaveLength(2);
    });

    it('должен выводить сообщение об ошибке если нет опций доставки', () => {
        const wrapper = shallow(<BeruOrderInfo />);

        expect(wrapper.find(BeruText).props()).toMatchObject({
            size: '200',
            theme: 'error',
            children: 'Нет доступных опций доставки.',
        });
    });
});
