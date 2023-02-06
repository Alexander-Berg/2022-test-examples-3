'use strict';

const { create, ReactEntity } = require('../../../../../vendors/hermione');

const PO = {};

PO.telegramLink = new ReactEntity({ block: 'OrgSkiLinks' });
module.exports = create(PO);
