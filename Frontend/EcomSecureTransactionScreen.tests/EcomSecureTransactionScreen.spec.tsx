import * as React from 'react';
import 'jest';
import { shallow } from 'enzyme';
import { EcomSecureTransactionScreen } from '../EcomSecureTransactionScreen';

describe('Компонент EcomSecureTransactionScreen', () => {
    it('renders without crashing', () => {
        shallow(
            <EcomSecureTransactionScreen
                title="Безопасные покупки"
                acceptText="Понятно"
                detailsText="Все подробности"
                detailsLink="https://ya.ru"
            >
                {[
                    'Ищите магазины,&nbsp;проверенные Яндексом, они отмечены специальным знаком «Безопасные покупки»',
                    'Добавьте понравившиеся товары в корзину, оформите заказ с предоплатой и получите его вовремя',
                    'Если с заказом что-то пойдёт не так, Яндекс придёт на помощь и вернет деньги',
                ]}
            </EcomSecureTransactionScreen>
        );
    });
});
