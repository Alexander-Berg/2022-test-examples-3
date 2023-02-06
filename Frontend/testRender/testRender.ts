import { createElement, ReactNode } from 'react';
import { render } from 'react-dom';
import { renderToString } from 'react-dom/server';
import { Provider } from 'react-redux';
import { createStore } from 'redux';

import { State } from 'schema/state/State';

export function testRender(state: DeepPartial<State>, node: ReactNode) {
    let store = createStore((state) => state, state as any);
    let app = createElement(Provider, { store: store }, node);

    if (IS_SERVER) {
        renderToString(app);
    } else {
        let container = document.createElement('div');
        render(app, container);
    }
}
