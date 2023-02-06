import React from 'react';
import '@testing-library/jest-dom/extend-expect';
import {MemoryRouter, Route, Routes} from 'react-router';
import {render, unmountComponentAtNode} from 'react-dom';
import {act} from 'react-dom/test-utils';
import History from '../../app/History';
import {Version} from '../../global/api';

describe('Страница History', () => {
    let container: Element;

    beforeEach(() => {
        container = document.createElement('div');
        document.body.appendChild(container);
    });

    afterEach(() => {
        unmountComponentAtNode(container);
        container.remove();
    });

    it('В истории версий отображаются корректные id и дата', async () => {
        const fakeList: {items: Version[]; cursor: string; templateName: string} = {
            items: [
                {
                    versionInfo: {userId: 1, login: 'av', createTime: '2021-11-24T20:04:50.649+00:00', versionId: 3},
                    // @ts-expect-error TS2739
                    template: {},
                },
                {
                    versionInfo: {userId: 2, login: 'bq', createTime: '2021-11-24T08:47:02.019+00:00', versionId: 2},
                    // @ts-expect-error TS2739
                    template: {},
                },
                {
                    versionInfo: {userId: 1, login: 'av', createTime: '2021-11-22T12:50:14.933+00:00', versionId: 1},
                    // @ts-expect-error TS2739
                    template: {},
                },
            ],
            cursor: '0',
            templateName: 'name',
        };

        const fetchSpy = jest.spyOn(global, 'fetch').mockImplementation(() =>
            Promise.resolve({
                text: () => Promise.resolve(JSON.stringify(fakeList)),
            } as Response),
        );

        const component = (
            <MemoryRouter initialEntries={['/1']}>
                <Routes>
                    <Route path="/:id" element={<History />} />
                </Routes>
            </MemoryRouter>
        );

        await act(async () => {
            render(component, container);
        });

        fakeList.items.forEach((item, index) => {
            expect(container.querySelectorAll('.history-list__item-id')[index]?.textContent).toBe(
                `#${item.versionInfo.versionId}`,
            );
            expect(container.querySelectorAll('.history-list__item-date')[index]?.textContent).toBe(
                new Date(item.versionInfo.createTime).toLocaleString(),
            );
        });

        fetchSpy.mockRestore();
    });
});
