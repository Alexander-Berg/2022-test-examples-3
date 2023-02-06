import * as React from 'react';
import { shallow } from 'enzyme';

import { EcomPromoBadge } from '@yandex-turbo/components/EcomPromoBadge/EcomPromoBadge';

import { IProps, IContact } from '../EcomFooter.types';
import { EcomFooterPresenter as EcomFooter } from '../EcomFooter';
import { EcomFooterContact } from '../Contact/EcomFooter-Contact';

// не смотря на то, что в вебмастере есть договоренность о максимальном числе контактов = 4, проверяем все
const contacts: IContact[] = [
    {
        type: 'call',
        text: 'Phone 1',
    },
    {
        type: 'vkontakte',
        text: 'Vk 1',
    },
    {
        type: 'chat',
        text: 'Chat 1',
    },
    {
        type: 'call',
        text: 'Phone 2',
    },
    {
        type: 'twitter',
        text: 'Twitter 1',
    },
    {
        type: 'mail',
        text: 'Mail 1',
    },
    {
        type: 'facebook',
        text: 'Fb 1',
    },
];

const props: IProps = {
    localization: {
        turboInfo: 'Этот магазин создан на технологии Яндекса {turboPages}',
        turboPages: 'Турбо-страницы',
        fullVersion: 'Оригинальный сайт',
        supportText: 'Пожаловаться',
        supportUrl: 'https://yandex.ru/support/abuse/troubleshooting/turbo/list.html',
    },
    promoBadge: <EcomPromoBadge url="https://ya.ru" />,
};

describe('Компонент EcomFooter', () => {
    it('должен отрендериться', () => {
        const footer = shallow(<EcomFooter {...props} />);

        expect(footer.length).toEqual(1);
    });

    it('должен не рендерить название компании при его отсутствии', () => {
        const footer = shallow(
            <EcomFooter
                {...props}
            />
        );

        expect(footer.find('.ecom-footer__company').length).toEqual(0);
    });

    it('должен отрендерить название компании при наличии', () => {
        const footer = shallow(
            <EcomFooter
                {...props}
                companyName="Company"
            />
        );

        expect(footer.find('.ecom-footer__company').length).toEqual(1);
    });

    it('должен не рендерить оригинальный url магазина при его отсутствии', () => {
        const footer = shallow(
            <EcomFooter
                {...props}
            />
        );

        expect(footer.find('.ecom-footer__support-link').length).toEqual(1);
    });

    it('должен отрендерить оригинальный url магазина при его наличии', () => {
        const url = 'https://yandex.ru';
        const footer = shallow(
            <EcomFooter
                {...props}
                url={url}
            />
        );

        expect(footer.find('.ecom-footer__support-link').length).toEqual(2);
    });

    it('должен не рендерить неизвестные группы контактов', () => {
        const contacts: IContact[] = [
            {
                type: 'call',
                text: 'Phone 1',
            },
            {
                type: 'vkontakte',
                text: 'Vk 1',
            },
        ];
        const footer = shallow(
            <EcomFooter
                {...props}
                contacts={contacts}
            />
        );

        expect(footer.find(EcomFooterContact).length).toEqual(1);
    });

    it('должен отрендерить контакты в правильном порядке', () => {
        const footer = shallow(
            <EcomFooter
                {...props}
                contacts={contacts}
            />
        );

        expect(footer.find(EcomFooterContact).map(elem => elem.render().text())).toEqual([
            'Phone 1',
            'Phone 2',
            'Mail 1',
        ]);
    });
});
