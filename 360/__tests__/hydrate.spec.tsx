import React, { FunctionComponent, ReactElement } from 'react';
import type { Renderer } from 'react-dom';
import type { CoverLayerProps } from '../index';

describe('CoverLayer', () => {
    describe('hydration', () => {
        let container: HTMLElement | null = null;

        function inServerContext(
            action: (
                container: HTMLElement,
                CoverLayer: FunctionComponent<CoverLayerProps>,
                renderToString: (element: ReactElement) => string
            ) => void,
        ) {
            action(
                container as HTMLElement,
                require('../index').CoverLayer,
                require('react-dom/server').renderToString,
            );
            jest.resetModules();
        }

        function inClientContext(
            action: (
                container: HTMLElement,
                CoverLayer: FunctionComponent<CoverLayerProps>,
                hydrate: Renderer
            ) => void,
        ) {
            action(
                container as HTMLElement,
                require('../index').CoverLayer,
                require('react-dom').hydrate,
            );
            jest.resetModules();
        }

        beforeEach(() => {
            jest.resetModules();
            container = document.createElement('div');
            document.body.appendChild(container as HTMLElement);
        });

        afterEach(() => {
            document.body.removeChild(container as HTMLElement);
            container = null;
            jest.resetAllMocks();
        });

        function run(props: CoverLayerProps) {
            const spy = jest.spyOn(console, 'error').mockImplementation(jest.fn());

            expect(spy).not.toBeCalled();

            inServerContext((container, CoverLayer, renderToString) => {
                container.innerHTML = renderToString(<CoverLayer className="fixed" { ...props } />);

                expect(container).toMatchSnapshot();
            });

            inClientContext((container, CoverLayer, hydrate) => {
                hydrate(<CoverLayer className="fixed" { ...props } />, container);

                expect(container).toMatchSnapshot();
            });

            return spy;
        }

        it('should hydrate image', () => {
            const error = run({
                imageUrl: 'https://example.org/img.jpg',
            });

            expect(error).not.toBeCalled();
        });

        it('should hydrate video', () => {
            const error = run({
                videoUrl: 'https://example.org/video.mp4',
            });

            // 'muted' аттрибут работает плохо
            // @see https://github.com/facebook/react/issues/10389
            expect(error).toBeCalledTimes(1);
            expect(error).toBeCalledWith(
                expect.stringContaining('Warning: Prop `%s` did not match'),
                'muted',
                expect.any(String),
                expect.any(String),
                expect.any(String),
            );
        });
    });
});
