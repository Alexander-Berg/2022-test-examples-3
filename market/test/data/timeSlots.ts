import { DayOfWeekType, SupplierScheduleDTO } from 'src/java/definitions-replenishment';

export const timeSlots: SupplierScheduleDTO[] = [
  {
    id: 1,
    dayOfWeek: DayOfWeekType.MO,
    warehouseId: 102,
    timeStart: '12:00:00',
    timeEnd: '13:00:00',
    active: true,
  },
  {
    id: 2,
    dayOfWeek: DayOfWeekType.WE,
    warehouseId: 103,
    timeStart: '22:00:00',
    timeEnd: '23:59:00',
    active: true,
  },
];
