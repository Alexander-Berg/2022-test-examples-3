const { Entity } = require('@yandex-int/bem-page-object');

const { organic } = require('../../../../../hermione/page-objects/common/construct/organic');
const { ReactEntity } = require('../../../../vendors/hermione');

const elems = {};

elems.yaTalk = new Entity({ block: 't-construct-adapter', elem: 'ya-talk' });
elems.yaTalk.title = organic.title.link.copy();
elems.yaTalk.greenUrl = organic.greenurl.copy();
elems.yaTalk.thumb = new Entity({ block: 'Thumb' });
elems.yaTalk.meta = new ReactEntity({ block: 'Meta' });
elems.yaTalk.meta.link = new ReactEntity({ block: 'Link' });

module.exports = elems;
