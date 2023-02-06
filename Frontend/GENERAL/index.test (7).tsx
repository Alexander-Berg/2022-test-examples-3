import * as React from 'react';
import { fireEvent, render } from '@testing-library/react';

import { Scrollbar, MIN_THUMB_HEIGHT } from '.';

const onMove = jest.fn();

describe('Scrollbar', () => {
    afterEach(() => {
        onMove.mockReset();
    });

    it('should have default classname', () => {
        const wrapper = render(
            <Scrollbar onMove={onMove} />,
        );

        expect(wrapper.baseElement.children[0].children[0]).toHaveClass('ui-scrollbar');
    });

    it('should have "dragging" modifier in the classname when iteraction started', () => {
        const wrapper = render(
            <Scrollbar onMove={onMove} />,
        );

        fireEvent.mouseDown(wrapper.baseElement.querySelector('.ui-scrollbar__thumb') as Element, { clientY: 0 });

        expect(wrapper.baseElement.children[0].children[0]).toHaveClass('ui-scrollbar_dragging');
    });

    it('should remove "dragging" modifier in the classname when iteraction ended', () => {
        const wrapper = render(
            <Scrollbar onMove={onMove} />,
        );

        fireEvent.mouseDown(wrapper.baseElement.querySelector<Element>('.ui-scrollbar__thumb') as Element, { clientY: 0 });

        fireEvent.mouseUp(document);

        expect(wrapper.baseElement.children[0].children[0]).not.toHaveClass('ui-scrollbar_dragging');
    });

    it('should add event listeners to the document', () => {
        const addEventListenerSpy = jest.spyOn(document, 'addEventListener');

        const wrapper = render(
            <Scrollbar onMove={onMove} />,
        );

        fireEvent.mouseDown(wrapper.baseElement.querySelector('.ui-scrollbar__thumb') as Element, { clientY: 0 });

        expect(addEventListenerSpy).toBeCalledWith('mousemove', expect.anything(), false);
        expect(addEventListenerSpy).toBeCalledWith('mouseup', expect.anything(), false);

        addEventListenerSpy.mockRestore();
    });

    it('should remove event listeners from the document', () => {
        const removeEventListenerSpy = jest.spyOn(document, 'removeEventListener');

        const wrapper = render(
            <Scrollbar onMove={onMove} />,
        );

        fireEvent.mouseUp(document);
        wrapper.unmount();

        expect(removeEventListenerSpy).toBeCalledWith('mousemove', expect.anything(), false);
        expect(removeEventListenerSpy).toBeCalledWith('mouseup', expect.anything(), false);

        removeEventListenerSpy.mockRestore();
    });

    it('should call "onMove" callback on mouse move', () => {
        const wrapper = render(
            <Scrollbar onMove={onMove} />,
        );

        fireEvent.mouseDown(wrapper.baseElement.querySelector('.ui-scrollbar__thumb') as Element, { clientY: 0 });

        const deltaY = 20;

        document.dispatchEvent(new MouseEvent('mousemove', { clientY: deltaY }));

        expect(onMove).toBeCalledWith(deltaY);
    });

    it('should hide scrollbar track if is nothing to scroll', () => {
        const scrollbarRef = React.createRef<Scrollbar>();
        render(<Scrollbar onMove={onMove} ref={scrollbarRef} />);
        scrollbarRef.current?.adjust(50, 100, 0);
        expect(scrollbarRef.current?.trackRef.current?.style.visibility).toEqual('hidden');
    });

    it('should show scrollbar track if is able to scroll', () => {
        const scrollbarRef = React.createRef<Scrollbar>();
        render(<Scrollbar onMove={onMove} ref={scrollbarRef} />);
        scrollbarRef.current?.adjust(1000, 100, 0);
        expect(scrollbarRef.current?.trackRef.current?.style.visibility).toEqual('visible');
    });

    it('should have min thumb height', () => {
        const scrollbarRef = React.createRef<Scrollbar>();
        render(<Scrollbar onMove={onMove} ref={scrollbarRef} />);

        const viewportHeight = 400;

        const thumbRef = scrollbarRef.current?.thumbRef.current!;
        const trackRef = scrollbarRef.current?.trackRef.current!;

        Object.defineProperty(trackRef, 'clientHeight', {
            value: viewportHeight - 4, // 4px padding
        });

        scrollbarRef.current?.adjust(400000, viewportHeight, 0);

        expect(thumbRef.style.height).toEqual(`${MIN_THUMB_HEIGHT}px`);
    });

    it('should call "onMove" callback on click to track', () => {
        const wrapper = render(
            <Scrollbar onMove={onMove} />,
        );

        fireEvent.click(wrapper.baseElement.querySelector('.ui-scrollbar__track') as Element, { clientY: 10 });

        expect(onMove).toBeCalled();
    });

    it('should call "onMove" callback on click to thumb', () => {
        const wrapper = render(
            <Scrollbar onMove={onMove} />,
        );

        fireEvent.click(wrapper.baseElement.querySelector('.ui-scrollbar__thumb') as Element, { clientY: 10 });

        expect(onMove).not.toBeCalled();
    });
});
