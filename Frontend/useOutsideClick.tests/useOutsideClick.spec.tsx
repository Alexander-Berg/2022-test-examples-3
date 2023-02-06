import React, { useRef } from 'react';
import { render, fireEvent } from '@testing-library/react';

describe('useOutsideClick', () => {
    it('should trigger callback only on click outside', () => {
        jest.isolateModules(() => {
            const { useOutsideClick } = require('../useOutsideClick');
            const onClick = jest.fn();

            const Component = () => {
                const ref = useRef(null);

                useOutsideClick(ref, onClick);

                return (
                    <div data-testid="container-element" style={{ width: 1000, height: 1000 }}>
                        <div ref={ref} data-testid="test-element" style={{ width: 200, height: 200, margin: '0 auto' }}>
                            <div data-testid="inner-element" style={{ width: 100, height: 100 }} />
                        </div>,
                    </div>
                );
            };

            const { getByTestId } = render(
                <Component />,
            );

            const inner = getByTestId('inner-element');
            const element = getByTestId('test-element');
            const container = getByTestId('container-element');

            fireEvent.click(inner);
            expect(onClick).toHaveBeenCalledTimes(0);

            fireEvent.click(element);
            expect(onClick).toHaveBeenCalledTimes(0);

            fireEvent.click(container);
            expect(onClick).toHaveBeenCalledTimes(1);
        });
    });

    it('should trigger callback only on mousedown outside', () => {
        jest.isolateModules(() => {
            const { useOutsideClick } = require('../useOutsideClick@desktop');
            const onClick = jest.fn();

            const Component = () => {
                const ref = useRef(null);

                useOutsideClick(ref, onClick);

                return (
                    <div data-testid="container-element" style={{ width: 1000, height: 1000 }}>
                        <div ref={ref} data-testid="test-element" style={{ width: 200, height: 200, margin: '0 auto' }}>
                            <div data-testid="inner-element" style={{ width: 100, height: 100 }} />
                        </div>,
                    </div>
                );
            };

            const { getByTestId } = render(
                <Component />,
            );

            const inner = getByTestId('inner-element');
            const element = getByTestId('test-element');
            const container = getByTestId('container-element');

            fireEvent.click(inner);
            expect(onClick).toHaveBeenCalledTimes(0);

            fireEvent.mouseDown(element);
            expect(onClick).toHaveBeenCalledTimes(0);

            fireEvent.mouseDown(container);
            expect(onClick).toHaveBeenCalledTimes(1);
        });
    });

    it('should trigger callback only on touchstart outside', () => {
        jest.isolateModules(() => {
            const { useOutsideClick } = require('../useOutsideClick@touch');
            const onClick = jest.fn();

            const Component = () => {
                const ref = useRef(null);

                useOutsideClick(ref, onClick);

                return (
                    <div data-testid="container-element" style={{ width: 1000, height: 1000 }}>
                        <div ref={ref} data-testid="test-element" style={{ width: 200, height: 200, margin: '0 auto' }}>
                            <div data-testid="inner-element" style={{ width: 100, height: 100 }} />
                        </div>,
                    </div>
                );
            };

            const { getByTestId } = render(
                <Component />,
            );

            const inner = getByTestId('inner-element');
            const element = getByTestId('test-element');
            const container = getByTestId('container-element');

            fireEvent.click(inner);
            expect(onClick).toHaveBeenCalledTimes(0);

            fireEvent.touchStart(element);
            expect(onClick).toHaveBeenCalledTimes(0);

            fireEvent.touchStart(container);
            expect(onClick).toHaveBeenCalledTimes(1);
        });
    });
});
