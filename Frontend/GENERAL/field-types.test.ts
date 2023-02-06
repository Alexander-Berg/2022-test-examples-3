import { Forms, FormsAPI } from './types';
import { toApi } from './field-types';
import { Nulls } from './utils/api-empty-survey';
import { answerTypes } from './utils/api-dicts';

const textQuestion: Forms.FieldText = {
    type: 'text',
    key: '',
    title: 'Текст',
    description: '',
    view: 'textinput',
    suggest: null,
};

const longTextQuestion: Forms.FieldText = {
    type: 'text',
    key: '',
    title: 'Длинный ответ',
    description: '',
    view: 'textarea',
    suggest: null,
};

describe('#toApi', () => {
    it('результат должен содеражать поля из шаблона пустого вопроса Nulls.Question', () => {
        const apiData = toApi(textQuestion, 'id');

        expect(apiData).toEqual(expect.objectContaining(Nulls.Question));
    });

    it('результат должен содеражать slug типа короткого ответа и id поля', () => {
        const id = 'a9ff57a2';
        const apiData = toApi(textQuestion, id);

        expect(apiData.param_slug).toBe(`${answerTypes.answer_short_text.slug}_${id}`);
    });

    it('результат должен содеражать slug типа длинного ответа и id поля', () => {
        const id = 'a9ff57a2';
        const apiData = toApi(longTextQuestion, id);

        expect(apiData.param_slug).toBe(`${answerTypes.answer_long_text.slug}_${id}`);
    });

    it('результат должен содеражать название вопроса', () => {
        const title = 'Название вопроса';
        const apiData = toApi({
            ...textQuestion,
            title,
        }, 'id');

        expect(apiData.label).toBe(title);
    });

    it('результат должен содеражать описание вопроса', () => {
        const description = 'Описание вопроса';
        const apiData = toApi({
            ...textQuestion,
            description,
        }, 'id');

        expect(apiData.param_help_text).toBe(description);
    });

    it('вопрос должен быть необязательным если не передано поле "required"', () => {
        const apiData = toApi(textQuestion, 'id');

        expect(apiData.param_is_required).toBe(false);
    });

    it('вопрос должен быть обязательным если не передано поле "required===true"', () => {
        const apiData = toApi({
            ...textQuestion,
            required: true,
        }, 'id');

        expect(apiData.param_is_required).toBe(true);
    });

    it(`вопрос без ответа должен иметь тип ${answerTypes.answer_statement.id}`, () => {
        const staticTextData: Forms.StaticText = {
            type: 'staticText',
            view: 'default',
            key: null,
            title: 'Вопрос без ответа',
            description: '',
        };

        const apiData = toApi(staticTextData, 'id');

        expect(apiData.answer_type_id).toBe(answerTypes.answer_statement.id);
    });

    it('вопрос без ответа в дефолтном виде не должен быть заголовком', () => {
        const staticTextData: Forms.StaticText = {
            type: 'staticText',
            view: 'default',
            key: null,
            title: 'Вопрос без ответа',
            description: '',
        };

        const apiData = toApi(staticTextData, 'id');

        expect(apiData.param_is_section_header).toBe(false);
    });

    it('вопрос без ответа быть заголовком если "view===header"', () => {
        const staticTextData: Forms.StaticText = {
            type: 'staticText',
            view: 'header',
            key: null,
            title: 'Вопрос без ответа',
            description: '',
        };

        const apiData = toApi(staticTextData, 'id');

        expect(apiData.param_is_section_header).toBe(true);
    });

    it(`вопрос короткий текст должен иметь тип ${answerTypes.answer_short_text.id}`, () => {
        const apiData = toApi({
            ...textQuestion,
            view: 'textinput',
        }, 'id');

        expect(apiData.answer_type_id).toBe(answerTypes.answer_short_text.id);
    });

    it(`вопрос длинный текст должен иметь тип ${answerTypes.answer_long_text.id}`, () => {
        const apiData = toApi({
            ...textQuestion,
            view: 'textarea',
        }, 'id');

        expect(apiData.answer_type_id).toBe(answerTypes.answer_long_text.id);
    });

    it(`списковый вопрос должен иметь тип ${answerTypes.answer_choices.id}`, () => {
        const apiData = toApi({
            ...textQuestion,
            type: 'choices',
            view: 'radio',
            options: [],
        }, 'id');

        expect(apiData.answer_type_id).toBe(answerTypes.answer_choices.id);
    });

    it('списковый вопрос с видом "radio" должен запрещать выбрать несколько вариантов', () => {
        const apiData = toApi({
            ...textQuestion,
            type: 'choices',
            view: 'radio',
            options: [],
        }, 'id');

        expect(apiData.param_is_allow_multiple_choice).toBe(false);
    });

    it('списковый вопрос с видом "select" должен запрещать выбрать несколько вариантов', () => {
        const apiData = toApi({
            ...textQuestion,
            type: 'choices',
            view: 'select',
            options: [],
        }, 'id');

        expect(apiData.param_is_allow_multiple_choice).toBe(false);
    });

    it('списковый вопрос с видом "checks" должен разрешать выбрать несколько вариантов', () => {
        const apiData = toApi({
            ...textQuestion,
            type: 'choices',
            view: 'checks',
            options: [],
        }, 'id');

        expect(apiData.param_is_allow_multiple_choice).toBe(true);
    });

    it('списковый вопрос с видом "select" должен быть с типом виджета "select"', () => {
        const apiData = toApi({
            ...textQuestion,
            type: 'choices',
            view: 'select',
            options: [],
        }, 'id');

        expect(apiData.param_widget).toBe('select');
    });

    it('списковый вопрос должен сформировать варианты ответа', () => {
        const options = [
            {
                text: 'вариант 1',
            },
            {
                text: 'вариант 2',
            },
        ];
        const apiData = toApi({
            ...textQuestion,
            type: 'choices',
            view: 'select',
            options,
        }, 'id');

        expect(apiData.choices[0].label).toBe(options[0].text);
        expect(apiData.choices[1].label).toBe(options[1].text);

        expect(apiData.choices[0].position).toBe(1);
        expect(apiData.choices[1].position).toBe(2);

        expect(apiData.choices[0].hasOwnProperty('id')).toBe(true);
        expect(apiData.choices[1].hasOwnProperty('id')).toBe(true);
        expect(apiData.choices[0].id).not.toBe(apiData.choices[1].id);
    });

    it(`булевый вопрос должен иметь тип ${answerTypes.answer_boolean.id}`, () => {
        const apiData = toApi({
            key: '',
            title: 'Текст',
            description: '',
            type: 'boolean',
        }, 'id');

        expect(apiData.answer_type_id).toBe(answerTypes.answer_boolean.id);
    });

    it(`булевый вопрос должен иметь тип ${answerTypes.answer_boolean.id}`, () => {
        const apiData = toApi({
            key: '',
            title: 'Да/Неут',
            description: '',
            type: 'boolean',
        }, 'id');

        expect(apiData.answer_type_id).toBe(answerTypes.answer_boolean.id);
    });

    it(`числовой вопрос должен иметь тип ${answerTypes.answer_short_text.id}`, () => {
        const apiData = toApi({
            key: '',
            title: 'Число',
            description: '',
            type: 'decimal',
        }, 'id');

        expect(apiData.answer_type_id).toBe(answerTypes.answer_short_text.id);
    });

    it('числовой вопрос должен быть с ID валидатора равным 2', () => {
        const apiData = toApi({
            key: '',
            title: 'Число',
            description: '',
            type: 'decimal',
        }, 'id');

        expect(apiData.validator_type_id).toBe(2);
    });
});

