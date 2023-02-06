const { ReactEntity } = require('../../../../../../vendors/hermione');

const elems = {};

elems.List = new ReactEntity({ block: 'UniSearchList' });
elems.List.Item = new ReactEntity({ block: 'UniSearchList', elem: 'Item' });
elems.List.ItemFirst = elems.List.Item.copy().withIndex(0);
elems.List.ItemSecond = elems.List.Item.copy().withIndex(1);
elems.List.ItemThird = elems.List.Item.copy().withIndex(2);
elems.List.Item.Rating = new ReactEntity({ block: 'UniSearchRating' });

module.exports = elems;
