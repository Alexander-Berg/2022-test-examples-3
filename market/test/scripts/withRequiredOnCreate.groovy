/*
 * @title withRequiredOnCreate
 */
// важно каждый обязательный атрибут заполнять отдельно чтобы после первого редактирования были
// установлены не все обязательные атрибуты
api.bcp.edit(obj, ['attr1':'value1'])
api.bcp.edit(obj, ['attr2':'value2'])