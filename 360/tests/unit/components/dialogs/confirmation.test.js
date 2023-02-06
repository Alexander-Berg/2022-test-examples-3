import '../../noscript';
import React from 'react';
import { shallow } from 'enzyme';
import serializer from 'enzyme-to-json';

jest.mock('../../../../components/extract-preloaded-data');
jest.mock('config', () => ({}));

import { ConfirmationDialog } from '../../../../components/redux/components/dialogs/confirmation';

describe('ConfirmationDialog', () => {
    it('should render dialog (single)', () => {
        const wrapper = shallow(
            <ConfirmationDialog
                title="confirmation dialog title"
                text="confirmation dialog text"
                submitButtonText="confirmation dialog submit button text"
                cancelButtonText="confirmation dialog cancel button text"
                onSubmit={jest.fn()}
                onSubmitAll={jest.fn()}
                onReject={jest.fn()}
                onRejectAll={jest.fn()}
            />
        );
        expect(serializer(wrapper)).toMatchSnapshot();
    });

    it('should render dialog (group, cancel all)', () => {
        const wrapper = shallow(
            <ConfirmationDialog
                isGroup
                title="confirmation dialog title"
                text="confirmation dialog text"
                submitButtonText="confirmation dialog submit button text"
                cancelButtonText="confirmation dialog cancel all button text"
                cancelAll
                submitAllButtonText="confirmation dialog submit all button text"
                onSubmit={jest.fn()}
                onSubmitAll={jest.fn()}
                onReject={jest.fn()}
                onRejectAll={jest.fn()}
            />
        );
        expect(serializer(wrapper)).toMatchSnapshot();
    });
});
