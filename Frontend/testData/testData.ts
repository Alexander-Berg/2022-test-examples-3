import { WizardFormData } from '../../WizardForm/WizardForm.container';
import { ServiceCreationResponseError } from '../../../ServiceCreation.types';
import { MessageError } from '../PreviewStep';

export const testFormData: WizardFormData = {
    general: {
        name: 'Название',
        englishName: 'English Name',
        slug: 'sluuug',
        owner: { id: 'aa', title: 'testOwner' },
        parent: { id: 11, name: { ru: 'testName', en: 'testName' }, status: 'develop' },
        tags: [{ id: 33, name: 'testTag1', color: '#FFFFFF' }, { id: 34, name: 'testTag2', color: '#000000' }],
    },
    description: {
        description: 'Это описание прекрасного сервиса',
        englishDescription: 'It is really cool service',
    },
};

export const testFinalErrors: ServiceCreationResponseError = {
    detail: 'Ошибка валидации.',
    code: 'invalid',
    extra: {
        slug: ['Это поле не может быть пустым.'],
        name: ['Это поле не может быть пустым.'],
    },
};

export const testMessageErrorData: MessageError = {
    detail: 'Ошибка валидации.',
    code: 'invalid',
    extra: {
        slug: {
            label: 'i18n:slug',
            values: 'Это поле не может быть пустым.',
        },
        name: {
            label: 'i18n:name',
            values: 'Это поле не может быть пустым.',
        },
    },
};
