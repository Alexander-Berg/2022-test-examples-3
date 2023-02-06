import {
    prepareSuite,
    mergeSuites,
    makeSuite,
} from 'ginny';


/** Объединяет провекри метрик указанного виджета и Snippet */
export default function (suiteName, widgetSuite, snippetSuite) {
    return makeSuite(suiteName, {
        params: {
            selector: 'Селектор для поиска виджета',
            /** крайне желательно передавать сюда схемы из файла ../schemas, чтобы было проще менять */
            payloadWidgetSchema: 'Объект со схемами валидации целей метрик виджета (js-schema)',
            payloadSnippetSchema: 'Объект со схемами валидации целей метрик Snippet (js-schema)',
            goalNamePrefix: 'Префикс зоны',
            snippetIndex: 'Позиция сниппета - отсчёт от 1',
        },
        story: mergeSuites(
            prepareSuite(widgetSuite, {
                hooks: {
                    async beforeEach() {
                        this.params = {
                            ...this.params,
                            payloadSchema: this.params.payloadWidgetSchema,
                        };
                    },
                },
            }),
            prepareSuite(snippetSuite, {
                hooks: {
                    async beforeEach() {
                        this.params = {
                            ...this.params,
                            payloadSchema: {
                                ...this.params.payloadWidgetSchema,
                                ...this.params.payloadSnippetSchema,
                            },
                        };
                    },
                },
            })
        ),
    });
}
