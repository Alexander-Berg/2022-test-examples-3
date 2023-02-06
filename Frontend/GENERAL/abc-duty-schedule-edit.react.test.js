import React from 'react';
import { shallow } from 'enzyme';

import AbcDutyScheduleEdit from 'b:abc-duty-schedule-edit';

describe('AbcDutyScheduleEdit', () => {
    it('Should render schedule edit form', () => {
        const wrapper = shallow(
            <AbcDutyScheduleEdit
                people={{}}
                selectedPerson={null}
                absences={[]}
                schedules={{}}
                loading={false}
                roles={[]}
                currentRole={[null]}
                dateFrom={new Date(Date.UTC(2019, 1, 1))}
                dateTo={new Date(Date.UTC(2019, 1, 7))}
                onFilterChange={() => {}}
                onPersonChange={() => {}}
                isShiftEditOpen={false}
                shiftEditId={null}
                openShiftEdit={() => {}}
                closeShiftEdit={() => {}}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
