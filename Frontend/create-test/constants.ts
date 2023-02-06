import { State } from 'client/components/create-test/types';

export const AMOUNT_OF_DATA_ROWS = 4;

export enum FIELD_NAMES {
    INPUT_PATH = 'inputPath',
    INPUT_CONTENT = 'inputContent',
    OUTPUT_PATH = 'outputPath',
    OUTPUT_CONTENT = 'outputContent',
}

export const EMPTY_STATE: State = {
    data: {
        inputPath: '',
        inputContent: '',
        outputPath: '',
        outputContent: '',
    },
};

/**
 * Валидные данные для сброшенной формы
 * Нужны, чтобы при закрытии модалки с формой исчезли валидационные сообщения
 */
export const RESET_STATE: State = {
    data: {
        inputPath: ' ',
        inputContent: ' ',
        outputPath: ' ',
        outputContent: ' ',
    },
};
