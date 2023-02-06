import type { CheckAnswerFn } from 'hooks/useCategoryTestController/types';

export const defaultCheckAnswer: CheckAnswerFn = option => {
    return Promise.resolve({
        categories: option.categories,
    });
};
