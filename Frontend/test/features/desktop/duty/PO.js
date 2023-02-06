const inherit = require('inherit');
const { create, Entity } = require('bem-page-object');
const PO = {};

const ReactEntity = inherit(Entity, null, { preset: 'react' });

PO.button = new Entity({ block: 'button2' });
PO.dutyCalendar = new ReactEntity({ block: 'DutyCalendar' });
PO.dutyCalendarScaleSelector = new ReactEntity({ block: 'DutyCalendar', elem: 'ScaleSelector' });
PO.dutyCalendarScaleSelector.notDisabled = new ReactEntity({ block: 'RadioButton', elem: 'Radio' }).modsInverse({ disabled: true });
PO.dutyCalendarScaleSelector.hours = new Entity('[value="hour"]');
PO.dutyCalendarGrid = new ReactEntity({ block: 'DutyCalendarGrid' });
PO.dutyCalendarGridSecond = PO.dutyCalendarGrid.adjacentSibling(PO.dutyCalendarGrid);
PO.dutyCalendarGridThird = PO.dutyCalendarGrid.adjacentSibling(PO.dutyCalendarGridSecond);
PO.dutyShift = new ReactEntity({ block: 'DutyShift' });
PO.dutyShiftDetails = new Entity({ block: 'abc-duty-details' });
PO.dutyShiftEdit = new ReactEntity({ block: 'DutyShiftEdit' });
PO.dutyScheduleEditModal = new Entity({ block: 'abc-duty-schedule-edit' });
PO.dutyShiftEditModal = new ReactEntity({ block: 'DutyCalendar', elem: 'ShiftEditModal' });
PO.dutyShiftEditModalContent = PO.dutyShiftEditModal.descendant(new ReactEntity({ block: 'Modal', elem: 'Content' }));
PO.suggestItem = new Entity({ block: 'ta-suggest-item' });

PO.visibleModal = new Entity({ block: 'Modal' }).mods({ visible: true });

PO.dutyCalendarChangeSchedule = new ReactEntity({ block: 'DutyCalendar', elem: 'ChangeSchedule' });
PO.dutyCalendarSpin = new ReactEntity({ block: 'DutyCalendar', elem: 'Spin' });

PO.dutyCalendarGrid.creatingSchedule = new ReactEntity({ block: 'DutyEmptySchedule' });
PO.dutyCalendarGrid.creatingSchedule.refreshPageBtn = new Entity('.Button2');

PO.dutyCalendarGrid.row = new ReactEntity({ block: 'DutyCalendarGrid', elem: 'Row' });
PO.dutyCalendarGrid.row.second = PO.dutyCalendarGrid.row.adjacentSibling(PO.dutyCalendarGrid.row);
PO.dutyCalendarGrid.interval = new ReactEntity({ block: 'DutyCalendarGrid', elem: 'Interval' });
PO.dutyCalendarGrid.intervalPending = new ReactEntity({ block: 'DutyCalendarGrid', elem: 'Interval' }).mods({ type: 'pending' });
PO.dutyCalendarGrid.intervalPendingShift = PO.dutyCalendarGrid.intervalPending.descendant(PO.dutyShift);
PO.dutyCalendarGrid.intervalApproved = new ReactEntity({ block: 'DutyCalendarGrid', elem: 'Interval' }).mods({ type: 'approved' });
PO.dutyCalendarGrid.intervalApprovedShift = PO.dutyCalendarGrid.intervalApproved.descendant(PO.dutyShift);

PO.dutyCalendarGrid.interval.holiday = new ReactEntity({ block: 'DutyCalendarGrid', elem: 'Interval' }).mods({ type: 'holiday' });
PO.dutyCalendarGrid.intervalFirstOnSecondRow = PO.dutyCalendarGrid.row.second.descendant(PO.dutyCalendarGrid.interval);

PO.dutyCalendarGridThird.intervalApproved = PO.dutyCalendarGrid.intervalApproved.copy();

module.exports = create(PO);
