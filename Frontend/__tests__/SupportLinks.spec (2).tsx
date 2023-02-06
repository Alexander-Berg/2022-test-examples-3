import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { act } from 'react-dom/test-utils';

import { SupportLinks } from '../SupportLinks';

let container: HTMLElement | null;

beforeEach(() => {
    container = document.createElement('div');
    document.body.appendChild(container);
});

afterEach(() => {
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    document.body.removeChild(container!);
    container = null;
});

describe('SupportLinks', () => {
    it('Рендерится без ошибок', () => {
        act(() => {
            ReactDOM.render(<SupportLinks />, container);
        });
        expect(container).toMatchSnapshot();
    });

    it('Добавляется ссылка на оригинал', () => {
        act(() => {
            ReactDOM.render(<SupportLinks url={'http://example.com/test'} />, container);
        });
        expect(container).toMatchSnapshot();
    });

    it('Cсылка "Пожаловаться" заменилась на ФОС', () => {
        act(() => {
            ReactDOM.render(<SupportLinks url={'http://example.com/test'} turboAppEnabled />, container);
        });
        expect(container).toMatchSnapshot();
    });

    it('Добавляется дополнительный класс', () => {
        act(() => {
            ReactDOM.render(<SupportLinks className="from-test" url={'http://example.com/test'} />, container);
        });
        expect(container).toMatchSnapshot();
    });
});
