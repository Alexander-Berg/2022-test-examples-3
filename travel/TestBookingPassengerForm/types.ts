export enum ETestGender {
    MALE = 'male',
    FEMALE = 'female',
}

export enum ETestFieldName {
    lastName = 'lastName',
    firstName = 'firstName',
    patronymicName = 'patronymicName',
    sex = 'sex',
    birthdate = 'birthdate',
    documentType = 'documentType',
    documentNumber = 'documentNumber',
    citizenship = 'citizenship',
    documentValidDate = 'documentValidDate',
    /* Addition fields */
    isPatronomicDisabled = 'isPatronomicDisabled',
}

export enum ETestDocumentType {
    RU_NATIONAL_PASSPORT = 'ru_national_passport',
    RU_FOREIGN_PASSPORT = 'ru_foreign_passport',
    RU_BIRTH_CERTIFICATE = 'ru_birth_certificate',
    RU_SEAMAN_PASSPORT = 'ru_seaman_passport',
    RU_MILITARY_ID = 'ru_military_id',
    OTHER = 'other',
}

export interface ITestFormDocument<
    TDocumentType extends ETestDocumentType = ETestDocumentType,
> {
    [ETestFieldName.lastName]?: string;
    [ETestFieldName.firstName]?: string;
    [ETestFieldName.patronymicName]?: string;
    [ETestFieldName.sex]?: ETestGender;
    [ETestFieldName.birthdate]?: string;
    [ETestFieldName.documentType]?: TDocumentType;
    [ETestFieldName.documentNumber]?: string;
    /** Code2 страны */
    [ETestFieldName.citizenship]?: string;
    [ETestFieldName.documentValidDate]?: string;
    [ETestFieldName.isPatronomicDisabled]?: boolean;
}
