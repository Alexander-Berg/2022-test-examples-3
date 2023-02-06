import {OrderedMap, Map} from 'immutable';

import ReminderRecord from '../ReminderRecord';
import {getSortedReminders} from '../remindersSelectors';

describe('remindersSelectors', () => {
  describe('getSortedReminders', () => {
    test('должен возвращать напоминания, отсортированные по убыванию времени напоминания', () => {
      const state = {
        reminders: new Map({
          '1': new ReminderRecord({
            id: '1',
            reminderDate: '2018-02-28T10:00:00+03:00'
          }),
          '2': new ReminderRecord({
            id: '2',
            reminderDate: '2018-02-28T09:00:00+03:00'
          }),
          '4': new ReminderRecord({
            id: '4',
            reminderDate: '2018-02-27T09:00:00+05:00'
          }),
          '3': new ReminderRecord({
            id: '3',
            reminderDate: '2018-02-27T09:00:00+03:00'
          })
        })
      };

      expect(getSortedReminders(state)).toEqual(
        new OrderedMap([
          ['4', new ReminderRecord({id: '4', reminderDate: '2018-02-27T09:00:00+05:00'})],
          ['3', new ReminderRecord({id: '3', reminderDate: '2018-02-27T09:00:00+03:00'})],
          ['2', new ReminderRecord({id: '2', reminderDate: '2018-02-28T09:00:00+03:00'})],
          ['1', new ReminderRecord({id: '1', reminderDate: '2018-02-28T10:00:00+03:00'})]
        ])
      );
    });
  });
});
