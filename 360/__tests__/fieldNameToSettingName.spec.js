import fieldNameToSettingName from '../fieldNameToSettingName';

describe('fieldNameToSettingName', () => {
  test('должен формировать название настройки из контекста формы и навзвания поля', () => {
    const formContext = 'grid';
    const fieldName = 'attendees';
    const settingName = 'grid_createPopupWithAttendees';

    expect(fieldNameToSettingName(formContext, fieldName)).toEqual(settingName);
  });
});
