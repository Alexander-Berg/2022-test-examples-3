const { Entity, ReactEntity } = require('../../../../vendors/hermione');

const { Organic } = require('../../../../features/Organic/Organic.test/Organic.page-objects');

const elems = {};

elems.afishaEvent = new Entity({ block: 't-construct-adapter', elem: 'afisha-event' });

elems.afishaEvent.title = Organic.Title.copy();
elems.afishaEvent.greenurl = Organic.Path.copy();
elems.afishaEvent.thumb = new ReactEntity({ block: 'AfishaEvent', elem: 'Thumb' });
elems.afishaEvent.button = new ReactEntity({ block: 'AfishaEvent', elem: 'Button' });

elems.pager = new Entity({ block: 'pager' });
elems.pager.item = new Entity({ block: 'pager', elem: 'item' });
elems.pager.next = elems.pager.item.mods({ kind: 'next' });

module.exports = elems;
