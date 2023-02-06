import * as React from 'react';
import { mount } from 'enzyme';

import { Item } from './ToolsSuggest';
import { ToolsSuggest } from './ToolsSuggest.bundle/desktop';

describe('ToolsSuggest@desktop', () => {
    it('Should render empty component', () => {
        const wrapper = mount(<ToolsSuggest />);

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });

    it('Should support Textinput / Input props', () => {
        const wrapper = mount(
            <ToolsSuggest
                view="default"
                theme="normal"
                size="m"
                pin="brick-brick"
                disabled
                name="FOO"
            />,
        );

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });

    it('Should support maxChosen', () => {
        const wrapper = mount(<ToolsSuggest maxChosen={1} chosen={[{ id: '1' }, { id: '2' }]} />);

        expect(wrapper.find('.ToolsSuggest-Chosen').length).toBe(1);

        wrapper.unmount();
    });

    it('Should render opened component', () => {
        const wrapper = mount(<ToolsSuggest opened />);

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });

    it('Should render empty choices', () => {
        const wrapper = mount(<ToolsSuggest choices={[]} />);

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });

    it('Should render loading component', () => {
        const wrapper = mount(<ToolsSuggest loading />);

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });

    it('Should render choices/chosen by default', () => {
        const wrapper = mount(<ToolsSuggest choices={[{ id: '1' }]} chosen={[{ id: '1' }]} />);

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });

    it('Should support Chosen/Choice render props', () => {
        function renderChoice({ id }: Item) {
            return `CHOICE-${id}`;
        }

        function renderChosen({ id }: Item) {
            return `CHOSEN-${id}`;
        }

        const wrapper = mount(
            <ToolsSuggest
                choices={[{ id: '1' }, { id: '2' }]}
                chosen={[{ id: '1' }, { id: '2' }]}
                renderChoice={renderChoice}
                renderChosen={renderChosen}
            />,
        );

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });

    it('Should support props.onValueChange', () => {
        const onValueChange = jest.fn();
        const wrapper = mount(<ToolsSuggest onValueChange={onValueChange} />);

        wrapper.find('input').simulate('change', {
            target: { value: 'Hello' },
        });

        expect(onValueChange).toHaveBeenCalledWith('Hello');

        wrapper.unmount();
    });

    it('Should call onOpenedChange(true) after value change', () => {
        const onOpenedChange = jest.fn();
        const wrapper = mount(<ToolsSuggest onOpenedChange={onOpenedChange} />);

        wrapper.find('input').simulate('change', {
            target: { value: 'Test' },
        });

        expect(onOpenedChange).toHaveBeenCalledWith(true);

        wrapper.unmount();
    });

    it('Should call props.onOpenedChange(true) on input mousedown', () => {
        const onOpenedChange = jest.fn();
        const wrapper = mount(<ToolsSuggest onOpenedChange={onOpenedChange} />);

        wrapper.find('input').simulate('mousedown');

        expect(onOpenedChange).toHaveBeenCalledWith(true);

        wrapper.unmount();
    });

    it('Should call props.onOpenedChange(true) on input focus', () => {
        const onOpenedChange = jest.fn();
        const wrapper = mount(<ToolsSuggest onOpenedChange={onOpenedChange} />);

        wrapper.find('input').simulate('focus');

        expect(onOpenedChange).toHaveBeenCalledTimes(1);
        expect(onOpenedChange).toHaveBeenCalledWith(true);

        wrapper.unmount();
    });

    it('Should call props.onOpenedChange(false) on input blur', () => {
        const onOpenedChange = jest.fn();
        const wrapper = mount(<ToolsSuggest opened onOpenedChange={onOpenedChange} />);

        wrapper.find('input').simulate('blur');

        expect(onOpenedChange).toHaveBeenCalledWith(false);

        wrapper.unmount();
    });

    it('Should call props.onPickedChange([]) if props.opened changes to false', () => {
        const onPickedChange = jest.fn();
        const wrapper = mount(<ToolsSuggest picked={[{ id: '1' }]} onPickedChange={onPickedChange} />);

        wrapper.find('input').simulate('blur');

        expect(onPickedChange).toHaveBeenCalledWith([]);

        wrapper.unmount();
    });

    it('Should not call props.onPickedChange picked is equal to previous', () => {
        const onPickedChange = jest.fn();
        const wrapper = mount(<ToolsSuggest picked={[]} onPickedChange={onPickedChange} />);

        wrapper.find('input').simulate('blur');

        expect(onPickedChange).not.toHaveBeenCalled();

        wrapper.unmount();
    });

    it('Should call props.onChosenChange() on &-ChosenRemove click', () => {
        const onChosenChange = jest.fn();
        const wrapper = mount(<ToolsSuggest chosen={[{ id: '1' }]} onChosenChange={onChosenChange} />);

        wrapper.find('.ToolsSuggest-ChosenRemove').simulate('click');

        expect(onChosenChange).toHaveBeenCalledWith([]);

        wrapper.unmount();
    });

    it('Should set props.onOpenedChange(true) on &-Arrow click if not opened', () => {
        const onOpenedChange = jest.fn();
        const wrapper = mount(<ToolsSuggest opened={false} onOpenedChange={onOpenedChange} />);

        wrapper.find('span.ToolsSuggest-Arrow').simulate('click');

        expect(onOpenedChange).toHaveBeenCalledWith(true);

        wrapper.unmount();
    });

    it('Should prevent &-Arrow mousedown', () => {
        const preventDefault = jest.fn();
        const wrapper = mount(<ToolsSuggest />);

        wrapper.find('span.ToolsSuggest-Arrow').simulate('mousedown', { preventDefault });

        expect(preventDefault).toHaveBeenCalled();

        wrapper.unmount();
    });

    it('Should set props.onOpenedChange(false) on &-Arrow click if opened', () => {
        const onOpenedChange = jest.fn();
        const wrapper = mount(<ToolsSuggest opened onOpenedChange={onOpenedChange} />);

        wrapper.find('span.ToolsSuggest-Arrow').simulate('click');

        expect(onOpenedChange).toHaveBeenCalledWith(false);

        wrapper.unmount();
    });

    it('Should call props.onOpenedChange(false) press Escape', () => {
        const eventMap: Record<string, EventListenerOrEventListenerObject> = {};
        document.addEventListener = jest.fn((event, cb) => {
            eventMap[event] = cb;
        });

        const onOpenedChange = jest.fn();
        const wrapper = mount(<ToolsSuggest opened onOpenedChange={onOpenedChange} />);

        // @ts-ignore
        eventMap.keyup({ key: 'Escape' });

        expect(onOpenedChange).toHaveBeenCalledWith(false);

        wrapper.unmount();
    });

    it('Should handle Enter press', () => {
        const onChosenChange = jest.fn();
        const onPickedChange = jest.fn();
        const onOpenedChange = jest.fn();
        const onValueChange = jest.fn();
        const wrapper = mount(
            <ToolsSuggest
                opened
                value="test"
                onChosenChange={onChosenChange}
                onOpenedChange={onOpenedChange}
                onPickedChange={onPickedChange}
                onValueChange={onValueChange}
                picked={[{ id: '1' }, { id: '2' }]}
            />,
        );

        wrapper.find('input').simulate('keydown', { key: 'Enter' });

        expect(onOpenedChange).toHaveBeenCalledWith(false);
        expect(onValueChange).toHaveBeenCalledWith('');
        expect(onPickedChange).toHaveBeenCalledWith([]);
        expect(onChosenChange).toHaveBeenCalledWith([{ id: '1' }, { id: '2' }]);

        wrapper.unmount();
    });

    it('Should handle Backspace press', () => {
        const onChosenChange = jest.fn();
        const onOpenedChange = jest.fn();
        const wrapper = mount(
            <ToolsSuggest
                opened
                value=""
                onChosenChange={onChosenChange}
                onOpenedChange={onOpenedChange}
                chosen={[{ id: '1' }, { id: '2' }]}
            />,
        );

        wrapper.find('input').simulate('keydown', { key: 'Backspace' });

        expect(onOpenedChange).toHaveBeenCalledWith(false);
        expect(onChosenChange).toHaveBeenCalledWith([{ id: '1' }]);

        wrapper.unmount();
    });

    it('Should not call props.onChosenChange if value is not empty', () => {
        const onChosenChange = jest.fn();
        const onOpenedChange = jest.fn();
        const wrapper = mount(
            <ToolsSuggest
                opened
                value="test"
                onChosenChange={onChosenChange}
                onOpenedChange={onOpenedChange}
                chosen={[{ id: '1' }, { id: '2' }]}
            />,
        );

        wrapper.find('input').simulate('keydown', { key: 'Backspace' });

        expect(onOpenedChange).not.toHaveBeenCalled();
        expect(onChosenChange).not.toHaveBeenCalled();

        wrapper.unmount();
    });

    it('Should not call props.onChosenChange if nothing changed after select', () => {
        const onChosenChange = jest.fn();
        const wrapper = mount(
            <ToolsSuggest
                opened
                onChosenChange={onChosenChange}
                chosen={[{ id: '1' }, { id: '2' }]}
                picked={[{ id: '1' }, { id: '2' }]}
            />,
        );

        wrapper.find('input').simulate('keydown', { key: 'Enter' });

        expect(onChosenChange).not.toHaveBeenCalled();

        wrapper.unmount();
    });

    it('Should handle ArrowDown', () => {
        const onPickedChange = jest.fn();
        const onOpenedChange = jest.fn();
        const wrapper = mount(
            <ToolsSuggest
                onOpenedChange={onOpenedChange}
                onPickedChange={onPickedChange}
                choices={[{ id: '1' }, { id: '2' }]}
                picked={[{ id: '1' }]}
            />,
        );

        wrapper.find('input').simulate('keydown', { key: 'ArrowDown' });

        expect(onOpenedChange).toHaveBeenCalledWith(true);
        expect(onPickedChange).toHaveBeenCalledWith([{ id: '2' }]);

        wrapper.unmount();
    });

    it('Should handle ArrowDown with shiftKey', () => {
        const onPickedChange = jest.fn();
        const wrapper = mount(
            <ToolsSuggest
                onPickedChange={onPickedChange}
                choices={[{ id: '1' }, { id: '2' }]}
                picked={[{ id: '1' }]}
            />,
        );

        wrapper.find('input').simulate('keydown', {
            key: 'ArrowDown',
            shiftKey: true,
        });

        expect(onPickedChange).toHaveBeenCalledWith([{ id: '1' }, { id: '2' }]);

        wrapper.unmount();
    });

    it('Should handle ArrowUp', () => {
        const onPickedChange = jest.fn();
        const onOpenedChange = jest.fn();
        const wrapper = mount(
            <ToolsSuggest
                onOpenedChange={onOpenedChange}
                onPickedChange={onPickedChange}
                choices={[{ id: '1' }, { id: '2' }]}
                picked={[{ id: '2' }]}
            />,
        );

        wrapper.find('input').simulate('keydown', { key: 'ArrowUp' });

        expect(onOpenedChange).toHaveBeenCalledWith(true);
        expect(onPickedChange).toHaveBeenCalledWith([{ id: '1' }]);

        wrapper.unmount();
    });

    it('Should handle ArrowUp with shiftKey', () => {
        const onPickedChange = jest.fn();
        const wrapper = mount(
            <ToolsSuggest
                onPickedChange={onPickedChange}
                choices={[{ id: '1' }, { id: '2' }]}
                picked={[{ id: '2' }]}
            />,
        );

        wrapper.find('input').simulate('keydown', {
            key: 'ArrowUp',
            shiftKey: true,
        });

        expect(onPickedChange).toHaveBeenCalledWith([{ id: '2' }, { id: '1' }]);

        wrapper.unmount();
    });

    it('Should handle &-Choice mousedown', () => {
        const onOpenedChange = jest.fn();
        const onChosenChange = jest.fn();
        const wrapper = mount(
            <ToolsSuggest
                opened
                onOpenedChange={onOpenedChange}
                onChosenChange={onChosenChange}
                choices={[{ id: '1' }, { id: '2' }]}
            />,
        );

        wrapper
            .find('.ToolsSuggest-Choice')
            .at(0)
            .simulate('mousedown');

        expect(onChosenChange).toHaveBeenCalledWith([{ id: '1' }]);
        expect(onOpenedChange).toHaveBeenCalledWith(false);

        wrapper.unmount();
    });

    it('Should handle &-Choice mousedown with .ctrlKey', () => {
        const onOpenedChange = jest.fn();
        const onChosenChange = jest.fn();
        const onPickedChange = jest.fn();
        const wrapper = mount(
            <ToolsSuggest
                opened
                onOpenedChange={onOpenedChange}
                onChosenChange={onChosenChange}
                onPickedChange={onPickedChange}
                picked={[{ id: '3' }]}
                choices={[{ id: '1' }, { id: '2' }]}
            />,
        );

        wrapper
            .find('.ToolsSuggest-Choice')
            .at(0)
            .simulate('mousedown', {
                ctrlKey: true,
            });

        expect(onChosenChange).not.toHaveBeenCalled();
        expect(onOpenedChange).not.toHaveBeenCalled();
        expect(onPickedChange).toHaveBeenCalledWith([{ id: '3' }, { id: '1' }]);

        wrapper.unmount();
    });

    it('Should handle &-Choice mousedown with .ctrlKey (unpick)', () => {
        const onOpenedChange = jest.fn();
        const onChosenChange = jest.fn();
        const onPickedChange = jest.fn();
        const wrapper = mount(
            <ToolsSuggest
                opened
                onOpenedChange={onOpenedChange}
                onChosenChange={onChosenChange}
                onPickedChange={onPickedChange}
                picked={[{ id: '1' }]}
                choices={[{ id: '1' }, { id: '2' }]}
            />,
        );

        wrapper
            .find('.ToolsSuggest-Choice')
            .at(0)
            .simulate('mousedown', {
                ctrlKey: true,
            });

        expect(onChosenChange).not.toHaveBeenCalled();
        expect(onOpenedChange).not.toHaveBeenCalled();
        expect(onPickedChange).toHaveBeenCalledWith([]);

        wrapper.unmount();
    });

    it('Should handle &-Choices scroll', () => {
        const onChoicesScroll = jest.fn();
        const wrapper = mount(
            <ToolsSuggest opened onChoicesScroll={onChoicesScroll} choices={[{ id: '1' }, { id: '2' }]} />,
        );

        wrapper.find('.ToolsSuggest-Choices').simulate('scroll');

        expect(onChoicesScroll).toHaveBeenCalled();
        wrapper.unmount();
    });

    it('Should hide arrow when noArrow paramater passed', () => {
        const wrapper = mount(<ToolsSuggest opened noArrow />);

        expect(wrapper.find('.ToolsSuggest-Arrow')).toHaveLength(0);
    });

    it('Should show SetValue button with buttonValue prop', () => {
        const wrapper = mount(
            <ToolsSuggest
                buttonValue={{ id: '1' }}
                buttonValueChildren="Set value"
            />,
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('Should handle &-SetValue click', () => {
        const onChosenChange = jest.fn();
        const wrapper = mount(
            <ToolsSuggest
                buttonValue={{ id: '1' }}
                buttonValueChildren="Set value"
                onChosenChange={onChosenChange}
            />,
        );

        wrapper
            .find('.ToolsSuggest-SetValue')
            .at(0)
            .simulate('click');

        expect(onChosenChange).toHaveBeenCalledWith([{ id: '1' }]);

        wrapper.unmount();
    });

    describe('Excluding groups', () => {
        const createExcludingGroup = (item: Item) => {
            const matches = /^group(\d+)-/.exec(String(item.id));

            if (!matches) {
                return;
            }

            return matches[1];
        };
        const group1Items: Item[] = [
            { id: 'group1-1' },
            { id: 'group1-2' },
        ];
        const group2Items: Item[] = [
            { id: 'group2-1' },
            { id: 'group2-2' },
        ];
        const otherItems: Item[] = [
            { id: 'item-1' },
            { id: 'item-2' },
        ];
        const mountWithExcludingGroups = (
            onChosenChange: (choices: Item[]) => void,
            chosen: Item[],
            restorableChosen?: boolean,
        ) => mount(
            <ToolsSuggest
                opened
                onChosenChange={onChosenChange}
                createExcludingGroup={createExcludingGroup}
                choices={chosen}
                chosen={chosen}
                restorableChosen={restorableChosen}
            />,
        );

        it('Should allow only one item from group', () => {
            const onChosenChange = jest.fn();
            const wrapper = mountWithExcludingGroups(onChosenChange, [...group1Items]);

            expect(onChosenChange).toHaveBeenLastCalledWith([group1Items[1]]);

            wrapper.unmount();
        });

        it('Should allow items not from groups', () => {
            const onChosenChange = jest.fn();
            const wrapper = mountWithExcludingGroups(onChosenChange, [...group1Items, ...otherItems]);

            expect(onChosenChange).toHaveBeenLastCalledWith([group1Items[1], ...otherItems]);

            wrapper.unmount();
        });

        it('Should allow more than one group', () => {
            const onChosenChange = jest.fn();
            const wrapper = mountWithExcludingGroups(onChosenChange, [...group1Items, ...group2Items]);

            expect(onChosenChange).toHaveBeenLastCalledWith([group1Items[1], group2Items[1]]);

            wrapper.unmount();
        });

        it('Should allow to restore removed item', () => {
            const onChosenChange = jest.fn();
            const wrapper = mountWithExcludingGroups(onChosenChange, [...group1Items], true);

            expect(onChosenChange).toHaveBeenLastCalledWith([group1Items[1]]);

            const removedChosen = wrapper.find('.ToolsSuggest-Chosen_removed');

            expect(removedChosen.exists()).toBe(true);
            expect(removedChosen.find('.ToolsSuggest-ChosenContent').text()).toBe(group1Items[0].id);

            wrapper.unmount();
        });

        it('Should remove other group item when restoring the first one', () => {
            const chosen = [...group1Items];
            const onChosenChange = jest.fn((items: Item[]) => {
                chosen.length = 0;
                chosen.push(...items);
            });
            const wrapper = mountWithExcludingGroups(onChosenChange, chosen, true);

            expect(onChosenChange).toHaveBeenLastCalledWith([group1Items[1]]);

            wrapper
                .find('.ToolsSuggest-ChosenRestore')
                .at(0)
                .simulate('click');

            expect(onChosenChange).toHaveBeenLastCalledWith([group1Items[0]]);

            wrapper.unmount();
        });
    });
});
