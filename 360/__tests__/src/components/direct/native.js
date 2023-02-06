import { getClassById } from 'lib/direct';
import NativeDirect from 'components/direct/native';
import React from 'react';
import { shallow } from 'enzyme';

jest.mock('lib/direct');

describe('native direct', () => {
    beforeEach(() => {
        getClassById.mockReturnValue('encrypted-class');
    });

    afterEach(() => {
        delete window.yaads;
    });

    it('should have a placeholder', () => {
        const component = shallow(
            <NativeDirect
                directClass="direct"
                directPlaceholderClass="placeholder"
            />
        );
        expect(component).toMatchSnapshot();
        expect(window.yaads).toEqual([{
            id: '',
            render: '.',
            success: expect.any(Function),
            reject: expect.any(Function)
        }]);
        expect(getClassById).not.toHaveBeenCalled();
    });

    it('should have an encrypted class', () => {
        const component = shallow(
            <NativeDirect
                directClass="direct"
                directPlaceholderClass="placeholder"
                encryptedClassId="encryptedClassId"
                blockId="blockId"
                cls="cls" />);
        component.setState({ isAdRendered: true });
        expect(component).toMatchSnapshot();
        expect(window.yaads).toEqual([{
            id: 'blockId',
            render: '.cls',
            success: expect.any(Function),
            reject: expect.any(Function)
        }, {
            id: 'blockId',
            render: '.cls',
            success: expect.any(Function),
            reject: expect.any(Function)
        }]);
        expect(getClassById).toHaveBeenCalledWith('encryptedClassId');
    });
});
