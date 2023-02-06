import * as React from 'react';
import { shallow } from 'enzyme';

import { SocialType, ContactsType } from '@yandex-turbo/components/lcTypes/lcTypes';
import { IProps as ILcFooterProps } from '../LcFooter.types';
import { LcFooterComponent as LcFooter } from '../LcFooter';

const localization = {
    turboPages: 'Турбо-страница',
    turboDescription: 'Вы находитесь на странице, адаптированной для быстрой загрузки',
    fullVersion: 'Полная версия',
    supportText: 'Пожаловаться',
    supportUrl: '/',
    closeModal: 'Закрыть',
    menu: 'Меню',
    companyAbout: 'О компании',
    contactsTitle: 'Контакты',
    ucLinkText: 'Создано в&nbsp;Конструкторе&nbsp;Турбо-сайтов в&nbsp;Яндекс&nbsp;Директе',
    ucLinkUrl: 'https://direct.yandex.ru/constructor?utm_source=sites_bottom',
};

const copyrights = {
    block: 'agreement',
    more: 'Согласие на обработку персональных данных',
    full_text: {
        title: 'Согласие на обработку персональных данных',
        text: [
            {
                block: 'paragraph',
                content: ['Текст такой то ', 'и такой то'],
            },
        ],
    },
};

const data: ILcFooterProps = {
    addFloatButtonPadding: false,
    short: false,
    localization,
    companyName: '',
    companyYear: '',
    copyrights,
    url: '',
    isAjax: false,
    isInfinityFeed: false,
    anchor: '',
    attrs: { 'data-section-id': '1', 'data-section-name': 'LcFooter' },
    pageId: 0,
    sectionName: 'sectionName',
    hasUcLinkFooter: false,
};

describe('Компонент LcFooter', () => {
    it('должен отрендериться', () => {
        const footer = shallow(<LcFooter {...data} />);

        expect(footer.length).toEqual(1);
    });

    it('должен отрендерить среднюю строку при наличии копирайта', () => {
        const footer = shallow(<LcFooter {...data} />);

        expect(footer.find('.lc-footer__wrap').length).toEqual(2);
    });

    it('должен отрендерить среднюю строку при наличии названия компании', () => {
        const footerData: ILcFooterProps = {
            ...data,
            copyrights: undefined,
            companyName: 'abc',
        };
        const footer = shallow(<LcFooter {...footerData} />);

        expect(footer.find('.lc-footer__wrap').length).toEqual(2);
    });

    it('должен отрендерить среднюю строку при наличии при наличии года основания', () => {
        const footerData: ILcFooterProps = {
            ...data,
            copyrights: undefined,
            companyYear: '1995',
        };
        const footer = shallow(<LcFooter {...footerData} />);

        expect(footer.find('.lc-footer__wrap').length).toEqual(2);
    });

    it('должен отрендерить среднюю строку при наличии при наличии соцсетей', () => {
        const footerData: ILcFooterProps = {
            ...data,
            copyrights: undefined,
            social: {
                items: [{ type: SocialType.Facebook, url: '/' }],
            },
        };

        const footer = shallow(<LcFooter {...footerData} />);

        expect(footer.find('.lc-footer__wrap').length).toEqual(2);
    });

    it('должен отрендерить среднюю строку при наличии лого', () => {
        const footerData: ILcFooterProps = {
            ...data,
            copyrights: undefined,
            logo: {
                text: '123',
            },
        };

        const footer = shallow(<LcFooter {...footerData} />);

        expect(footer.find('.lc-footer__wrap').length).toEqual(2);
    });

    it('должен отрендерить среднюю строку при наличии контактов', () => {
        const footerData: ILcFooterProps = {
            ...data,
            copyrights: undefined,
            contacts: {
                items: [{ type: ContactsType.Phone, text: '123' }],
            },
        };

        const footer = shallow(<LcFooter {...footerData} />);

        expect(footer.find('.lc-footer__wrap').length).toEqual(2);
    });

    it('должен отрендерить 3 строки при наличии контактов и соцсетей ', () => {
        const footerData: ILcFooterProps = {
            ...data,
            copyrights: undefined,
            contacts: {
                items: [{ type: ContactsType.Phone, text: '123' }],
            },
            social: {
                items: [{ type: SocialType.Facebook, url: '/' }],
            },
        };

        const footer = shallow(<LcFooter {...footerData} />);

        expect(footer.find('.lc-footer__wrap').length).toEqual(3);
    });

    it('должен отрендерить только нижнюю строку', () => {
        const footerData: ILcFooterProps = {
            ...data,
            copyrights: undefined,
        };
        const footer = shallow(<LcFooter {...footerData} />);

        expect(footer.find('.lc-footer__wrap').length).toEqual(1);
    });

    it('должен отрендерить строку со ссылкой на конструктор если передан флаг hasUcLinkFooter', () => {
        const footerData: ILcFooterProps = {
            ...data,
            copyrights: undefined,
            hasUcLinkFooter: true,
        };
        const footer = shallow(<LcFooter {...footerData} />);

        expect(footer.find('.lc-footer__wrap').length).toEqual(2);
    });
});
