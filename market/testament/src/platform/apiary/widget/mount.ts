/* eslint-disable global-require */

import Runtime from '@yandex-market/apiary/client/runtime';
import ReactDOM from 'react-dom';

let runtime: Runtime;
let inc = 0;

let lastContainer: HTMLDivElement | null = null;

export type MountResult = {
    container: HTMLDivElement;
    runtime: Runtime;
};

export function mount(pathToWidget: string, html: string): MountResult {
    if (lastContainer) {
        ReactDOM.unmountComponentAtNode(
            lastContainer.firstElementChild || lastContainer,
        );
        lastContainer.remove();
        lastContainer = null;
        runtime.destroy();
    }

    runtime = new Runtime({strategy: {}, useShadowStore: false});
    runtime.run();
    const store = runtime._selectStoreByWidgetSource(null);
    const originalDispatch = store.dispatch.bind(store);
    store.dispatch = jest.fn(action => originalDispatch(action));

    require(pathToWidget);

    const container = (lastContainer = window.document.createElement('div'));
    container.id = `apiary-widget-${process.pid}-${inc++}`;
    container.innerHTML = html;

    window.document.body.appendChild(container);

    container
        .querySelectorAll('script:not([type="application/json"])')
        // eslint-disable-next-line
        .forEach(script => window.eval(script.innerHTML));

    return {container, runtime};
}
