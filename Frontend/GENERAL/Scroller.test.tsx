import * as React from 'react';
import { shallow, mount } from 'enzyme';

import { ScrollerBase as Scroller } from './Scroller';
import { ScrollerItem } from './Item/Scroller-Item';
import { Text } from '../Text';
import { ScrollerArrow } from './Arrow/Scroller-Arrow';

describe('Scroller', () => {
    const items = (
        <>
            <ScrollerItem index={1}>
                <Text>Текст 1</Text>
            </ScrollerItem>
            <ScrollerItem index={2}>
                <Text>Текст 2</Text>
            </ScrollerItem>
            <ScrollerItem index={3}>
                <Text>Текст 3</Text>
            </ScrollerItem>
        </>
    );

    it('Должен корректно отрендериться компонент скроллера', () => {
        const component = shallow(
            <Scroller>
                {items}
            </Scroller>,
        );

        expect(component).toMatchSnapshot();
    });

    it('Должен корректно отрендериться компонент скроллера с двумя стрелками', () => {
        const component = shallow(
            <Scroller>
                {items}
            </Scroller>,
        );
        component.setState({ scrollDirection: 'both' });

        expect(component).toMatchSnapshot();
    });

    it('Должны обрабатываться клики по стрелкам', () => {
        const cb = jest.fn();
        const component = mount(
            <Scroller onClickArrow={cb}>
                {items}
            </Scroller>,
        );
        component.setState({ scrollDirection: 'both' });

        component.find(ScrollerArrow).first().simulate('click');

        expect(cb).toHaveBeenCalledWith('left');

        component.find(ScrollerArrow).last().simulate('click');

        expect(cb).toHaveBeenCalledWith('right');
        expect(cb).toHaveBeenCalledTimes(2);
    });
});
