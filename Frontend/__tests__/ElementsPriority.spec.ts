import { ElementsPriority } from '../ElementsPriority';

const elements = new ElementsPriority({
    advert: {
        priority: 90,
        exclude: {
            advert: true,
            socialPanel: true,
        },
    },
    socialPanel: {
        priority: 50,
    },
    testElement: {
        priority: 70,
    },
});

describe('elementsPriority', () => {
    test('Приходят верные уведомления для подписанных элементов', async() => {
        const advertCallback = jest.fn();
        const socialPanelCallback = jest.fn();
        const testElementCallback = jest.fn();

        const socialPanelId = elements.registerElement({ name: 'socialPanel', callback: socialPanelCallback });
        const advertBlockId = elements.registerElement({ name: 'advert', callback: advertCallback });
        const testElementId = elements.registerElement({ name: 'testElement', callback: testElementCallback });

        elements.setReadyToShow(socialPanelId, true);
        elements.setReadyToShow(advertBlockId, true);
        elements.setReadyToShow(testElementId, true);

        elements.setReadyToShow(testElementId, false);
        elements.setReadyToShow(testElementId, true);

        // У socialPanel приоритет меньше. Когда она регистрируется первой - ей приходит уведомление с предложением показаться
        // Т.к. она одна в очереди на тот момент. Но когда после регистрируется advert блок с большим приоритетом
        // И исключающий socialPanel. В socialPanel приходит еще одно уведомление - на этот раз с предложением скрыться
        expect(testElementCallback.mock.calls.length).toBe(3);
        expect(socialPanelCallback.mock.calls.length).toBe(2);
        expect(advertCallback.mock.calls.length).toBe(1);

        expect(socialPanelCallback.mock.calls[0][0]).toStrictEqual({ visible: true, priority: 50 });
        expect(socialPanelCallback.mock.calls[1][0]).toStrictEqual({ visible: false, priority: 50 });
        expect(advertCallback.mock.calls[0][0]).toStrictEqual({ visible: true, priority: 90 });

        expect(testElementCallback.mock.calls[0][0]).toStrictEqual({ visible: true, priority: 70 });
        expect(testElementCallback.mock.calls[1][0]).toStrictEqual({ visible: false, priority: 70 });
        expect(testElementCallback.mock.calls[2][0]).toStrictEqual({ visible: true, priority: 70 });
    });
});
