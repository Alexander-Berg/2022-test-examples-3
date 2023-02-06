import 'jest';
import * as React from 'react';
import * as enzyme from 'enzyme';

import { Price, PriceBlock } from '../Price';
import { costAsString } from '../../../../Cost/Cost.utils';

/** Делаем из costAsString stub. */
const { ECurrencyAvailable } = jest.requireActual('../../../../Cost/Cost.types');
jest.mock('../../../../Cost/Cost.utils');

describe('CartFormRadioItemPrice', () => {
    const { costAsString: mockedCostAsString }: {
        costAsString: jest.MockedFunction<typeof costAsString>
    } = require('../../../../Cost/Cost.utils');

    afterEach(() => {
        mockedCostAsString.mockReset();
    });

    describe('Соответствует snapshot', () => {
        afterEach(() => {
            mockedCostAsString.mockReset();
        });

        it('Ненулевая цена, без замещающего текста', () => {
            mockedCostAsString.mockReturnValue('100 тестовых рублей');
            const price = enzyme.shallow(<Price value={100} currencyId={ECurrencyAvailable.RUB} />);
            expect(price).toMatchSnapshot();
        });

        it('Ненулевая цена, с пустым замещающим текстом', () => {
            mockedCostAsString.mockReturnValue('100 тестовых рублей');
            const freeText = '';
            const price = enzyme.shallow(
                <Price value={100} currencyId={ECurrencyAvailable.RUB} freeText={freeText} />
            );
            expect(price).toMatchSnapshot();
        });

        it('Ненулевая цена, с непустым замещающим текстом', () => {
            mockedCostAsString.mockReturnValue('100 тестовых рублей');
            const freeText = 'Только сегодня совершенно бесплатно!';
            const price = enzyme.shallow(
                <Price value={100} currencyId={ECurrencyAvailable.RUB} freeText={freeText} />
            );
            expect(price).toMatchSnapshot();
        });

        it('Нулевая цена, без замещающего текста', () => {
            mockedCostAsString.mockReturnValue('0 тестовых рублей');
            const freeText = 'Только сегодня совершенно бесплатно!';
            const price = enzyme.shallow(
                <Price value={100} currencyId={ECurrencyAvailable.RUB} freeText={freeText} />
            );
            expect(price).toMatchSnapshot();
        });

        it('Нулевая цена, с пустым замещающим текстом', () => {
            mockedCostAsString.mockReturnValue('0 тестовых рублей');
            const freeText = '';
            const price = enzyme.shallow(
                <Price value={0} currencyId={ECurrencyAvailable.RUB} freeText={freeText} />
            );
            expect(price).toMatchSnapshot();
        });

        it('Нулевая цена, с непустым замещающим текстом', () => {
            mockedCostAsString.mockReturnValue('0 тестовых рублей');
            const freeText = 'Только сегодня совершенно бесплатно!';
            const price = enzyme.shallow(
                <Price value={0} currencyId={ECurrencyAvailable.RUB} freeText={freeText} />
            );
            expect(price).toMatchSnapshot();
        });

        it('Диапазон цен', () => {
            mockedCostAsString.mockReturnValue('0 тестовых рублей');
            const price = enzyme.shallow(
                <Price minValue={0} maxValue={100} currencyId={ECurrencyAvailable.RUB} />
            );
            expect(price).toMatchSnapshot();
        });
    });

    it('Передает параметры цены в costAsString', () => {
        mockedCostAsString.mockReturnValue('100 тестовых рублей');
        const priceParams = {
            value: 100,
            currencyId: ECurrencyAvailable.RUB,
        };
        enzyme.shallow(<PriceBlock {...priceParams} />);
        expect(mockedCostAsString).toHaveBeenCalledTimes(1);
        expect(mockedCostAsString).toHaveBeenCalledWith(priceParams);
    });
});
