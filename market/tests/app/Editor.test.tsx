// import React from 'react';

// import {MemoryRouter, Route, Routes} from 'react-router';
// import {render, unmountComponentAtNode} from 'react-dom';
// import {act} from 'react-dom/test-utils';
// import Editor from 'app/Editor';
// import {FullTemplate} from 'global/api';
// import {Transports} from 'global/consts';

import {unmountComponentAtNode} from 'react-dom';

import '@testing-library/jest-dom/extend-expect';

describe('Страница Editor', () => {
    let container: Element;

    beforeEach(() => {
        container = document.createElement('div');
        document.body.appendChild(container);
    });

    afterEach(() => {
        unmountComponentAtNode(container);
        container.remove();
    });

    // it('На странице редактора отображаются корректные данные шаблона', async () => {
    //     const fakeTemplate: FullTemplate = {
    //         name: 'Template 1',
    //         templateId: '1',
    //         template: 'На счету магазина {{#variable}}shop_name{{/variable}} недостаточно средств',
    //         commonTemplateId: '1',
    //         variables: [
    //             {name: 'var1', type: 'string'},
    //             {name: 'var2', type: 'string'},
    //             {name: 'var3', type: 'string'},
    //         ],
    //         transports: ['EMAIL', 'WEB'],
    //     };
    //
    //     const fetchSpy = jest.spyOn(global, 'fetch').mockImplementation(() =>
    //         Promise.resolve({
    //             text: () => Promise.resolve(JSON.stringify(fakeTemplate)),
    //         } as Response),
    //     );
    //
    //     const component = (
    //         <MemoryRouter initialEntries={['/1']}>
    //             <Routes>
    //                 <Route path="/:id" element={<Editor />} />
    //             </Routes>
    //         </MemoryRouter>
    //     );
    //
    //     await act(async () => {
    //         render(component, container);
    //     });
    //
    //     const variablesItemElements = container.querySelectorAll('.vars__list-item');
    //
    //     expect(container.querySelector('.editor__template-name')?.textContent).toBe(fakeTemplate.name);
    //     expect(container.querySelector('.params-select__item_parent-id .Button2-Text')?.textContent).toBe(
    //         fakeTemplate.commonTemplateId,
    //     );
    //     expect(container.querySelector('.params-select__item_transport .Button2-Text')?.textContent).toBe(
    //         Transports[fakeTemplate.transports[0]],
    //     );
    //     fakeTemplate.variables.forEach((variable, index) => {
    //         expect(variablesItemElements[index]?.querySelector('.vars__input-name')?.textContent).toBe(variable.name);
    //     });
    //
    //     fetchSpy.mockRestore();
    // });
});
