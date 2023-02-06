'use strict';

// Магия для добавления src в пути:
process.env['NODE_PATH'] = require('path').resolve(__dirname, '..', 'src');
require('module')._initPaths();

require('babel-register');

require('./store/reducers');
require('./store/actions');
require('./helpers/i18n');
require('./ufo-common/mail-origin-checker');