export const QuestionsWithImportants: Partial<FormsAPI.Question>[] = [
    {
        id: 365460,
        answer_type_id: 3,
        page: 1,
        position: 1,
        label: 'Один вариант',
        param_slug: 'answer_choices_365460',
        param_data_source_params: { filters: [
            { type: 'specified_value', filter: { name: 'question' }, value: 365460 },
        ] },
        choices: [
            { id: 523308, survey_question_id: 365460, position: 1, slug: '523308', label: 'Вариант 1', is_hidden: false },
        ],
        param_quiz: { enabled: true, required: false, answers: [
            { scores: 0, correct: false, value: 'Вариант 1' },
        ] },
    },

    {
        id: 397244,
        answer_type_id: 3,
        page: 1,
        position: 2,
        label: 'Несколько вариантов',
        param_slug: 'answer_choices_397244',
        param_is_allow_multiple_choice: true,
        param_data_source_params: { filters: [
            { type: 'specified_value', filter: { name: 'question' }, value: 397244 },
        ] },
        choices: [
            { id: 571788, survey_question_id: 397244, position: 1, slug: '571788', label: 'Вариант 1', is_hidden: false },
            { id: 571789, survey_question_id: 397244, position: 2, slug: '571789', label: 'Вариант 2', is_hidden: false },
        ],
        param_quiz: { enabled: true, required: true, answers: [
            { scores: 10, correct: true, value: 'Вариант 1' },
            { scores: 20, correct: true, value: 'Вариант 2' },
        ] },
    },

    {
        id: 397245,
        answer_type_id: 1,
        validator_type_id: 2,
        page: 1,
        position: 3,
        label: 'Число',
        param_slug: 'answer_short_text_397245',
        param_quiz: { enabled: true, required: false, answers: [
            { scores: 777, correct: true, value: '1.73' },
        ] },
    },

    {
        id: 354696,
        answer_type_id: 1,
        page: 1,
        position: 4,
        label: 'Короткий',
        param_slug: 'answer_short_text_354052',
        param_is_required: true,
        param_help_text: 'Краткое описание проблемы в формате\n`инструмент (или сервис): описание проблемы`',
    },

    {
        id: 397274,
        answer_type_id: 1,
        page: 1,
        position: 5,
        label: '! Короткий с Адресами',
        param_slug: 'answer_short_text_397274',
        param_hint_data_source: 'address',
    },

    {
        id: 397275,
        answer_type_id: 1,
        page: 1,
        position: 6,
        label: '! Короткий с Email',
        param_slug: 'answer_short_text_397275',
        param_hint_data_source: 'user_email_list',
    },

    {
        id: 397276,
        answer_type_id: 1,
        page: 1,
        position: 7,
        label: '! Короткий с Сервисом',
        param_slug: 'answer_short_text_397276',
        param_hint_data_source: 'abc_service',
    },

    {
        id: 397243,
        answer_type_id: 2,
        page: 1,
        position: 8,
        label: 'Длинный текст',
        param_slug: 'answer_long_text_397243',
        param_min: 111,
        param_max: 555,
    },

    {
        id: 385778,
        answer_type_id: 28,
        page: 1,
        position: 9,
        label: '! Статический заголовок',
        param_slug: 'answer_statement_385778',
        param_is_section_header: true,
        param_help_text: 'Комментарий к заголовку',
    },

    {
        id: 365461,
        answer_type_id: 3,
        page: 1,
        position: 10,
        label: '! Несколько вариков',
        param_slug: 'answer_choices_365461',
        param_is_allow_multiple_choice: true,
        param_suggest_choices: true,
        param_data_source_params: { filters: [
            { type: 'specified_value', filter: { name: 'question' }, value: 365461 },
        ] },
        choices: [
            { id: 523309, survey_question_id: 365461, position: 1, slug: '523309', label: 'Вариант 1', is_hidden: false },
            { id: 571790, survey_question_id: 365461, position: 2, slug: '571790', label: 'Вариант 2', is_hidden: false },
            { id: 571791, survey_question_id: 365461, position: 3, slug: '571791', label: 'Вариант 3', is_hidden: false },
            { id: 571792, survey_question_id: 365461, position: 4, slug: '571792', label: 'Вариант 4', is_hidden: false },
        ],
    },

    {
        id: 397269,
        answer_type_id: 3,
        page: 1,
        position: 11,
        label: '! Несколько в случайном',
        param_slug: 'answer_choices_397269',
        param_is_allow_multiple_choice: true,
        param_modify_choices: 'shuffle',
        param_data_source_params: { filters: [
            { type: 'specified_value', filter: { name: 'question' }, value: 397269 },
        ] },
        choices: [
            { id: 571813, survey_question_id: 397269, position: 1, slug: '571813', label: 'Вариант 1', is_hidden: false },
            { id: 571814, survey_question_id: 397269, position: 2, slug: '571814', label: 'Вариант 2', is_hidden: false },
            { id: 571815, survey_question_id: 397269, position: 3, slug: '571815', label: 'Вариант 3', is_hidden: false },
        ],
    },

    {
        id: 372351,
        answer_type_id: 3,
        page: 1,
        position: 12,
        label: '! Селект с сортировкой и NOT NULLABLE',
        param_slug: 'answer_choices_372351',
        param_widget: 'select',
        param_is_disabled_init_item: false,
        param_modify_choices: 'sort',
        param_data_source_params: { filters: [
            { type: 'specified_value', filter: { name: 'question' }, value: 372351 },
        ] },
        choices: [
            { id: 533603, survey_question_id: 372351, position: 1, slug: '533603', label: 'Вариант 1', is_hidden: false },
            { id: 571802, survey_question_id: 372351, position: 2, slug: '571802', label: 'Вариант 2', is_hidden: false },
        ],
    },

    {
        id: 397273,
        answer_type_id: 3,
        page: 1,
        position: 13,
        label: '! Выпадающий список с фильтрацией',
        param_slug: 'answer_choices_397273',
        param_widget: 'select',
        param_suggest_choices: true,
        param_data_source_params: { filters: [
            { type: 'specified_value', filter: { name: 'question' }, value: 397273 },
        ] },
        choices: [
            { id: 571829, survey_question_id: 397273, position: 1, slug: '571829', label: 'Вариант 1', is_hidden: false },
            { id: 571830, survey_question_id: 397273, position: 2, slug: '571830', label: 'Вариант 2', is_hidden: false },
            { id: 571831, survey_question_id: 397273, position: 3, slug: '571831', label: 'Вариант 3', is_hidden: false },
        ],
    },

    {
        id: 384264,
        answer_type_id: 33,
        page: 1,
        position: 14,
        label: 'Да/Нет',
        param_slug: 'answer_boolean_384264',
        param_is_required: true,
        param_min: 1,
        param_max: 1,
    },

    {
        id: 397250,
        answer_type_id: 1,
        validator_type_id: 2,
        page: 1,
        position: 15,
        label: '! Число',
        param_slug: 'answer_short_text_397250',
    },

    {
        id: 397246,
        answer_type_id: 1,
        validator_type_id: 1003,
        page: 1,
        position: 16,
        label: '! Число',
        param_slug: 'answer_short_text_397246',
    },

    {
        id: 397248,
        answer_type_id: 1,
        validator_type_id: 1004,
        page: 1,
        position: 17,
        label: '! Число',
        param_slug: 'answer_short_text_397248',
    },

    {
        id: 397249,
        answer_type_id: 1,
        validator_type_id: 1037,
        validator_options: { regexp: 'regular-expression-here' },
        page: 1,
        position: 18,
        label: '! Число',
        param_slug: 'answer_short_text_397249',
    },

    {
        id: 397247,
        answer_type_id: 31,
        page: 1,
        position: 19,
        label: '! Целое число с ограничением',
        param_slug: 'answer_number_397247',
        param_min: 333,
        param_max: 888,
    },

    {
        id: 397251,
        answer_type_id: 3,
        page: 1,
        position: 20,
        label: '! Оценка по шкале',
        param_slug: 'answer_choices_397251',
        param_widget: 'matrix',
        param_data_source: 'survey_question_matrix_choice',
        matrix_titles: [
            { id: 55142, survey_question_id: 397251, position: 3, type: 'column', label: 'Ответ 3' },
            { id: 55141, survey_question_id: 397251, position: 2, type: 'column', label: 'Ответ 2' },
            { id: 55140, survey_question_id: 397251, position: 1, type: 'column', label: 'Ответ 1' },
            { id: 55139, survey_question_id: 397251, position: 3, type: 'row', label: 'Критерий 3' },
            { id: 55138, survey_question_id: 397251, position: 2, type: 'row', label: 'Критерий 2' },
            { id: 55137, survey_question_id: 397251, position: 1, type: 'row', label: 'Критерий 1' },
        ],
    },

    {
        id: 397252,
        answer_type_id: 32,
        page: 1,
        position: 21,
        label: '! Почта',
        param_slug: 'answer_non_profile_email_397252',
    },

    {
        id: 397253,
        answer_type_id: 35,
        page: 1,
        position: 22,
        label: '! Ссылка',
        param_slug: 'answer_url_397253',
    },

    {
        id: 397254,
        answer_type_id: 38,
        page: 1,
        position: 23,
        label: '! Телефон',
        param_slug: 'answer_phone_397254',
    },

    {
        id: 397255,
        answer_type_id: 34,
        page: 1,
        position: 24,
        label: '! Несколько файлов',
        param_slug: 'answer_files_397255',
    },

    {
        id: 397256,
        answer_type_id: 39,
        page: 1,
        position: 25,
        label: '! Дата',
        param_slug: 'answer_date_397256',
    },

    {
        id: 397259,
        answer_type_id: 39,
        page: 1,
        position: 26,
        label: '! Пара дат',
        param_slug: 'answer_date_397259',
        param_date_field_type: 'daterange',
        param_date_field_min: '2020-07-01',
        param_date_field_max: '2020-07-24',
    },

    {
        id: 397257,
        answer_type_id: 3,
        page: 1,
        position: 27,
        label: '! Города',
        param_slug: 'answer_choices_397257',
        param_data_source: 'city',
        param_data_source_params: { filters: [
            { value: '2', filter: { data_source: 'country', name: 'country' }, type: 'specified_value' },
            { value: '7', filter: { data_source: 'country', name: 'country' }, type: 'specified_value' },
            { value: '23', filter: { data_source: 'country', name: 'country' }, type: 'specified_value' },
        ] },
    },

    {
        id: 397260,
        answer_type_id: 3,
        page: 1,
        position: 28,
        label: '! Страны',
        param_slug: 'answer_choices_397260',
        param_is_allow_multiple_choice: true,
        param_data_source: 'country',
    },

    {
        id: 397258,
        answer_type_id: 1,
        validator_type_id: 1003,
        page: 1,
        position: 29,
        label: '! ИНН',
        param_slug: 'answer_short_text_397258',
    },

    {
        id: 397262,
        answer_type_id: 3,
        page: 2,
        position: 1,
        label: '! Данные стаффа: Несколько логинов',
        param_slug: 'answer_choices_397262',
        param_is_allow_multiple_choice: true,
        param_data_source: 'staff_login',
    },

    {
        id: 386087,
        answer_type_id: 3,
        page: 2,
        position: 2,
        label: '! Данные стаффа: Группа',
        param_slug: 'answer_choices_386087',
        param_data_source: 'staff_group',
    },

    {
        id: 397263,
        answer_type_id: 3,
        page: 2,
        position: 3,
        label: '! Данные стаффа: Орг',
        param_slug: 'answer_choices_397263',
        param_data_source: 'staff_organization',
    },

    {
        id: 397264,
        answer_type_id: 3,
        page: 2,
        position: 4,
        label: '! Данные стаффа: Офис',
        param_slug: 'answer_choices_397264',
        param_data_source: 'staff_office',
    },

    {
        id: 372350,
        answer_type_id: 3,
        page: 2,
        position: 5,
        label: '! Каталог сервисов',
        param_slug: 'answer_choices_372350',
        param_is_allow_multiple_choice: true,
        param_data_source: 'abc_service',
    },

    {
        id: 397265,
        answer_type_id: 3,
        page: 2,
        position: 6,
        label: '! Вики',
        param_slug: 'answer_choices_397265',
        param_data_source: 'wiki_table_source',
        param_data_source_params: { filters: [
            { type: 'specified_value', filter: { data_source: 'free_url', name: 'free_url' }, value: 'https://wiki.yandex-team.ru/lego/components/feedback/' },
        ] },
    },

    {
        id: 397278,
        answer_type_id: 3,
        page: 2,
        position: 7,
        label: '! Вики с фильтрацией',
        param_slug: 'answer_choices_397278',
        param_data_source: 'wiki_table_source',
        param_data_source_params: { filters: [
            { type: 'specified_value', filter: { data_source: 'free_url', name: 'free_url' }, value: 'https://wiki.yandex-team.ru/Lego/Roadmap-Lego/' },
            { type: 'field_value', filter: { data_source: 'wiki_table_source', name: 'wiki_table_source' }, value: null, field: '397265' },
        ] },
    },

    {
        id: 397277,
        answer_type_id: 3,
        page: 2,
        position: 8,
        label: '! Список из YT',
        param_slug: 'answer_choices_397277',
        param_data_source: 'yt_table_source',
        param_data_source_params: { filters: [
            { type: 'specified_value', filter: { data_source: 'free_url', name: 'free_url' }, value: 'https://yt.yandex-team.ru/hahn/navigation?path=//home/logfeller/logs/qloud-runtime-log/1d/2020-01-31' },
        ] },
    },

    {
        id: 397279,
        answer_type_id: 1040,
        page: 2,
        position: 9,
        label: '! Серия вопросов',
        param_slug: 'answer_group_397279',
    },
    {
        id: 397280,
        group_id: 397279,
        answer_type_id: 1,
        page: 2,
        position: 1,
        label: 'Короткий текст',
        param_slug: 'answer_short_text_397280',
    },

    {
        id: 397281,
        answer_type_id: 4,
        page: 3,
        position: 1,
        label: '? Лекции',
        param_slug: 'answer_lectures_397281',
        param_is_required: true,
        param_min: 1,
        param_max: 1,
        param_data_source_params: { filters: [
            { type: 'specified_value', filter: { name: 'question' }, value: 397281 },
        ] },
    },

    {
        id: 397282,
        answer_type_id: 37,
        page: 3,
        position: 2,
        label: '? Имя',
        param_slug: 'answer_name_397282',
        param_min: 0,
        param_max: 1,
        param_data_source_params: { filters: [
            { type: 'specified_value', filter: { name: 'question' }, value: 397282 },
        ] },
    },

    {
        id: 397283,
        answer_type_id: 36,
        page: 3,
        position: 3,
        label: '? Фамилия',
        param_slug: 'answer_surname_397283',
        param_min: 0,
        param_max: 1,
        param_data_source_params: { filters: [
            { type: 'specified_value', filter: { name: 'question' }, value: 397283 },
        ] },
    },

    {
        id: 397284,
        answer_type_id: 27,
        page: 3,
        position: 4,
        label: '? Темы',
        param_slug: 'answer_themes_397284',
        param_min: 0,
        param_max: 1,
        param_is_allow_multiple_choice: true,
        param_data_source_params: { filters: [
            { value: 397284, filter: { name: 'question' }, type: 'specified_value' },
        ] },
    },

    {
        id: 397285,
        answer_type_id: 3,
        page: 3,
        position: 5,
        label: '? Список жанров',
        param_slug: 'answer_choices_397285',
        param_min: 0,
        param_max: 1,
        param_is_allow_multiple_choice: true,
        param_data_source: 'music_genre',
        param_is_disabled_init_item: false,
        param_suggest_choices: true,
    },
];
