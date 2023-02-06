import React, { Component } from 'react';
import { act } from 'react-dom/test-utils';
import { mount } from 'enzyme';
import { CopyToClipboard } from '~/src/common/components/CopyToClipboard/CopyToClipboard';

jest.mock('@yandex-lego/components/Tooltip/desktop', () => ({
    Tooltip: (props: Object) => <div className="mockTooltip" {...props} />,
    withSizeS: (WrappedComponent: Component) => WrappedComponent,
    withViewDefault: (WrappedComponent: Component) => WrappedComponent,
}));

jest.mock('copy-to-clipboard', () => ({
    __esModule: true,
    copy: (value: unknown) => {
        return (typeof value === 'string');
    },
    default: (value: unknown) => {
        return (typeof value === 'string');
    },
}));

describe('CopyToClipboard', () => {
    const getWrapper = () => mount(
        <CopyToClipboard
            text="some text to copy"
            tooltipTimeout={1000}
            hint="some hint"
        />,
    );

    beforeEach(() => {
        jest.useFakeTimers();
    });

    it('should render simple copy button', () => {
        const wrapper = getWrapper();
        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('should show tooltip on copy success', () => {
        const wrapper = getWrapper();
        wrapper.find('.Button2').hostNodes().simulate('click');

        const tooltip = wrapper.find('.mockTooltip[visible=true]');
        expect(tooltip.prop('state')).toBe('success');
        expect(tooltip.text()).toBe('i18n:tooltip-success');

        act(() => { jest.runAllTimers() });
        wrapper.update();

        expect(wrapper.exists('.mockTooltip[visible=true]')).toBeFalsy();
        wrapper.unmount();
    });

    it('should show tooltip on copy failure', () => {
        const wrapper = mount(
            <CopyToClipboard
                // @ts-ignore явно вызываем ошибку, передавая неправильный тип данных
                text={{ foo: 'bar' }}
                tooltipTimeout={1000}
            />,
        );
        wrapper.find('.Button2').hostNodes().simulate('click');

        const tooltip = wrapper.find('.mockTooltip[visible=true]');
        expect(tooltip.prop('state')).toBe('alert');
        expect(tooltip.text()).toBe('i18n:tooltip-alert');

        act(() => { jest.runAllTimers() });
        wrapper.update();

        expect(wrapper.exists('.mockTooltip[visible=true]')).toBeFalsy();
        wrapper.unmount();
    });

    it('should show tooltip after second button click', () => {
        const wrapper = getWrapper();
        const clickButton = () => wrapper.find('.Button2').hostNodes().simulate('click');

        clickButton();
        expect(wrapper.exists('.mockTooltip[visible=true]')).toBeTruthy();

        act(() => { jest.runAllTimers() });
        wrapper.update();

        expect(wrapper.exists('.mockTooltip[visible=true]')).toBeFalsy();
        clickButton();
        expect(wrapper.exists('.mockTooltip[visible=true]')).toBeTruthy();

        wrapper.unmount();
    });

    it('should show hint on mouse enter', () => {
        const wrapper = getWrapper();

        wrapper.find('.Button2').hostNodes().simulate('mouseenter');
        act(() => { jest.runAllTimers() });
        wrapper.update();

        const tooltip = wrapper.find('.mockTooltip[visible=true]');
        expect(tooltip.prop('state')).toBeUndefined();
        expect(tooltip.text()).toBe('some hint');

        wrapper.unmount();
    });

    it('should hide hint on mouse leave', () => {
        const wrapper = getWrapper();

        wrapper.find('.Button2').hostNodes().simulate('mouseenter');
        act(() => { jest.runAllTimers() });
        wrapper.update();

        wrapper.find('.Button2').hostNodes().simulate('mouseleave');
        act(() => { jest.runAllTimers() });
        wrapper.update();

        expect(wrapper.exists('.mockTooltip[visible=true]')).toBeFalsy();

        wrapper.unmount();
    });

    it('should show tooltip on mouse enter followed by copy success', () => {
        const wrapper = getWrapper();

        wrapper.find('.Button2').hostNodes().simulate('mouseenter');
        act(() => { jest.runAllTimers() });
        wrapper.update();

        wrapper.find('.Button2').hostNodes().simulate('click');

        const tooltip = wrapper.find('.mockTooltip[visible=true]');
        expect(tooltip.prop('state')).toBe('success');
        expect(tooltip.text()).toBe('i18n:tooltip-success');

        wrapper.unmount();
    });
});
