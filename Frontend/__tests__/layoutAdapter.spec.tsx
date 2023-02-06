import { LayoutAdapter, IScheme } from '../layoutAdapter';
import { IAdapterContext } from '../../types/AdapterContext';

enum ELayouts {
    default = 'default',
    foo = 'foo'
}

interface IProps {
    propFoo: string
}

const layouts = {
    [ELayouts.default]: {
        component: jest.fn(),
        bundleName: 'defaultBundle',
        svg: {
            name: 'icon-name',
            icon: 'icon',
        },
    },
    [ELayouts.foo]: {
        hasClient: false,
        component: jest.fn(),
        bundleName: 'fooBundle',
        svg: [
            {
                name: 'icon-name',
                icon: 'icon',
            },
            {
                name: 'icon-name2',
                icon: 'icon2',
            },
        ],
    },
};

class TestAdapter extends LayoutAdapter<IProps, IScheme<ELayouts>, ELayouts> {
    protected layout = ELayouts.default;
    protected layoutList = layouts;
    transform(data) {
        return { propFoo: data.propFoo };
    }
}

const context: IAdapterContext = {
    assets: {
        pushSvgToSprite: jest.fn(),
    },
    // это мок, нам не надо реализовывать все методы
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
} as any;

const data: IScheme<ELayouts> = {
    propFoo: 'string',
    block: 'foo',
};

describe('LayoutAdapter', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('Реализует абстрактные методы адаптера', () => {
        const adapter = new TestAdapter(context);
        const props = adapter.transform(data);

        expect(props).toEqual({ propFoo: 'string' });

        const component = adapter.element(props);

        expect(component).toBeDefined();
        expect(component.props).toEqual(props);
        expect(component.type).toEqual(layouts[ELayouts.default].component);
    });

    test('Выбирается переданый layout', () => {
        const adapter = new TestAdapter(context);
        const scheme = { ...data, layout: ELayouts.foo };
        adapter.processLayout(scheme);
        const props = adapter.transform(scheme);

        expect(adapter.bundleName()).toBe(layouts[ELayouts.foo].bundleName);
        expect(adapter.hasClient(props)).toBe(layouts[ELayouts.foo].hasClient);
    });

    test('Пушится иконка', () => {
        const adapter = new TestAdapter(context);
        adapter.processLayout(data);
        adapter.transform(data);

        expect(context.assets.pushSvgToSprite).toBeCalledWith(layouts[ELayouts.default].svg);
    });

    test('Пушится массив иконок', () => {
        const adapter = new TestAdapter(context);
        const scheme = { ...data, layout: ELayouts.foo };
        adapter.processLayout(scheme);
        adapter.transform(scheme);

        expect(context.assets.pushSvgToSprite).toHaveBeenCalledTimes(2);
        expect(context.assets.pushSvgToSprite).toHaveBeenLastCalledWith(layouts[ELayouts.foo].svg[1]);
    });
});
