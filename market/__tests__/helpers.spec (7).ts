import {FilterParams, ScriptPreview, Scripts, ScriptSources, ScriptTypes, SourceScript} from '../models';
import {
    filterScripts,
    getFilterItem,
    getScriptSectionName,
    prepareScript,
    prepareScriptsData,
    splitBySectionsReducer,
    updateScript,
} from '../helpers';
import {NO_SECTION_TITLE} from '../constants';

const USER_SCRIPT: SourceScript = {
    title: 'Сотрудник::Активный звонок::Заполнить значение',
    source: ScriptSources.USER,
    code: 'actualizeResponsibleEmployeeVoximplantCall',
    type: ScriptTypes.DEFAULT,
    body: 'xxx',
    usages: [],
};

const PREPARED_SCRIPT: ScriptPreview = {
    ...USER_SCRIPT,
    displayTitle: 'Активный звонок::Заполнить значение',
    titleLC: 'сотрудник::активный звонок::заполнить значение',
    codeLC: USER_SCRIPT.code.toLowerCase(),
    typeLC: USER_SCRIPT.type.toLowerCase(),
};

describe('prepareScript', () => {
    it('выдача содержит все нужные поля с правильными значениями', () => {
        const prepared = prepareScript(USER_SCRIPT);

        expect(prepared).toMatchObject(PREPARED_SCRIPT);
    });
});

// =================================================================================================

const TITLE_WITH_SECTION_NAME = 'Сотрудник::Активный звонок::Заполнить значение';
const SECTION_NAME = 'Сотрудник';
const TITLE_WITHOUT_SECTION_NAME = 'attribute: вычисление url для получения содержимого вложения (default)';

describe('getScriptSectionName', () => {
    it('возвращает правильное значение для строк содержащих ::', () => {
        const sectionName = getScriptSectionName(TITLE_WITH_SECTION_NAME);

        expect(sectionName).toEqual(SECTION_NAME);
    });
    it('возвращает правильное значение для строк не содержащих ::', () => {
        const sectionName = getScriptSectionName(TITLE_WITHOUT_SECTION_NAME);

        expect(sectionName).toEqual(NO_SECTION_TITLE);
    });
});

// =================================================================================================

const PREPARED_SECTIONS: Scripts = {
    sectionNames: [SECTION_NAME],
    sections: {
        [SECTION_NAME]: [PREPARED_SCRIPT],
    },
};

describe('splitBySections', () => {
    it('выдача содержит корректно размещенный в секции скрипт', () => {
        const prepared = splitBySectionsReducer({sectionNames: [], sections: {}}, USER_SCRIPT);

        expect(prepared).toMatchObject(PREPARED_SECTIONS);
    });
});

// =================================================================================================

const UPDATED_SCRIPT_TITLE = 'Updated title';
const TO_ADD_SCRIPT: SourceScript[] = [];
const TO_REPLACE_SCRIPT: SourceScript[] = [USER_SCRIPT];
const UPDATED_SCRIPT = {
    ...USER_SCRIPT,
    title: UPDATED_SCRIPT_TITLE,
};

describe('updateScript', () => {
    it('выдача содержит корректно добавленный скрипт', () => {
        const prepared = updateScript(UPDATED_SCRIPT.code, UPDATED_SCRIPT, TO_ADD_SCRIPT);

        expect(prepared).toContain(UPDATED_SCRIPT);
        expect(prepared).toHaveLength(1);
        expect(prepared[0].title).toEqual(UPDATED_SCRIPT_TITLE);
    });
    it('выдача содержит корректно замещенный скрипт', () => {
        const prepared = updateScript(UPDATED_SCRIPT.code, UPDATED_SCRIPT, TO_REPLACE_SCRIPT);

        expect(prepared).toContain(UPDATED_SCRIPT);
        expect(prepared).toHaveLength(1);
        expect(prepared[0].title).toEqual(UPDATED_SCRIPT_TITLE);
    });
});

// =================================================================================================

const USER_SCRIPT1: SourceScript = {
    title: 'Сотрудник::Атрибут::Статистика по оператору',
    source: ScriptSources.SYSTEM,
    code: 'employeeStatistics',
    type: ScriptTypes.DEFAULT,
    body: 'xxx1',
    usages: [],
};

