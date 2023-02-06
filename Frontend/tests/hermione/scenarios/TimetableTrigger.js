const Trigger = require('./Trigger');

const everyday = [
    'monday',
    'tuesday',
    'wednesday',
    'thursday',
    'friday',
    'saturday',
    'sunday',
];

module.exports = class TimetableTrigger extends Trigger {
    type = 'scenario.trigger.timetable';

    constructor(daysOfWeek = everyday, timeOffset = 74345) {
        super({
            days_of_week: daysOfWeek,
            time_offset: timeOffset,
        });
    }
};
