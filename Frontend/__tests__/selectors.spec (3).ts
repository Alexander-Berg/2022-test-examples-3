import { selectWidgets, selectCollections } from '../selectors';

describe('store/selectors', () => {
    const store = {
        'beru.ru': {
            widgets: {
                AnyWidget: {},
            },
            collections: {
                anyCollection: {},
            },
        },
    };

    describe('selectWidgets', () => {
        it('достает из redux стора срез, в котором хранятся виджеты', () => {
            expect(selectWidgets(store)).toEqual({ AnyWidget: {} });
        });
    });

    describe('selectCollections', () => {
        it('достает из redux стора срез, в котором хранятся коллекции сущностей', () => {
            expect(selectCollections(store)).toEqual({ anyCollection: {} });
        });
    });
});
