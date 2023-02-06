import React from 'react';
import WidgetPage from '../../../client/blocks/WidgetPage/WidgetPage';
import { Skill } from '../../../client/model/skill';

const publishedSkill = {
    onAir: true,
    botGuid: 'd6a257b8-b930-4689-b00c-8ae994670c71',
};

const unpublishedSkill = {
    onAir: false,
};

describe('WidgetPage', () => {
    it('should render correctly when skill is published', () => {
        const widgetPage = enzyme.shallow(<WidgetPage skill={publishedSkill as Skill} createWidget={jest.fn()} />);

        expect(widgetPage.exists('.WidgetPage__stub')).toBeFalsy();
    });

    it('should render correctly when skill is unpublished', () => {
        const widgetPage = enzyme.shallow(<WidgetPage skill={unpublishedSkill as Skill} createWidget={jest.fn()} />);

        expect(widgetPage.exists('.WidgetPage__stub')).toBeTruthy();
    });

    it('should discover subfield when buttonText is entered and hide when deleted', () => {
        const widgetPage = enzyme.mount<WidgetPage>(
            <WidgetPage skill={publishedSkill as Skill} createWidget={jest.fn()} />,
        );

        const targetInput = widgetPage.find('input[name="button-text"]');

        targetInput.simulate('change', {
            target: {
                value: 'test',
            },
        });

        expect(widgetPage.exists('.WidgetPage__subfield')).toBeTruthy();

        targetInput.simulate('change', {
            target: {
                value: '',
            },
        });

        expect(widgetPage.exists('.WidgetPage__subfield')).toBeFalsy();

        widgetPage.unmount();
    });

    it('should not discover subfield by default', () => {
        const widgetPage = enzyme.mount(<WidgetPage skill={publishedSkill as Skill} createWidget={jest.fn()} />);

        expect(widgetPage.exists('.WidgetPage__subfield')).toBeFalsy();

        widgetPage.unmount();
    });
});
