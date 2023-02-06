import {screen} from '@testing-library/dom';

const ESTIMATED_DELIVERY_TITLE_REGEX = /^.*?(с \d{1,2} [а-я]+).*?$/;

export const containsUniqueOffers = async widgetContainer => {
    await step('Cрок доставки указан в формате "... с D M"', async () =>
        expect(screen.getByRole('heading', {name: ESTIMATED_DELIVERY_TITLE_REGEX}))
    );
};

export const containsEstimatedOffers = async widgetContainer => {
    await step('Cрок доставки указан в формате "... с D M"', async () =>
        expect(screen.getByRole('heading', {name: ESTIMATED_DELIVERY_TITLE_REGEX}))
    );
};
