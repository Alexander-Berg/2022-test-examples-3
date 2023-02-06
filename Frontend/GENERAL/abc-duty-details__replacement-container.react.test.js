import React from 'react';
import { shallow } from 'enzyme';

import AbcDutyDetails__ReplacementContainer from 'b:abc-duty-details e:replacement-container';

describe('Temporary replacements changes', () => {
    const PARENT_ID = 42;
    const REPLACEMENT_ID = 1;
    const REPLACEMENT = {
        id: REPLACEMENT_ID,
        isDeleted: false,
    };
    const DELETED_REPLACEMENT = Object.assign({}, REPLACEMENT, { isDeleted: true });

    let wrapper = null;
    let restoreRequest = null;
    let deleteRequest = null;

    beforeEach(() => {
        restoreRequest = jest.fn();
        deleteRequest = jest.fn();

        wrapper = shallow(
            <AbcDutyDetails__ReplacementContainer
                replacement={REPLACEMENT}
                replaceFor={PARENT_ID}
                createDutyShiftsReplacement={restoreRequest}
                deleteDutyShiftsReplacement={deleteRequest}
            />,
        );
    });

    afterEach(() => {
        wrapper.unmount();
    });

    it('Should update state from props', () => {
        wrapper.setProps({ replacement: DELETED_REPLACEMENT });
        expect(wrapper.state('isDeleted')).toBe(true);
    });

    describe('Delete', () => {
        it('Should call delete callback when not yet deleted', () => {
            wrapper.instance().onDelete();

            expect(wrapper.state('isDeleted')).toBe(true);
            expect(deleteRequest).toHaveBeenCalledWith(PARENT_ID, REPLACEMENT_ID);
            expect(restoreRequest).not.toHaveBeenCalled();
        });

        it('Should not call delete callback when already deleted', () => {
            wrapper.setState({ isDeleted: true });

            wrapper.instance().onDelete();

            expect(wrapper.state('isDeleted')).toBe(true);
            expect(deleteRequest).not.toHaveBeenCalled();
            expect(restoreRequest).not.toHaveBeenCalled();
        });
    });

    describe('Restore', () => {
        it('Should call restore callback when deleted', () => {
            wrapper.setState({ isDeleted: true });

            wrapper.instance().onRestore();

            expect(wrapper.state('isDeleted')).toBe(false);
            expect(deleteRequest).not.toHaveBeenCalled();
            expect(restoreRequest).toHaveBeenCalledWith(PARENT_ID, REPLACEMENT);
        });

        it('Should not call restore callback when not deleted', () => {
            wrapper.instance().onRestore();

            expect(wrapper.state('isDeleted')).toBe(false);
            expect(restoreRequest).not.toHaveBeenCalled();
            expect(deleteRequest).not.toHaveBeenCalled();
        });
    });

    it('Should not call callbacks in loading state', () => {
        wrapper.setProps({ loading: true });

        wrapper.instance().onRestore();
        wrapper.instance().onDelete();

        expect(restoreRequest).not.toHaveBeenCalled();
        expect(deleteRequest).not.toHaveBeenCalled();
    });
});
