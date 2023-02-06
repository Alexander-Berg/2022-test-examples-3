import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruFooter } from '../BeruFooter';

describe('BeruFooter', () => {
    const fullPageLink = 'path/to/desktop';

    it('должен отрисовываться без ошибок', () => {
        const wrapper = shallow(<BeruFooter fullPageLink={fullPageLink} />);

        expect(wrapper.length).toEqual(1);
    });

    it('должен содержать правильные ссылки', () => {
        const expectedProps = [
            { text: 'Каталог товаров', url: 'https://m.pokupki.market.yandex.ru/catalog', target: '_blank' },
            { text: 'Мои заказы', url: '//m.market.yandex.ru/my/orders', target: '_blank' },
            { text: 'Доставка и оплата', url: 'https://m.pokupki.market.yandex.ru/my/order/conditions', target: '_blank' },
            { text: 'Возврат', url: 'https://m.pokupki.market.yandex.ru/help/return/terms.html', target: '_blank' },
            { text: 'Справка', url: '//yandex.ru/support/market/', target: '_blank' },
            { text: 'О сервисе', url: '//m.market.yandex.ru/about', target: '_blank' },
            { text: 'Как начать продавать', url: '//partner.market.yandex.ru/welcome/shops?from=adv', target: '_blank' },
            { text: 'Справка для партнёров', url: '//yandex.ru/support/partnermarket/index.html', target: '_blank' },
            { text: 'Партнерская программа', url: '//marketaff.ru/', target: '_blank' },
            { text: 'Полная версия сайта', url: fullPageLink, target: '_blank' },
            { text: 'Пользовательское соглашение', url: '//yandex.ru/legal/marketplace_termsofuse/', target: '_blank' },
        ];
        const wrapper = shallow(<BeruFooter fullPageLink={fullPageLink} />);

        expectedProps.forEach(props => {
            const link = wrapper.find(props);

            expect(link.length).toEqual(1);
        });
    });
});
