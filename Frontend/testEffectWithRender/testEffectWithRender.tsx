import * as React from 'react';
import { render, unmountComponentAtNode } from 'react-dom';

export interface TestEffectWithRenderOptions {
    root: HTMLElement;

    render(Component: React.FC<any>): void;
}

export function testEffectWithRender(title: string, cb: (options: TestEffectWithRenderOptions) => any) {
    describe('test effect with render', function() {
        let root: HTMLDivElement;

        beforeEach(function() {
            root = document.createElement('div');
            document.body.appendChild(root);
        });

        afterEach(() => {
            document.body.removeChild(root);
            unmountComponentAtNode(root);
        });

        it(title, async function() {
            return cb({
                root,
                render(Component) {
                    render(<Component />, root);
                },
            });
        });
    });
}
