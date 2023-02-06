import CategoryQuestionsFooterEntrypoint from '@self/platform/widgets/parts/CategoryQuestionsFooterEntrypoint/__pageObject';
import {hideElementBySelector} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'CategoryQuestionsFooterEntrypoint',
    selector: CategoryQuestionsFooterEntrypoint.root,
    before(actions) {
        // Скрываем кнопку "Ещё N вопрос/вопроса/вопросов", из-за меняющегося текста скринтесты постоянно флапают
        hideElementBySelector(actions, CategoryQuestionsFooterEntrypoint.more);
    },
    capture() {
    },
};
