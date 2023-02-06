import { WeekOpeningHours } from '../../src/types/OpeningHours';
const date = new Date();

date.setFullYear(2018);
date.setMonth(7);
// В 11:30 GMT в любой таймзоне будет один и тот же день недели
date.setUTCHours(11);
date.setUTCMinutes(30);
date.setUTCSeconds(0);
date.setUTCMilliseconds(0);

date.setUTCDate(13);
export const monday = new Date(date);

date.setUTCDate(14);
export const tuesday = new Date(date);

date.setUTCDate(15);
export const wednesday = new Date(date);

date.setUTCDate(16);
export const thursday = new Date(date);

date.setUTCDate(17);
export const friday = new Date(date);

date.setUTCDate(18);
export const saturday = new Date(date);

date.setUTCDate(19);
export const sunday = new Date(date);

export const defaultDayOpeningHours = {
    from: '10:00',
    to: '18:00',
};

export const defaultOpeningHours: WeekOpeningHours = {
    Monday: defaultDayOpeningHours,
    Tuesday: defaultDayOpeningHours,
    Wednesday: defaultDayOpeningHours,
    Thursday: defaultDayOpeningHours,
    Friday: defaultDayOpeningHours,
    Saturday: null,
    Sunday: null,
};

export const badOpeningHours: WeekOpeningHours = {
    Monday: null,
    Tuesday: null,
    Wednesday: null,
    Thursday: null,
    Friday: null,
    Saturday: null,
    Sunday: null,
};
