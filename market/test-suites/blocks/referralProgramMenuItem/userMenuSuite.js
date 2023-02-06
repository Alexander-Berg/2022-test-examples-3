import {makeSuite} from 'ginny';

import makeCases from './makeCases';

module.exports = params => makeSuite('Плашка "Приглашайте друзей",', {
    story: makeCases(params),
});
