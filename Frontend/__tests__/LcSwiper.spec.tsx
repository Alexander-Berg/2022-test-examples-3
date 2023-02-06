import * as React from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { noop } from 'lodash';
import { LcSwiper } from '../LcSwiper';

function renderSwiper(props: object = {}) {
    const defaultProps = {
        children: [
            <div key="0" id="0" className="first-child">1</div>,
            <div key="1" id="1" className="second-child">2</div>,
            <div key="2" id="2" className="third-child">3</div>,
        ],
        options: {},
    };
    const mergedProps = { ...defaultProps, ...props };

    return mount<LcSwiper>(<LcSwiper {...mergedProps} />);
}

type SwiperCore = {
    activeIndex: number;
    slides: unknown[];
    emit: (event: string) => void;
    autoplay: {
        run: () => void;
        running: boolean;
    };
    slidePrev(): void;
    slideNext(): void;
    slideToLoop(index: number, time?: number): void;
};

describe('<LcSwiper /> section', () => {
    test('should render children with data-hash', () => {
        const sectionIds = ['foo', 'bar', 'baz'];
        const props = {
            children: sectionIds.map(key => (
                // @ts-ignore
                <div key={key} anchor={key}>
                    {key}
                </div>
            )),
        };
        const component = renderSwiper(props);
        const dataHashes = component.find('.swiper-slide').map(slide => slide.prop('data-hash'));

        expect(dataHashes).toEqual(sectionIds);
    });

    test('should disable autoscroll when mouseenter and enable when mouseleave', () => {
        const props = {
            options: { autoplay: { delay: 300 } },
        };
        const component = renderSwiper(props);
        const instance = component.instance();

        if (instance && instance.swiper && instance.swiper.autoplay) {
            instance.swiper.autoplay.start();
            expect(instance.swiper.autoplay.running).toEqual(true);

            component.find('.slider-container').simulate('mouseEnter');
            expect(instance.swiper.autoplay.running).toEqual(false);

            component.find('.slider-container').simulate('mouseLeave');
            expect(instance.swiper.autoplay.running).toEqual(true);
        }
    });

    test('should disable autoscroll when touchstart and enable when touchend', () => {
        const props = {
            options: { autoplay: { delay: 300 } },
            isTouch: true,
        };
        const component = renderSwiper(props);
        const instance = component.instance();

        if (instance && instance.swiper && instance.swiper.autoplay) {
            instance.swiper.autoplay.start();
            expect(instance.swiper.autoplay.running).toEqual(true);

            component.find('.slider-container').simulate('touchStart');
            expect(instance.swiper.autoplay.running).toEqual(false);

            component.find('.slider-container').simulate('touchEnd');
            expect(instance.swiper.autoplay.running).toEqual(true);
        }
    });

    test('should change slides', () => {
        const component = renderSwiper();
        const instance = component.instance();

        instance.swiper.slideNext();

        expect(instance.swiper.activeIndex).toEqual(1);

        instance.swiper.slideNext();

        expect(instance.swiper.activeIndex).toEqual(2);
    });

    describe('Loop = true', () => {
        let currentMutationObserverCallback: (arg?: unknown) => void = noop;

        beforeEach(() => {
            // @ts-ignore
            global.MutationObserver = class {
                constructor(callback: (arg: unknown) => void) {
                    currentMutationObserverCallback = callback;
                }

                disconnect() {}

                observe() {}
            };

            jest.useFakeTimers();
        });

        afterEach(() => {
            // @ts-ignore
            delete global.MutationObserver;
            currentMutationObserverCallback = noop;
        });

        test('should update innerHTML of duplicated slide if original slide was changed', () => {
            const props = {
                mix: 'test',
                options: { loop: true },
            };
            const component = renderSwiper(props);

            component
                .find('.first-child')
                .getDOMNode()
                .classList.add('test-class-name');

            currentMutationObserverCallback();

            // @ts-ignore
            component.instance().swiper.emit('touchMove');

            jest.runAllTimers();

            expect(component.html()).toMatchSnapshot();
        });

        test('should slide to original slide from duplicated', () => {
            const props = {
                mix: 'test',
                options: { loop: true },
            };

            const component = (renderSwiper(props) as unknown) as {
                instance: () => {
                    swiper: SwiperCore;
                    handleSlideChange: () => {};
                    replaceDuplicatedByOriginalSlide: () => void;
                };
            } & ReactWrapper;

            const instance = component.instance();
            const swiper = instance.swiper;
            const slideToLoopSpy = jest.spyOn(swiper, 'slideToLoop');

            swiper.slidePrev();
            swiper.slides.length = 5;
            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
            component
                .getDOMNode()
                .querySelector('.swiper-slide-duplicate')!
                .classList.add('swiper-slide-active');

            instance.replaceDuplicatedByOriginalSlide();
            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
            component
                .getDOMNode()
                .querySelector('.swiper-slide-duplicate')!
                .classList.remove('swiper-slide-active');

            jest.runAllTimers();

            expect(slideToLoopSpy).toHaveBeenCalledWith(2, 0);
        });
    });
});
