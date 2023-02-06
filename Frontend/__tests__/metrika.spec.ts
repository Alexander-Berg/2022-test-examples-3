import * as bem from '@yandex-turbo/core/BEM';
import { extractMetrikaCounterParams } from '../metrika';

describe('extractMetrikaCounterParams', () => {
    const getBlockStatic = jest.spyOn(bem, 'getBlockStatic');
    const getFromDom = jest.fn();
    const createFakeDomBlock = () => {
        const element = document.createElement('div');

        element.setAttribute('class', 'page__result');
        document.body.appendChild(element);

        return element;
    };
    const removeFakeDomBlock = () => {
        const element = document.querySelector('.page__result');

        if (element) {
            document.body.removeChild(element);
        }
    };

    getBlockStatic.mockResolvedValue({ getFromDom });

    afterEach(() => {
        removeFakeDomBlock();
    });

    it('возвращает пустой объект, если .page__result элемента нет в DOM', async() => {
        const result = await extractMetrikaCounterParams('123');

        expect(result).toEqual({});
    });

    it('возвращает пустой объект, если bem блок не проинициализирован', async() => {
        const element = createFakeDomBlock();

        getFromDom.mockReturnValue(undefined);

        const result = await extractMetrikaCounterParams('123');

        expect(getFromDom).toHaveBeenCalledWith(element);
        expect(result).toEqual({});
    });

    it('возвращает пустой объект, если у счетчика нет базовых параметров', async() => {
        createFakeDomBlock();
        getFromDom.mockReturnValue({});

        const result = await extractMetrikaCounterParams('123');

        expect(getFromDom).toHaveBeenCalled();
        expect(result).toEqual({});
    });

    it('возвращает пустой объект, если опция counters не является списком', async() => {
        createFakeDomBlock();
        getFromDom.mockReturnValue({ params: { counters: {} } });

        const result = await extractMetrikaCounterParams('123');

        expect(getFromDom).toHaveBeenCalled();
        expect(result).toEqual({});
    });

    it('возвращается пустой объект, если нет искомого счетчика', async() => {
        createFakeDomBlock();
        getFromDom.mockReturnValue({
            params: {
                counters: [
                    { id: '555', params: { one: 1, two: 2 } },
                ],
            },
        });

        const result = await extractMetrikaCounterParams('444');

        expect(getFromDom).toHaveBeenCalled();
        expect(result).toEqual({});
    });

    it('возвращает базовые пареметры счетчика', async() => {
        createFakeDomBlock();
        getFromDom.mockReturnValue({
            params: {
                counters: [
                    { id: '444', params: { one: 1, two: 2 } },
                ],
            },
        });

        expect(await extractMetrikaCounterParams('444')).toEqual({ one: 1, two: 2 });
    });
});
