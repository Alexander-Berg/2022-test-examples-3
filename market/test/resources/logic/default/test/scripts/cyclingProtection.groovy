/*
 * @title Защита от зацикливания
 */
api.bcp.edit(obj, ['attr2': (obj.attr2?obj.attr2:'1') + '0'])