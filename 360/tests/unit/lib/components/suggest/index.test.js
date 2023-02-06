import React from 'react';
import { mount } from 'enzyme';

import Suggest from '../../../../../lib/components/suggest';
import { KEYS } from '../../../../../lib/consts';
import SearchInput from '../../../../../lib/components/suggest/search-input';

describe('suggest', () => {
    const mainProps = {
        onTextChange: jest.fn(),
        renderItem: jest.fn(),
        getShowText: jest.fn(),
        handleItemSelect: jest.fn(),
        onSubmit: jest.fn(),
        onBubblesRemove: jest.fn()
    };

    const map = {};
    window.addEventListener = jest.fn((event, callback) => {
        map[event] = callback;
    });

    it('should trigger startSuggest by input focus', () => {
        const startSuggest = jest.fn(() => {});
        const props = { startSuggest, ...mainProps };
        const wrapper = mount(<Suggest {...props} />);
        wrapper.find('lego-components_textinput').first().simulate('focus');
        expect(startSuggest).toHaveBeenCalledTimes(1);
        expect(startSuggest).toHaveBeenCalledWith('click');
    });

    it('should trigger startSuggest by hotkey', () => {
        const startSuggest = jest.fn(() => {});
        const props = { startSuggest, ...mainProps };

        const wrapper = mount(
            <div>
                <div id="keydown-target"></div>
                <Suggest {...props} />
            </div>
        );

        map.keydown({
            key: '/',
            target: wrapper.find('#keydown-target').getDOMNode(),
            stopPropagation: () => {},
            preventDefault: () => {}
        });
        expect(startSuggest).toHaveBeenCalledTimes(1);
        expect(startSuggest).toHaveBeenCalledWith('hotkey');
    });

    describe('should not trigger startSuggest by hotkey if keydown triggered =>', () => {
        const configurations = [
            {
                name: 'inside input',
                dom: <input id="keydown-target"/>
            },
            {
                name: 'inside element with "js-no-suggest-hotkey" class',
                dom: <div id="keydown-target" className="js-no-suggest-hotkey"/>
            },
            {
                name: 'inside children of element with "js-no-suggest-hotkey" class',
                dom:
                    <div className="js-no-suggest-hotkey">
                        <div id="keydown-target">
                        </div>
                    </div>
            }
        ];

        configurations.forEach((config) => {
            it(config.name, () => {
                const startSuggest = jest.fn(() => {});
                const props = { startSuggest, ...mainProps };
                const wrapper = mount(
                    <div>
                        { config.dom }
                        <Suggest {...props} />
                    </div>);
                map.keydown({
                    key: '/',
                    target: wrapper.find('#keydown-target').getDOMNode(),
                    stopPropagation: () => {},
                    preventDefault: () => {}
                });
                expect(startSuggest).toHaveBeenCalledTimes(0);
            });
        });
    });

    const createEvent = (params) => ({
        ...params,
        stopPropagation: jest.fn(),
        preventDefault: jest.fn()
    });

    describe('Выбор бабблов', () => {
        const bubbles = [
            { id: 'id1', content: 'баббл1' },
            { id: 'id2', content: 'баббл2' },
            { id: 'id3', content: 'баббл3' },
            { id: 'id4', content: 'баббл4' },
            { id: 'id5', content: 'баббл5' },
            { id: 'id6', content: 'баббл6' }
        ];
        const wrapper = mount(<Suggest { ...mainProps } bubbles={ bubbles }/>);
        const instance = wrapper.instance();
        instance._onInputFocus();

        it('По умолчанию бабблы не должны быть выбраны', () => {
            expect(instance.state.selectedBubblesIndexes).toEqual([]);
        });

        it('При отсутсвии выделенных бабблов нажатие стрелки вправо не должно ничего выделять', () => {
            instance._onKeyDown({ keyCode: KEYS.RIGHT });
            expect(wrapper.instance().state.selectedBubblesIndexes).toEqual([]);
        });

        it('При отсутсвии выделенных бабблов нажатие стрелки влево или бекспейса должно выделять последний баббл',
            () => {
                wrapper.find(SearchInput).instance()._onKeyDown({
                    keyCode: KEYS.LEFT,
                    target: {
                        selectionStart: 0,
                        selectionEnd: 0
                    }
                });
                expect(wrapper.instance().state.selectedBubblesIndexes).toEqual([5]);
            }
        );

        it('При выбранном баббле выделение должно двигаться стрелками', () => {
            instance._inputFocused = false;
            instance._onKeyDown(createEvent({ keyCode: KEYS.LEFT }));
            expect(wrapper.instance().state.selectedBubblesIndexes).toEqual([4]);
            instance._onKeyDown(createEvent({ keyCode: KEYS.RIGHT }));
            expect(wrapper.instance().state.selectedBubblesIndexes).toEqual([5]);
        });

        it('При выбранном баббле выделение c шифтом должно выдедять диапазон', () => {
            instance._onKeyDown(createEvent({ keyCode: KEYS.LEFT, shiftKey: true }));
            expect(wrapper.instance().state.selectedBubblesIndexes).toEqual([5, 4]);
            instance._onKeyDown(createEvent({ keyCode: KEYS.LEFT, shiftKey: true }));
            expect(wrapper.instance().state.selectedBubblesIndexes).toEqual([5, 4, 3]);
        });

        it('При выбранном баббле выделение c шифтом должно снимать выделение', () => {
            instance._onKeyDown(createEvent({ keyCode: KEYS.RIGHT, shiftKey: true }));
            expect(wrapper.instance().state.selectedBubblesIndexes).toEqual([5, 4]);
            instance._onKeyDown(createEvent({ keyCode: KEYS.RIGHT, shiftKey: true }));
            expect(wrapper.instance().state.selectedBubblesIndexes).toEqual([5]);
        });

        it('Клик по бабблу должен выделять его', () => {
            instance._onBubbleClick(createEvent(), 1);
            expect(wrapper.instance().state.selectedBubblesIndexes).toEqual([1]);
        });

        it('Клик по бабблу с контролом должен добавлять его к выделению', () => {
            instance._onBubbleClick(createEvent({ ctrlKey: true }), 4);
            expect(wrapper.instance().state.selectedBubblesIndexes).toEqual([1, 4]);
        });

        it('Клик по бабблу с шифтом должен выделять диапазон', () => {
            instance._onBubbleClick(createEvent({ shiftKey: true }), 0);
            expect(wrapper.instance().state.selectedBubblesIndexes.sort()).toEqual([0, 1, 2, 3, 4]);
        });

        it('Клик по бабблу с шифтом должен убирать из диапазона некоторые бабблы', () => {
            instance._onBubbleClick(createEvent({ shiftKey: true }), 3);
            expect(wrapper.instance().state.selectedBubblesIndexes.sort()).toEqual([3, 4]);
        });

        it('По нажатию backspace должен вызываться onBubblesRemove с идентификаторами соответствущих бабблов', () => {
            instance._onKeyDown(createEvent({ keyCode: KEYS.BACKSPACE }));
            expect(mainProps.onBubblesRemove).toBeCalledWith(['id4', 'id5']);
        });
    });
});
