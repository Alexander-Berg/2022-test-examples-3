// @ts-ignore
import {defineTest} from 'jscodeshift/dist/testUtils';

jest.autoMockOff();

defineTest(__dirname, 'ymaps-tanker', null, 'ymaps-tanker', {parser: 'tsx'});
defineTest(__dirname, 'ymaps-tanker', null, 'ymaps-tanker.single', {parser: 'tsx'});
/**
 * Когда функция по случайному совпадению называется i18n ничего не делаем
 */
defineTest(__dirname, 'ymaps-tanker', null, 'ymaps-tanker.fake', {parser: 'tsx'});
