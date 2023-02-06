import * as React from 'react';
import { renderToString } from 'react-dom/server';
import { mount, shallow, ReactWrapper, ShallowWrapper } from 'enzyme';
import * as serializer from 'jest-serializer-html';

import { LcSizePx, LcFont, LcTypeface, LcSizes } from '@yandex-turbo/components/lcTypes/lcTypes';
import { LcIframeComponent } from '../LcIframe';
import { ILcIframeProps } from '../LcIframe.types';

expect.addSnapshotSerializer(serializer);

describe('LcIframe component', () => {
    let wrapper: ReactWrapper | ShallowWrapper;
    const requiredProps: ILcIframeProps = {
        id: 'iframe-id',
        src: 'https://ya.ru',
    };

    function simulateMessageEvent(init: MessageEventInit, source: Window) {
        const event = new MessageEvent('message', {
            origin: 'https://ya.ru',
            source,
            ...init,
        });

        window.dispatchEvent(event);
    }

    beforeAll(() => {
        // необходимо для работы window.location.search без ошибок
        // https://github.com/jsdom/jsdom/issues/2112#issuecomment-456171953
        delete window.location;
        Object.defineProperty(window, 'location', {
            writable: true,
            value: {
                reload: jest.fn(),
            },
        });
    });

    describe('Client', () => {
        afterEach(() => {
            wrapper.unmount();
        });

        test('should render title', () => {
            const props = {
                title: {
                    content: 'Iframe title',
                    size: LcSizePx.s36,
                    font: LcFont.DISPLAY,
                    typeface: LcTypeface.MEDIUM,
                },
                titleOffsets: {
                    top: LcSizes.NONE,
                    bottom: LcSizes.S,
                },
            };
            wrapper = shallow(<LcIframeComponent {...requiredProps} {...props} />);

            expect(wrapper.html()).toMatchSnapshot();
        });

        test('should pass className to root element', () => {
            wrapper = shallow(<LcIframeComponent {...requiredProps} className="test-class" />);
            const className = wrapper.prop('className');

            expect(className).toBe('lc-iframe test-class');
        });

        test('should pass height to iframe element', () => {
            wrapper = shallow(<LcIframeComponent {...requiredProps} height={1337} />);
            const style = wrapper.find('iframe').prop('style');

            expect(style).toEqual({ height: 1337 });
        });

        test('should call onLoad', () => {
            const onLoad = jest.fn();
            wrapper = mount(<LcIframeComponent {...requiredProps} onLoad={onLoad} />);
            const iframe = wrapper.find('iframe');

            iframe.simulate('load');

            expect(onLoad).toHaveBeenCalledTimes(1);
            expect(onLoad).toHaveBeenCalledWith(iframe.instance());
        });

        test('should call onMessage', () => {
            const onMessage = jest.fn();
            const data = { name: 'iframe-id', foo: 'bar' };

            wrapper = mount(<LcIframeComponent {...requiredProps} onMessage={onMessage} />);

            const frameSource = wrapper.find('iframe').getDOMNode();

            simulateMessageEvent({ data: JSON.stringify(data) }, frameSource);

            expect(onMessage).toHaveBeenCalledTimes(1);
            expect(onMessage).toHaveBeenCalledWith(data);
        });

        describe('error logging', () => {
            test('should log errors if iframeLogger is enabled', () => {
                window.console.error = jest.fn();
                window.location.search = '?iframeLogger=1';

                wrapper = mount(<LcIframeComponent {...requiredProps} />);

                const frameSource = wrapper.find('iframe').getDOMNode();

                simulateMessageEvent({ data: 'bad json' }, frameSource);

                expect(window.console.error).toHaveBeenCalledTimes(1);
                expect(window.console.error).toHaveBeenCalledWith("Can't parse message data:", expect.any(SyntaxError));
            });

            test('should not log errors if iframeLogger is disabled', () => {
                window.console.error = jest.fn();
                window.location.search = '';

                wrapper = mount(<LcIframeComponent {...requiredProps} />);

                const frameSource = wrapper.find('iframe').getDOMNode();

                simulateMessageEvent({ data: 'bad json' }, frameSource);

                expect(window.console.error).toHaveBeenCalledTimes(0);
            });
        });

        test('should pass security props to iframe element', () => {
            wrapper = shallow(<LcIframeComponent {...requiredProps} src="https://evil.com" />);

            expect(wrapper.find('iframe').prop('sandbox')).toBe(
                'allow-forms allow-scripts allow-top-navigation allow-same-origin allow-presentation'
            );

            wrapper.setProps({ src: 'https://yandex.ru' });

            expect(wrapper.find('iframe').prop('sandbox')).toBeUndefined();
        });

        test('should set dynamic height', () => {
            wrapper = mount(<LcIframeComponent {...requiredProps} height={107} />);

            const instance = wrapper.instance();
            const setState = jest.spyOn(instance, 'setState');
            const frameSource = wrapper.find('iframe').getDOMNode();
            const data = { ['iframe-height']: 42, name: 'iframe-id' };

            simulateMessageEvent({ data: JSON.stringify(data) }, frameSource);

            expect(setState).toHaveBeenCalledTimes(1);
            expect(setState).toHaveBeenCalledWith({ dynamicHeight: 42 });
        });
    });

    describe('SSR', () => {
        test('should pass src to iframe element when deferIframeRender is false', () => {
            expect(renderToString(<LcIframeComponent {...requiredProps} />)).toMatchSnapshot();
        });

        test('should not pass src to iframe element when deferIframeRender is true', () => {
            expect(renderToString(<LcIframeComponent {...requiredProps} deferIframeRender />)).toMatchSnapshot();
        });
    });
});
