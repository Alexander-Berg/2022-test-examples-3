import { render } from './duty.po';

import { getServicesViewerPermissions } from '~/test/jest/mocks/data/common';
import { getDutyAbsencesMock, getDutySchedulesMock, getDutyShiftsMock } from '~/test/jest/mocks/data/duty';

describe('Гранулярный доступ к дежурствам', () => {
    beforeEach(() => {
        jest.useFakeTimers();
    });

    describe('Положительные', () => {
        it('1. Пользователь с ограниченной ролью может смотреть графики дежурств', () => {
            // - do: залогиниться пользователем robot-abc-003
            // устанавливаем права для ограниченной роли, как у робота
            // - do: открыть страницу графика дежурств (/services/allkindsofduty/duty/)
            // рендерим компонент дежурств
            const calendar = render({
                granularPermissions: getServicesViewerPermissions(),
                dutyShifts: getDutyShiftsMock(),
                dutySchedules: getDutySchedulesMock(),
                dutyAbsences: getDutyAbsencesMock(),
            });

            // - screenshot: отображаются графики дежурств, доступна фильтрация по датам, периодам и графикам
            expect(calendar.firstShift?.container).toBeInTheDocument();
            expect(calendar.dateFilter?.container).toBeInTheDocument();
            expect(calendar.periodFilter?.container).toBeInTheDocument();
            expect(calendar.scheduleFilter?.container).toBeInTheDocument();
            expect(calendar.container).not.toContainElement(calendar.addScheduleButton);

            // - do: навести курсор на любого дежурного
            calendar.firstShift?.hover();

            // даём попапу появиться
            jest.runAllTimers();

            // - screenshot: появился попап с информацией по дежурному и статусом смены - подтверждена/не подтверждена
            expect(calendar.shiftPopup?.container).toBeInTheDocument();
        });
    });
});
