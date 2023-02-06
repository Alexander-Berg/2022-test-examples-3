import React from 'react';
import '@testing-library/jest-dom/extend-expect';
import {BrowserRouter} from 'react-router-dom';
import {render, unmountComponentAtNode} from 'react-dom';
import {act} from 'react-dom/test-utils';
import TemplatesList from '../../app/TemplatesList';
import {BasicTemplate, Page} from '../../global/api';

describe('Страница TemplatesList', () => {
    let container: Element;

    beforeEach(() => {
        container = document.createElement('div');
        document.body.appendChild(container);
    });

    afterEach(() => {
        unmountComponentAtNode(container);
        container.remove();
    });

    it('В списке шаблонов отображаются корректные имя, id и количество транспортов', async () => {
        const fakeList: Page<BasicTemplate> = {
            items: [
                {
                    name: 'TemplateName',
                    templateId: '1',
                    transports: ['EMAIL', 'PUSH'],
                },
                {
                    name: 'TemplateName 2',
                    templateId: '2',
                    transports: [],
                },
                {
                    name: 'TemplateName 3',
                    templateId: '3',
                    transports: ['EMAIL'],
                },
            ],
            cursor: '0',
        };

        const fetchSpy = jest.spyOn(global, 'fetch').mockImplementation(() =>
            Promise.resolve({
                text: () => Promise.resolve(JSON.stringify(fakeList)),
            } as Response),
        );

        const component = (
            <BrowserRouter>
                <TemplatesList />
            </BrowserRouter>
        );

        await act(async () => {
            render(component, container);
        });

        fakeList.items.forEach((item, index) => {
            expect(container.querySelectorAll('.templates-list__item-id')[index]?.textContent).toBe(
                `#${item.templateId}`,
            );
            expect(container.querySelectorAll('.templates-list__template-name')[index]?.textContent).toBe(item.name);
            expect(container.querySelectorAll('.templates-list__item-transports')[index]?.childElementCount).toBe(
                item.transports.length,
            );
        });

        fetchSpy.mockRestore();
    });
});
