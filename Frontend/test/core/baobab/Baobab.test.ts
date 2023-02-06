import { Baobab, IBlockstat } from '../../../src/core/baobab/Baobab';
import { IData, EPlatform } from '../../../platform/types/IData';

describe('Baobab', () => {
    const data: IData = {
        // @ts-ignore
        doc: {
            ab_attrs: {
                attr: 'value',
            },
        },
        reqdata: {
            reqid: '1595850489678028-334391190036758056000254-production-app-host-vla-web-yp-185',
            device: 'touch',
            user_region: {
                id: 1111,
            },
            // @ts-ignore неполные тайпинги
            experiments: {
                ids: ['some-id'],
            },
            url: 'https://example.com',
        },
        // @ts-ignore частичный мок
        cgidata: {
            args: {
                'event-id': ['event-id'],
            },
        },
        env: {
            platform: EPlatform.phone,
            expFlags: {},
            csp: {
                nonce: 'nonce',
            },
        },
    };

    class Blockstat implements IBlockstat {
        counterData = jest.fn()
        writeTree = jest.fn()
    }

    test('Создаёт правильный корень для обычного запроса', () => {
        const blockstat = new Blockstat();
        const baobab = new Baobab({ data, blockstat });

        expect(baobab.createRoot('fullsearch')).toMatchSnapshot();
    });
    test('Создаёт правильный корень для "подстраницы"', () => {
        const blockstat = new Blockstat();
        const baobab = new Baobab({ data, blockstat });

        expect(baobab.createRoot('subpage')).toMatchSnapshot();
    });

    test('Создаёт правильный корень c customBaobabRoot', () => {
        const customBaobabRoot = { url: 'https://another.url' };
        const blockstat = new Blockstat();
        const baobab = new Baobab({
            data: { ...data, doc: { ...data.doc, customBaobabRoot } },
            blockstat,
        });

        expect(baobab.createRoot('fullsearch').attrs!.url).toEqual('https://another.url');
    });

    test('Отправляет событие show в blockstat', () => {
        const blockstat = new Blockstat();
        const baobab = new Baobab({ data, blockstat });
        const event = baobab.sendShowEvent(
            {
                logTree: [
                    {
                        name: '$page',
                        id: 'contant-id',
                        children: [{ name: 'child', id: 'some-id' }],
                    },
                ],
                extrasTree: [{ name: 'extras-node', id: 'extras-id' }],
            },
            'fullsearch'
        );

        expect(event).toMatchSnapshot();
    });
    test('Отправляет событие append в blockstat', () => {
        const blockstat = new Blockstat();
        const baobab = new Baobab({ data, blockstat });
        const event = baobab.sendShowEvent(
            {
                logTree: [{ name: '$page', id: 'contant-id', children: [{ name: 'child', id: 'some-id' }] }],
                extrasTree: [{ name: 'extras-node', id: 'extras-id' }],
            },
            'subpage'
        );

        expect(blockstat.counterData).toBeCalledWith('/turbo/link');
        expect(blockstat.writeTree).toBeCalledWith(event);
        expect(event).toMatchSnapshot();
    });

    test('Создаёт корневой узел при отправке события', () => {
        const blockstat = new Blockstat();
        const baobab = new Baobab({ data, blockstat });
        const event = baobab.sendShowEvent(
            {
                logTree: [{ name: 'block', id: 'some-id' }],
            },
            'fullsearch'
        );

        expect(blockstat.counterData).toBeCalledWith('/turbo/link');
        expect(blockstat.writeTree).toBeCalledWith(event);
        expect(event.tree.name).toBe('$page');
        expect(event.tree.children![0].children).toEqual([{ name: 'block', id: 'some-id' }]);
    });

    test('Отправляет в blockstat extras, даже если дерево пустое', () => {
        const blockstat = new Blockstat();
        const baobab = new Baobab({ data, blockstat });
        const event = baobab.sendShowEvent(
            {
                logTree: [],
            },
            'fullsearch'
        );

        expect(event.tree.children![1].name).toBe('extras');
        expect(event.tree.children![1].children).toBe(undefined);
    });
});
