import * as React from 'react';
import { shallow, mount } from 'enzyme';
import { LockFrameSwipe } from '../LockFrameSwipe';

describe('LockFrameSwipe', () => {
    it('Рендерится без ошибок', () => {
        expect(() => shallow(
            <LockFrameSwipe>
                <div />
            </LockFrameSwipe>
        )).not.toThrowError();
    });

    it('Препятствует всплытию события touchstart', () => {
        const touchStartSpy = jest.fn();

        const wrapper = mount(
            <div onTouchStart={touchStartSpy}>
                <LockFrameSwipe>
                    <div className="for-test" />
                </LockFrameSwipe>
            </div>
        );

        wrapper.find('div.for-test').simulate('touchstart');

        expect(touchStartSpy).not.toHaveBeenCalled();
    });
});
