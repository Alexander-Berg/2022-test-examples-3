import React from 'react';
import { render as libRender } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { User } from '~/src/common/context/types';
import { withContext } from '~/src/common/hoc';

import DutyConnected from '~/src/features/Duty/Duty.container';

import { configureStore } from '~/src/abc/react/redux/store';
import { withRedux } from '~/src/common/hoc';

import { PageFragment, RootPageFragment } from '~/test/jest/utils';

export class DutyShift extends PageFragment {
    hover() {
        userEvent.hover(this.container);
    }
}

export class DutyCalendar extends RootPageFragment {
    get dateFilter() {
        return this.query(PageFragment, '.DutyCalendar__date-filter');
    }

    get periodFilter() {
        return this.query(PageFragment, '.DutyCalendar__period-filter');
    }

    get scheduleFilter() {
        return this.query(PageFragment, '.DutyCalendar__schedule-filter');
    }

    get firstShift() {
        return this.getAll(DutyShift, '.DutyShift')[0];
    }

    get addScheduleButton() {
        return this.renderResult.queryByText('abc-duty-calendar:create-duty-settings');
    }

    get shiftPopup() {
        const popup = this.container?.closest('body')?.querySelector('.Popup2.DutyCalendarGrid-ShiftDetailsPopup');
        return popup && new PageFragment(popup);
    }
}

export function render(storeData: Record<string, unknown>): DutyCalendar {
    const store = configureStore({
        initialState: storeData,
        fetcherOptions: {
            fetch: () => Promise.resolve(),
        },
    });

    const abcContextMock = {
        configs: {
            hosts: {
                centerClient: { protocol: 'https:', hostname: 'center.y-t.ru' },
                staff: { protocol: 'https:', hostname: 'staff.y-t.ru' },
            },
        },
        user: {} as User,
    };

    const Calendar = withContext(withRedux(DutyConnected, store), abcContextMock);

    return new DutyCalendar(libRender(
        <Calendar
            service={{ id: 123 }}
        />,
    ));
}
