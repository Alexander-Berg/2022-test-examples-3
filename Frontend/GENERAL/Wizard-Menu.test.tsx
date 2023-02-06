import React, { FC } from 'react';
import { mount } from 'enzyme';

import { IStepEntity, StepState } from '../Wizard.lib';
import { Menu } from './Wizard-Menu';

const MockStep: FC<{ name: string }> = ({ name }) => (
    <div className="MockStep">
        {name}
    </div>
);

const baseStepEntity: IStepEntity<{}, {}> = {
    id: 'base',
    title: 'Base',
    description: 'Base description',
    validate: () => Promise.resolve({ passed: true }),
    checkStepCondition: () => false,
    component: () => <MockStep name="base" />,
};

describe('Wizard-Menu', () => {
    const baseSteps = [1, 2, 3, 4].map(i => ({
        ...baseStepEntity,
        id: `id${i}`,
        title: `Step${i}`,
    }));

    it('Should render currentSteps', () => {
        const isStepVisited = (id: string) => !(id === 'id2');

        const wrapper = mount(
            <Menu
                currentStepId="id1"
                onMenuItemChange={jest.fn()}
                currentSteps={baseSteps}
                stepStates={{}}
                disableUnvisitedSteps
                isStepVisited={isStepVisited}
            />,
        );

        expect(wrapper.find('.Menu-Item').length).toBe(4);
        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });

    it('Should set unvisited states disabled with flag disableUnvisitedSteps', () => {
        const isStepVisited = (id: string) => !(id === 'id2');

        const wrapper = mount(
            <Menu
                currentStepId="id1"
                onMenuItemChange={jest.fn()}
                currentSteps={baseSteps}
                stepStates={{}}
                disableUnvisitedSteps
                isStepVisited={isStepVisited}
            />,
        );

        const disabledItems = wrapper.find('.Menu-Item_disabled');
        expect(disabledItems.length).toBe(1);
        expect(disabledItems.contains('Step2')).toBeTruthy();
        expect(wrapper.find('.Menu-Item').at(1)).toEqual(disabledItems);

        wrapper.unmount();
    });

    it('Should render right status icons', () => {
        const stepStates = {
            id1: StepState.passed,
            id2: StepState.error,
            id3: StepState.loading,
            id4: undefined,
        };

        const props = {
            currentStepId: 'id2',
            onMenuItemChange: jest.fn(),

            currentSteps: baseSteps,
            stepStates: stepStates,
            disableUnvisitedSteps: false,
            isStepVisited: jest.fn(),
        };
        const wrapper = mount(
            <Menu
                {...props}
            />,
        );

        const menuItems = wrapper.find('.MenuItem.Wizard-MenuItem');

        expect(menuItems.at(0).find('span.Wizard-Icon').prop('className')).toContain('Icon_glyph_check');
        expect(menuItems.at(1).find('span.Wizard-Icon').prop('className')).toContain('Icon_glyph_error-filled');
        expect(menuItems.at(2).find('div.Spin2').length).toBe(1);
        expect(menuItems.at(3).find('span.Wizard-Icon').length).toBe(0);
        expect(menuItems.at(3).find('div.Spin2').length).toBe(0);

        wrapper.unmount();
    });

    it('Should set as selected step with currentStepId', () => {
        const wrapper = mount(
            <Menu
                currentStepId="id2"
                onMenuItemChange={jest.fn()}
                currentSteps={baseSteps}
                stepStates={{}}
                disableUnvisitedSteps={false}
                isStepVisited={jest.fn().mockReturnValue(false)}
            />,
        );

        expect(wrapper.find('.Menu-Item_checked').length).toBe(1);
        expect(wrapper.find('.Menu-Item_checked').contains('Step2')).toBeTruthy();

        wrapper.unmount();
    });

    it('Should call onMenuItemChange after click on MenuItem', () => {
        const onMenuItemChange = jest.fn();

        const wrapper = mount(
            <Menu
                currentStepId="id2"
                onMenuItemChange={onMenuItemChange}
                currentSteps={baseSteps}
                stepStates={{}}
                disableUnvisitedSteps={false}
                isStepVisited={jest.fn().mockReturnValue(false)}
            />,
        );

        wrapper.find('.Menu-Item').at(3).simulate('click');

        expect(onMenuItemChange).toBeCalled();

        wrapper.unmount();
    });
});
