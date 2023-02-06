import React from 'react';
import { IPopupProps } from '@yandex-lego/components/Popup';

import { getSampleContacts } from '~/test/jest/mocks/data/operational-link';
import { render } from './operational-link.po';

jest.mock('~/src/common/components/Modal/Modal', () => ({
    ModalWithCross: ({ visible, children }: IPopupProps) => (
        Boolean(visible) && <div className="ModalWithCross">{children}</div>
    ),
}));

describe('Блок оперативной связи с Сервисом', () => {
    it('1. Ожидание загрузки списка контактов', () => {
        // - do: открыть страницу сервиса (/services/service)
        // рендерим блок оперативной связи с Сервисом
        const operationalLink = render({
            communicationContacts: {
                loading: true,
                contacts: [],
            },
        });

        // - assert: отображается спиннер
        expect(operationalLink.spinner?.container).toBeInTheDocument();
    });

    it('2. Отображение списка контактов', () => {
        // - do: открыть страницу сервиса (/services/service)
        // рендерим блок оперативной связи с Сервисом
        const operationalLink = render({
            communicationContacts: {
                loading: false,
                contacts: getSampleContacts(),
            },
        });

        // - assert: отображаются две кнопки
        expect(operationalLink.buttons.length).toEqual(4);
    });

    it('3. Клик по ссылке на сайт', () => {
        // - do: открыть страницу сервиса (/services/service)
        // рендерим блок оперативной связи с Сервисом
        const operationalLink = render({
            communicationContacts: {
                loading: false,
                contacts: getSampleContacts(),
            },
        });

        // - do: клик по кнопке с текстом Сайт
        const button = operationalLink.buttons.find(x => x.content === 'Сайт');
        button?.click();

        // - assert: произошёл переход по ссылке в текущем окне
        expect(button?.url).toEqual('https://abc.yandex-team.ru/');
        expect(button?.opensInNewWindow).toBeFalsy();
        expect(operationalLink.iframe?.container).toBeUndefined();
    });

    it('4. Клик по ссылке на внешний сайт', () => {
        // - do: открыть страницу сервиса (/services/service)
        // рендерим блок оперативной связи с Сервисом
        const operationalLink = render({
            communicationContacts: {
                loading: false,
                contacts: getSampleContacts(),
            },
        });

        // - do: клик по кнопке с текстом Внешний сайт
        const button = operationalLink.buttons.find(x => x.content === 'Внешний сайт');
        button?.click();

        // - assert: произошёл переход по ссылке в новом окне через hidereferer
        expect(button?.url).toEqual('http://h.yandex.net?https%3A//yandex.ru/');
        expect(button?.opensInNewWindow).toBeTruthy();
        expect(operationalLink.iframe?.container).toBeUndefined();
    });

    it('5. Клик по ссылке на форму', () => {
        // - do: открыть страницу сервиса (/services/service)
        // рендерим блок оперативной связи с Сервисом
        const operationalLink = render({
            communicationContacts: {
                loading: false,
                contacts: getSampleContacts(),
            },
        });

        // - do: клик по кнопке с текстом Неанонимная форма
        const button = operationalLink.buttons.find(x => x.content === 'Неанонимная форма');
        button?.click();

        // - assert: открылось модальное окно с формой
        expect(button?.url).toEqual('https://forms.yandex-team.ru/surveys/62156/?iframe=1');
        expect(button?.opensInNewWindow).toBeFalsy();
        expect(operationalLink.iframe?.container).toBeInTheDocument();
    });

    it('6. Клик по ссылке на форму', () => {
        // - do: открыть страницу сервиса (/services/service)
        // рендерим блок оперативной связи с Сервисом
        const operationalLink = render({
            communicationContacts: {
                loading: false,
                contacts: getSampleContacts(),
            },
        });

        // - do: клик по кнопке с текстом Анонимная форма
        const button = operationalLink.buttons.find(x => x.content === 'Анонимная форма');
        button?.click();

        // - assert: открылось модальное окно с формой
        expect(button?.url).toEqual('https://forms.yandex.net/surveys/62156/?iframe=1');
        expect(button?.opensInNewWindow).toBeFalsy();
        expect(operationalLink.iframe?.container).toBeInTheDocument();
    });

    it('7. Отображение пустого списка контактов', () => {
        // - do: открыть страницу сервиса (/services/service)
        // рендерим блок оперативной связи с Сервисом
        const operationalLink = render({
            communicationContacts: {
                loading: false,
                contacts: [],
            },
        });

        // - assert: блок оперативной связи с Сервисом скрыт
        expect(operationalLink?.container).toBeEmptyDOMElement();
    });
});
