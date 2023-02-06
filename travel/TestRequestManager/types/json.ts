export interface IJsonArrayEntity {
    [index: number]: TJsonEntity;
}

export interface IJsonObjectEntity {
    [key: string]: TJsonEntity;
}

export type TJsonEntity =
    | number
    | string
    | boolean
    | null
    | IJsonArrayEntity
    | IJsonObjectEntity;
