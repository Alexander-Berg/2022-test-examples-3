import React from 'react';
import { shallow, mount } from 'enzyme';

import { withRegistry } from '@bem-react/di';
import { compose } from '@bem-react/core';

import registry from '../../../../src/components/registry';

import { Menu as MenuBase } from '../../../../src/components/notification/menu';

const Menu = withRegistry(registry)(MenuBase);

describe('Menu', () => {
    const options = [
        { name: 'Прочитать все' },
        { name: 'Настройки' },
    ];

    describe('plain', () => {
        test('should render menu with 2 options', () => {
            const component = shallow(<Menu options={ options } />);

            expect(component.html()).toMatchSnapshot();
        });

        test('should render menu with 2 options and visible kebab', () => {
            const component = shallow(<Menu visible options={ options } />);

            expect(component.html()).toMatchSnapshot();
        });
    });

    describe('popup', () => {
        test('should have _visible class if popupVisible', () => {
            const component = mount(<Menu visible options={ options } />);
            const popup = component.find('.gnc-notifications-item__popup');

            component.find(MenuBase).setState({ popupVisible: true });

            expect(popup.html()).toMatchSnapshot();
        });

        test('should appear on kebab hover', () => {
            jest.useFakeTimers();

            const component = mount(<Menu visible options={ options } />);
            const kebab = component.find('.gnc-notifications-item__menu-kebab');

            kebab.simulate('mouseenter');
            expect(component.find(MenuBase).state('popupVisible')).toBe(true);

            kebab.simulate('mouseleave', {});
            jest.runAllTimers();
            expect(component.find(MenuBase).state('popupVisible')).toBe(false);
        });

        test('should not appear if disableHover is passed', () => {
            const component = mount(<Menu disableHover visible options={ options } />);
            const kebab = component.find('.gnc-notifications-item__menu-kebab');

            kebab.simulate('mouseenter', { stopPropagation: jest.fn() });
            expect(component.find(MenuBase).state('popupVisible')).toBe(false);
        });

        test('should toggle on kebab click with disableHover', () => {
            const component = mount(<Menu disableHover visible options={ options } />);
            const kebab = component.find('.gnc-notifications-item__menu-kebab');

            kebab.simulate('click', { stopPropagation: jest.fn(), preventDefault: jest.fn() });
            expect(component.find(MenuBase).state('popupVisible')).toBe(true);

            kebab.simulate('click', { stopPropagation: jest.fn(), preventDefault: jest.fn() });
            expect(component.find(MenuBase).state('popupVisible')).toBe(false);
        });

        // why not work (((
        /*        test.skip('should close on outside click', () => {
            const component = mount(
                <Menu disableHover visible options={ options } />
            );

            const kebab = component.find('.gnc-notifications-item__menu-kebab');

            kebab.simulate('click', { stopPropagation: jest.fn() });
            expect(component.state('popupVisible')).toBe(true);

            window.document.dispatchEvent(new Event('mousedown'));
            expect(component.state('popupVisible')).toBe(false);
        });
*/
        test('should call clickHandler on menu click', () => {
            const menuClickHandler = jest.fn();
            const clickOptions = [{ name: 'Прочитать все', clickHandler: menuClickHandler }];
            const component = mount(
                <Menu disableHover visible options={ clickOptions } />
            );

            const kebab = component.find('.gnc-notifications-item__menu-kebab');
            const menuItem = component.find('.gnc-notifications-item__popup-option').at(0);

            kebab.simulate('click', { stopPropagation: jest.fn() });
            expect(component.find(MenuBase).state('popupVisible')).toBe(true);

            menuItem.simulate('click', { stopPropagation: jest.fn() });
            expect(menuClickHandler).toHaveBeenCalledTimes(1);
            expect(component.find(MenuBase).state('popupVisible')).toBe(false);
        });
    });
});
