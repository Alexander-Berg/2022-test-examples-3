/**
 * Нужно для использования в тестах с использованием тестамента
 * так как при импорте из index.touch.js ругается на то что EditableCardRoot
 * является undefined
 */
import {resolveBy, select} from 'reselector';
import {PageObject} from 'ginny';

const resolve = resolveBy(require.resolve);

const {EditableCardContent: EditableCardContentSelector} = resolve('../');

export class EditableCardContent extends PageObject {
    static get root() {
        return select`${EditableCardContentSelector}`;
    }
}