const PREPARED_SCRIPT1: ScriptPreview = {
    ...USER_SCRIPT1,
    displayTitle: 'Атрибут::Статистика по оператору',
    titleLC: USER_SCRIPT1.title.toLowerCase(),
    codeLC: USER_SCRIPT1.code.toLowerCase(),
    typeLC: USER_SCRIPT1.type.toLowerCase(),
};

// @ts-ignore
const {body1, ...SOURCE} = USER_SCRIPT;
// @ts-ignore
const {body2, ...SOURCE1} = USER_SCRIPT1;
// @ts-ignore
const {body3, ...SCRIPT} = PREPARED_SCRIPT;
// @ts-ignore
const {body4, ...SCRIPT1} = PREPARED_SCRIPT1;

describe('prepareScriptsData', () => {
    it('выдача содержит правильную структуру', () => {
        const prepared = prepareScriptsData([SOURCE]);

        expect(prepared.sectionNames).toContain(SECTION_NAME);
        expect(prepared.sections[SECTION_NAME][0]).toMatchObject(SCRIPT);
    });

    it('выдача содержит правильную структуру с учетом сортировки', () => {
        const prepared = prepareScriptsData([SOURCE1, SOURCE]);

        expect(prepared.sectionNames).toContain(SECTION_NAME);
        expect(prepared.sections[SECTION_NAME][0]).toMatchObject(SCRIPT);
        expect(prepared.sections[SECTION_NAME][1]).toMatchObject(SCRIPT1);
    });
});

// =================================================================================================

describe('getFilterItem', () => {
    it('заданный скрипт, содержащий искомое значение в title, фильтруется корректно', () => {
        const fp: FilterParams = {
            filterBy: 'акт',
            isUser: false,
            isOverride: false,
        };
        const filtered = getFilterItem(fp)(PREPARED_SCRIPT);

        expect(filtered).toBeTruthy();
    });
    it('заданный скрипт, содержащий искомое значение в code, фильтруется корректно', () => {
        const fp: FilterParams = {
            filterBy: 'actual',
            isUser: false,
            isOverride: false,
        };
        const filtered = getFilterItem(fp)(PREPARED_SCRIPT);

        expect(filtered).toBeTruthy();
    });
    it('заданный скрипт, содержащий искомое значение в type, фильтруется корректно', () => {
        const fp: FilterParams = {
            filterBy: 'def',
            isUser: false,
            isOverride: false,
        };
        const filtered = getFilterItem(fp)(PREPARED_SCRIPT);

        expect(filtered).toBeTruthy();
    });
    it('заданный скрипт, с типом источника = USER, фильтруется корректно', () => {
        const fp: FilterParams = {
            filterBy: '',
            isUser: true,
            isOverride: false,
        };
        const filtered = getFilterItem(fp)(PREPARED_SCRIPT);

        expect(filtered).toBeTruthy();
    });
    it('заданный скрипт, не содержащий искомое значение в title, code либо type, фильтруется корректно', () => {
        const fp: FilterParams = {
            filterBy: 'xxxxx',
            isUser: false,
            isOverride: false,
        };
        const filtered = getFilterItem(fp)(PREPARED_SCRIPT);

        expect(filtered).not.toBeTruthy();
    });
});

// =================================================================================================

const SCRIPTS = prepareScriptsData([USER_SCRIPT, USER_SCRIPT1]);

describe('filterScripts', () => {
    it('заданный скрипт #1, содержащий искомое значение в title, фильтруется корректно', () => {
        const fp: FilterParams = {
            filterBy: 'акт',
            isUser: false,
            isOverride: false,
        };
        const sectionName = 'Сотрудник';
        const filtered = filterScripts(SCRIPTS, fp);

        expect(filtered.sectionNames).toEqual([sectionName]);
        expect(filtered.sections[sectionName][0]).toMatchObject(PREPARED_SCRIPT);
        expect(filtered.sections[sectionName]).toHaveLength(1);
    });
    it('заданный скрипт #2, содержащий искомое значение в title, фильтруется корректно', () => {
        const fp: FilterParams = {
            filterBy: 'атр',
            isUser: false,
            isOverride: false,
        };
        const sectionName = 'Сотрудник';
        const filtered = filterScripts(SCRIPTS, fp);

        expect(filtered.sectionNames).toEqual([sectionName]);
        expect(filtered.sections[sectionName][0]).toMatchObject(PREPARED_SCRIPT1);
        expect(filtered.sections[sectionName]).toHaveLength(1);
    });
});

// =================================================================================================
