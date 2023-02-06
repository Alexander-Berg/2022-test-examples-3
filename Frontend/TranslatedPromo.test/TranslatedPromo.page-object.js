'use strict';

const { ReactEntity, create } = require('../../../vendors/hermione/index');

const PO = {};

PO.translatedPromo = new ReactEntity({ block: 'TranslatedPromo' });
PO.translatedPromo.link = new ReactEntity({ block: 'TranslatedPromo', elem: 'Link' });

module.exports = create(PO);
