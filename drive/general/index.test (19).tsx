import { shallow } from 'enzyme';
import * as React from 'react';

import { ActionCheckbox } from './index';

describe('ActionCheckbox', () => {
    it('should be checked after change', async function () {
        const mockRequestOnToggle = () => {
            return new Promise<undefined>((resolve) => { resolve(); });
        };

        const mockSetValue = jest.fn();
        const actionCheckboxWrapper = shallow(
            <ActionCheckbox value={false}
                            setValue={mockSetValue}
                            expandError={() => undefined}
                            requestOnToggle={mockRequestOnToggle}/>,
        );

        actionCheckboxWrapper.find('input').simulate('change');
        //hack to wait for await inside the component before evaluating expect()
        await Promise.resolve();
        expect(mockSetValue).toBeCalledWith(true);
    });

    it('should be unchecked after change', async function () {
        const mockRequestOnToggle = () => {
            return new Promise<undefined>((resolve) => { resolve(); });
        };

        const mockSetValue = jest.fn();
        const actionCheckboxWrapper = shallow(
            <ActionCheckbox value={true}
                            setValue={mockSetValue}
                            expandError={() => undefined}
                            requestOnToggle={mockRequestOnToggle}/>,
        );

        actionCheckboxWrapper.find('input').simulate('change');
        //hack to wait for await inside the component before evaluating expect()
        await Promise.resolve();
        expect(mockSetValue).toBeCalledWith(false);
    });
});
