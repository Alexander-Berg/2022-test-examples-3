/*
 * @title Заполнение значение из триггера на создание
 */
if (old.attr0 != obj.attr0) {
    api.bcp.edit(self, ['attr0': obj.attr0])
}
