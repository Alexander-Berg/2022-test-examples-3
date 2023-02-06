import { producer, objectComposer, entityCreatorFactory } from './utils';
import { generateGuid } from './common';
import { Compose, ComposeState } from '../../compose';

type ComposeTemplate = Partial<Compose> & { chatId?: string };

const ID_SYMBOL = Symbol('id');

const composeStateProducerFactory = producer<
    ComposeTemplate,
    ComposeState,
    Compose
>(
    objectComposer<ComposeTemplate, ComposeState>((compose) => compose[ID_SYMBOL]),
);

export const composeFactory = (props: ComposeTemplate | undefined): Compose => {
    if (!props?.chatId) {
        throw new Error('chatId is required');
    }

    const {
        chatId = generateGuid(),
        ...rest
    } = props || {};

    const compose = {
        ...rest,
    };

    Object.defineProperty(compose, ID_SYMBOL, {
        value: chatId,
        enumerable: false,
        writable: false,
    });

    return compose;
};

function entityCreatorFactoryProducer() {
    return (common: ComposeTemplate = {}) =>
        entityCreatorFactory((data: ComposeTemplate | string | undefined) => {
            const chatId = typeof data === 'string' ? data : (data?.chatId || generateGuid());

            return composeFactory({
                ...common,
                ...(typeof data === 'string' ? { chatId } : { ...data, chatId }),
            });
        });
}

export function composeMockFactory() {
    return {
        createCompose: entityCreatorFactoryProducer(),
        createState: composeStateProducerFactory(composeFactory),
    };
}
