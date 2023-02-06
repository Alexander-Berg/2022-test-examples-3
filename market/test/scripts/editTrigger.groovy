/*
 * @title Заполнение значение из триггера на редактирование
 */
if (obj.attr0 != old.attr0) {
    api.bcp.edit(obj, ['attr1': 'valueFromEditTrigger'])
}