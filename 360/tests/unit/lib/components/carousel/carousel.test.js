import { mount } from 'enzyme';
import React from 'react';
import SwitchArrowButton from '../../../../../lib/components/slider/switch-arrow-button';
import Carousel from '../../../../../lib/components/carousel';

describe('Carousel', () => {
    const originalWindowAddEventListener = window.addEventListener;

    window.addEventListener = jest.fn();

    beforeEach(() => {
        window.addEventListener.mockClear();
    });

    afterAll(() => {
        window.addEventListener = originalWindowAddEventListener;
    });

    it('should call scrollBy function on arrow click', () => {
        const scrollStep = 120;
        const wrapper = mount(
            <Carousel scrollStep={scrollStep}>
                {[]}
            </Carousel>
        );
        const carousel = wrapper.instance()._carousel;

        carousel.scrollBy = jest.fn();
        wrapper.find(SwitchArrowButton).at(1).simulate('click');
        expect(carousel.scrollBy).toBeCalledWith(
            expect.objectContaining({
                left: scrollStep,
                behavior: 'smooth'
            }));
        wrapper.find(SwitchArrowButton).at(0).simulate('click');
        expect(carousel.scrollBy).toBeCalledWith(
            expect.objectContaining({
                left: -scrollStep,
                behavior: 'smooth'
            }));
    });

    it('should match snapshot with mixed cls, hidden-scrollbar and arrow buttons', () => {
        const wrapper = mount(
            <Carousel cls="mixed-class" hideScrollbar>
                {[]}
            </Carousel>
        );

        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should match snapshot with non-hidden-scrollbar', () => {
        const wrapper = mount(
            <Carousel hideScrollbar={false}>
                {[]}
            </Carousel>
        );

        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should add window resize listener if `shouldListenToWindowResize === true`', () => {
        mount(
            <Carousel shouldListenToWindowResize hideScrollbar>
                {[]}
            </Carousel>
        );
        expect(window.addEventListener.mock.calls.find((call) => call[0] === 'resize')).not.toBe(undefined);
    });

    it('should not add window resize listener if `shouldListenToWindowResize === false`', () => {
        mount(
            <Carousel shouldListenToWindowResize={false} hideScrollbar>
                {[]}
            </Carousel>
        );
        expect(window.addEventListener.mock.calls.find((call) => call[0] === 'resize')).toBe(undefined);
    });
});
