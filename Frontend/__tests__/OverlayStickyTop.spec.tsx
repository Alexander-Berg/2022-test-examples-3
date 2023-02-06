import * as React from 'react';
import { shallow } from 'enzyme';
import { OverlayStickyTop } from '../OverlayStickyTop';
import { OverlayStickyMenuState } from '../OverlayStickyTop.types';

describe('OverlayStickyTop', () => {
    let _addEventListener = window.addEventListener;

    beforeAll(() => {
        _addEventListener = window.addEventListener;
    });

    afterAll(() => {
        window.addEventListener = _addEventListener;
    });

    test('Должен соответствовать снэпшоту в состоянии "open"', () => {
        const wrapper = shallow(<OverlayStickyTop />);
        wrapper.setState({
            overlayMenuState: OverlayStickyMenuState.OPEN,
        });
        expect(wrapper).toMatchSnapshot();
    });

    test('Должен соответствовать снэпшоту в состоянии "close"', () => {
        const wrapper = shallow(<OverlayStickyTop />);
        wrapper.setState({
            overlayMenuState: OverlayStickyMenuState.CLOSE,
        });
        expect(wrapper).toMatchSnapshot();
    });

    test('Начальный state должен быть "open"', () => {
        const wrapper = shallow(<OverlayStickyTop />);
        expect(wrapper.state('overlayMenuState')).toEqual(OverlayStickyMenuState.OPEN);
    });

    test('State должен изменяться по postMessage из состояния "open" в "close"', () => {
        const map: Record<string, EventListenerOrEventListenerObject> = {};
        window.addEventListener = jest.fn((event, cb) => {
            map[event] = cb;
        });

        const wrapper = shallow(<OverlayStickyTop />);
        wrapper.setState({
            overlayMenuState: OverlayStickyMenuState.OPEN,
        });

        expect(map.message).not.toBe(undefined);
        const fireEventHandler = typeof map.message === 'function' ? map.message : map.message.handleEvent;
        const event = new MessageEvent('message', {
            data: {
                action: 'overlay-menu',
                state: OverlayStickyMenuState.CLOSE,
            },
        });
        fireEventHandler(event);

        expect(wrapper.state('overlayMenuState')).toEqual(OverlayStickyMenuState.CLOSE);
    });

    test('State должен изменяться по postMessage из состояния "close" в "open"', () => {
        const map: Record<string, EventListenerOrEventListenerObject> = {};
        window.addEventListener = jest.fn((event, cb) => {
            map[event] = cb;
        });

        const wrapper = shallow(<OverlayStickyTop />);
        wrapper.setState({
            overlayMenuState: OverlayStickyMenuState.CLOSE,
        });

        expect(map.message).not.toBe(undefined);
        const fireEventHandler = typeof map.message === 'function' ? map.message : map.message.handleEvent;
        const event = new MessageEvent('message', {
            data: {
                action: 'overlay-menu',
                state: OverlayStickyMenuState.OPEN,
            },
        });
        fireEventHandler(event);

        expect(wrapper.state('overlayMenuState')).toEqual(OverlayStickyMenuState.OPEN);
    });

    test('Должен добавляться event listener на mount и удаляться на unmount', () => {
        const map: Record<string, EventListenerOrEventListenerObject> = {};
        window.addEventListener = jest.fn((event, cb) => {
            map[event] = cb;
        });
        window.removeEventListener = jest.fn((event, cb) => {
            if (cb === map[event]) {
                delete map[event];
            }
        });

        const wrapper = shallow(<OverlayStickyTop />);

        expect(map.message).not.toBe(undefined);

        wrapper.unmount();

        expect(map.message).toBe(undefined);
    });

    test('Должен рендерить переданные children', () => {
        const MyComponent: React.FunctionComponent = () => null;
        const wrapper = shallow(
            <OverlayStickyTop>
                <MyComponent />
            </OverlayStickyTop>
        );

        expect(wrapper.find(MyComponent).length).toBe(1);
    });
});
