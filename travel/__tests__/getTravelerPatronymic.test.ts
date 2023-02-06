import {IDocumentDTO} from 'server/api/TravelersApi/types/IDocumentDTO';

import getTravelerPatronymic from 'projects/trains/lib/order/traveler/patchServerResponse/getTravelerPatronymic';

const document = {
    middle_name: 'Александрович',
    middle_name_en: 'Alexandrovich',
    first_name: 'Александ',
    first_name_en: 'Alex',
    last_name: 'Александров',
    last_name_en: 'Alexandrov',
    type: 'ru_national_passport',
} as IDocumentDTO;

describe('getTravelerPatronymic', () => {
    it('Кириллическое отчество задано - вернем отчество на кириллице', () => {
        expect(getTravelerPatronymic(document)).toEqual('Александрович');
    });

    it('Кириллического отчества нет, задано отчество на латинице - веренем отчество на латинице', () => {
        expect(
            getTravelerPatronymic({
                ...document,
                middle_name: undefined,
            }),
        ).toEqual('Alexandrovich');
    });

    it('Отчество не задано, документ с поддержкой кириллических символов, нету кирилличекого имени - оставим отчество пустым', () => {
        expect(
            getTravelerPatronymic({
                ...document,
                middle_name: undefined,
                middle_name_en: undefined,
                first_name: undefined,
            }),
        ).toEqual('');
    });

    it('Отчество не задано, документ с поддержкой кириллических символов, нет кириллической фамилии - оставим отчество пустым', () => {
        expect(
            getTravelerPatronymic({
                ...document,
                middle_name: undefined,
                middle_name_en: undefined,
                last_name: undefined,
            }),
        ).toEqual('');
    });

    it('Отчество не задано, документ с поддержкой кириллических символов, есть и фамилия и имя - выставим в отчество дефис', () => {
        expect(
            getTravelerPatronymic({
                ...document,
                middle_name: undefined,
                middle_name_en: undefined,
            }),
        ).toEqual('-');
    });

    it('Отчество не задано, документ с поддержкой кириллических и латинских символов - выставим в отчество дефис', () => {
        expect(
            getTravelerPatronymic({
                ...document,
                middle_name: undefined,
                middle_name_en: undefined,
            }),
        ).toEqual('-');
    });
});
