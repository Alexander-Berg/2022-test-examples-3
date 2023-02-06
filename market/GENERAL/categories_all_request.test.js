
import {Categories} from '../Categories';

jest.mock('../resolvers/resolveCategories');
jest.mock('../resolvers/resolveRecomContext');
import {resolveCategories} from '../resolvers/resolveCategories';
import {resolveRecomContext} from '../resolvers/resolveRecomContext';

const category = new Categories();

describe('Тестируем полный проход запроса за результатами', () => {
    const categories = [
        {formula1: 21,},
        {formula1: 22,},
    ]
    const recomContext = ''
    resolveCategories.mockReturnValue(Promise.resolve({
        result: {categories},
        context: {
            id: 212121,
            time: 2121121441,
        },
    }));
    resolveRecomContext.mockReturnValue(Promise.resolve({
        range: 1,
        context: 'awdawdawdawdwd',
    }));

    test('при withContext=false отдает рез-ты без запроса контекста', async () => {
        const results = await category.resolveResults({
            useContextMock: true,
            withContext: false,
        })

        const defaultParams = {
            context_experiment: undefined,
            dj_viewer_toloka_experiment: undefined,

            experiment: undefined,
            rearrFactors: undefined,
            countOfCategories: undefined,
            urlPathAddition: undefined,
        };

        expect(resolveCategories).toHaveBeenCalledTimes(1);
        expect(resolveCategories).toHaveBeenCalledWith(defaultParams);

        expect(resolveRecomContext).toHaveBeenCalledTimes(0);
    });

});
