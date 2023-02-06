import * as React from 'react';
import 'jest';
import { shallow } from 'enzyme';
import { ProductsCarousel } from '../ProductsCarousel';

describe('ProductsCarousel', () => {
    it('Рендерится без ошибок', () => {
        shallow(
            <ProductsCarousel title={'Carousel Title'}>
                <div>1</div>
                <div>2</div>
                <div>3</div>
                <div>4</div>
            </ProductsCarousel>
        );
    });
});
