import assert from 'assert';
import * as VarsTracer from '../src/triggers/bpmn/VarsTracer';
import fs from 'fs';
import path from 'path';
import BpmnModdle from 'bpmn-moddle';

const moddle = new BpmnModdle();

const PRODUCT_ITEMS = {
    name: 'productItems',
    readableName: 'Модели',
    type: 'NUMBER_ARRAY'
};

const RANDOM_VAR = {
    name: 'randomVar',
    readableName: 'Случайная переменная',
    type: 'STRING'
};

const COIN = {
    name: 'coin',
    readableName: 'Монетка',
    type: 'OBJECT'
};

const MESSAGE_ID = {
    name: 'message_id',
    readableName: 'Идентификатор сообщения',
    type: 'STRING'
};

const ORDER_ID = {
    name: 'order_id',
    readableName: 'Идентификатор потерянного заказа',
    type: 'NUMBER'
};

const MADE_UP_VARIABLE = {
    name: 'madeUpVar',
    readableName: 'Придуманная переменная',
    type: 'STRING'
};

const CART_ITEM_CHANGE = {
    name: 'cartItemChange',
    readableName: 'Модель, измененная в корзине',
    type: 'OBJECT'
};

const VARIABLES_INFO = {
    delegates: {
        'cartItemChangeListener': {
            sets: [
                PRODUCT_ITEMS,
                RANDOM_VAR
            ]
        },
        'sendEmailTrigger': {
            sets: [MESSAGE_ID]
        },
        'madeUpDelegate1': {
            sets: [
                {
                    ...MADE_UP_VARIABLE,
                    optional: true
                }
            ]
        },
        'madeUpDelegate2': {
            sets: [MADE_UP_VARIABLE]
        },
        'madeUpDelegate3': {
            removes: [MADE_UP_VARIABLE]
        },
        'madeUpDelegate4': {
            removes: [
                {
                    ...MADE_UP_VARIABLE,
                    optional: true
                }
            ]
        }
    },
    messages: {
        'COIN_CREATED': {
            sets: [COIN]
        },
        'CART_ITEM_ADDED': {
            sets: [CART_ITEM_CHANGE]
        },
        'platform_OrderWasLost': {
            sets: [ORDER_ID]
        },
        'madeUpMessage1': {
            sets: [MADE_UP_VARIABLE]
        },
        'madeUpMessage2': {
            sets: [
                {
                    ...MADE_UP_VARIABLE,
                    optional: true
                }
            ]
        },
        'madeUpMessage3': {
            // Кей поддрержан "для порядка". На самом деле сообщения не могут удалять переменные
            removes: [MADE_UP_VARIABLE]
        }
    }
};

let cartProcessModel;

const loadModel = file =>
    new Promise((resolve, reject) => {
        const xml = fs.readFileSync(
            path.resolve(__dirname, 'assets/' + file),
            { encoding: 'UTF-8' }
        );

        moddle.fromXML(xml, (err, model) => {
            if (err) {
                reject(err);
            } else {
                resolve(model)
            }
        });
    });

before(() =>
    loadModel('cart_process.xml').then(model => {
        cartProcessModel = model
    })
);

const getBlockById = (id, model) => {
    const process = model.rootElements[0];
    return process.flowElements.find(e => e.id === id);
};

describe('VarsTracer', () => {

    it('should return vars of preceding start event with specified delegate', () => {
        const link = getBlockById('SequenceFlow_0c3ywxk', cartProcessModel);

        const expectedVars = [CART_ITEM_CHANGE, PRODUCT_ITEMS, RANDOM_VAR];
        const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;

        assert.deepStrictEqual(actualVars, expectedVars);
    });

    it('should return vars of preceding start event without specified delegate', () =>
        loadModel('coin_process.xml').then(model => {
            const link = getBlockById('SequenceFlow_1bn7bh2', model);
            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;
            assert.deepStrictEqual(actualVars, [COIN]);
        })
    );

    it('should return vars from all preceding blocks', () =>
        loadModel('coin_process.xml').then(model => {
            const link = getBlockById('SequenceFlow_15hq71v', model);
            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;
            assert.deepStrictEqual(actualVars, [COIN, MESSAGE_ID]);
        })
    );

    it('should process cyclic path with same variables', () => {
        const link = getBlockById('SequenceFlow_19ins0v', cartProcessModel);

        const expectedVars = [CART_ITEM_CHANGE, PRODUCT_ITEMS, RANDOM_VAR];
        const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;

        assert.deepStrictEqual(actualVars, expectedVars);
    });

    it('should mark event loop path variables as optional', () =>
        loadModel('loop_process.xml').then(model => {
            const link = getBlockById('SequenceFlow_0bpx9k5', model);

            const expectedVars = [
                CART_ITEM_CHANGE,
                {
                    ...COIN,
                    optional: true
                },
                PRODUCT_ITEMS,
                RANDOM_VAR
            ];
            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;

            assert.deepStrictEqual(actualVars, expectedVars);
        })
    );

    it('should return loop variables even for links in the beginning of loop', () =>
        loadModel('loop_process.xml').then(model => {
            const link = getBlockById('SequenceFlow_1r2fa7x', model);

            const expectedVars = [
                CART_ITEM_CHANGE,
                {
                    ...COIN,
                    optional: true
                },
                PRODUCT_ITEMS,
                RANDOM_VAR
            ];
            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;

            assert.deepStrictEqual(actualVars, expectedVars);
        })
    );

    it('should mark loop variables in the end of the loop as not optional', () =>
        loadModel('loop_process.xml').then(model => {
            const link = getBlockById('SequenceFlow_12jl58i', model);

            const expectedVars = [CART_ITEM_CHANGE, COIN, PRODUCT_ITEMS, RANDOM_VAR];
            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;

            assert.deepStrictEqual(actualVars, expectedVars);
        })
    );

    it('should return variables of all loops in possible cyclic paths', () =>
        loadModel('double_loop_process.xml').then(model => {
            const link = getBlockById('SequenceFlow_0ilti6l', model);

            const expectedVars = [
                CART_ITEM_CHANGE,
                {
                    ...COIN,
                    optional: true
                },
                {
                    ...ORDER_ID,
                    optional: true
                },
                PRODUCT_ITEMS,
                RANDOM_VAR
            ];
            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;

            assert.deepStrictEqual(actualVars, expectedVars);
        })
    );

    it('should mark different variables from two possible starts as optional', () =>
        loadModel('two_starts_process.xml').then(model => {
            const link = getBlockById('SequenceFlow_1c2iv78', model);

            const expectedVars = [
                {
                    ...CART_ITEM_CHANGE,
                    optional: true
                },
                {
                    ...COIN,
                    optional: true
                },
                MESSAGE_ID,
                {
                    ...PRODUCT_ITEMS,
                    optional: true
                },
                {
                    ...RANDOM_VAR,
                    optional: true
                }
            ];
            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;

            assert.deepStrictEqual(actualVars, expectedVars);
        })
    );

    it('should mark variable as optional if it came as optional from one of the paths', () =>
        loadModel('two_starts_process_2.xml').then(model => {
            const block = getBlockById('ServiceTask_16xovs3', model);

            const expectedVars = [
                {
                    ...MADE_UP_VARIABLE,
                    optional: true
                }
            ];
            const actualVars = VarsTracer.getVars(block, VARIABLES_INFO).input;

            assert.deepStrictEqual(actualVars, expectedVars);
        })
    );

    it('should override optional variable status if last set was not optional', () =>
        loadModel('override_opt_process.xml').then(model => {
            const link = getBlockById('SequenceFlow_1kh4lxx', model);
            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;
            assert.deepStrictEqual(actualVars, [COIN, MADE_UP_VARIABLE]);
        })
    );

    it('should not override not optional variable status if last set wat optional', () =>
        loadModel('do_not_override_opt_process.xml').then(model => {
            const link = getBlockById('SequenceFlow_1kh4lxx', model);
            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;
            assert.deepStrictEqual(actualVars, [COIN, MADE_UP_VARIABLE]);
        })
    );

    it('start event listener should override event variable optional status if its var is not optional', () =>
        loadModel('start_end_process_1.xml').then(model => {
            const link = getBlockById('SequenceFlow_15yq4me', model);
            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;
            assert.deepStrictEqual(actualVars, [MADE_UP_VARIABLE]);
        })
    );

    it('start event listener should not override event variable not optional status if its var is optional', () =>
        loadModel('start_end_process_2.xml').then(model => {
            const link = getBlockById('SequenceFlow_15yq4me', model);
            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;
            assert.deepStrictEqual(actualVars, [MADE_UP_VARIABLE]);
        })
    );

    it('should connect border event path with previous blocks', () =>
        loadModel('email_with_error_process.xml').then(model => {
            const link = getBlockById('SequenceFlow_0lkburj', model);
            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;

            assert.deepStrictEqual(actualVars, [COIN]);
        })
    );

    it('block, removing variable, should not let it further', () =>
        loadModel('removing_var_block_process.xml').then(model => {
            const link = getBlockById('SequenceFlow_0t1amv4', model);
            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;

            assert.deepStrictEqual(actualVars, []);
        })
    );

    it('block, optionally removing not optional variable, should mark it as optional', () =>
        loadModel('opt_removing_var_block_process.xml').then(model => {
            const link = getBlockById('SequenceFlow_0t1amv4', model);
            const expectedVars = [
                {
                    ...MADE_UP_VARIABLE,
                    optional: true
                }
            ];
            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;

            assert.deepStrictEqual(actualVars, expectedVars);
        })
    );

    it('listener, removing variable from message, should not let is further', () =>
        loadModel('start_end_process_3.xml').then(model => {
            const link = getBlockById('SequenceFlow_15yq4me', model);
            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;
            assert.deepStrictEqual(actualVars, []);
        })
    );

    it('listener, optionally removing variable from message, should mark it as optional', () =>
        loadModel('start_end_process_4.xml').then(model => {
            const link = getBlockById('SequenceFlow_15yq4me', model);

            const expectedVars = [
                {
                    ...MADE_UP_VARIABLE,
                    optional: true
                }
            ];

            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;
            assert.deepStrictEqual(actualVars, expectedVars);
        })
    );

    it('removing listener should remove variable from previous path', () =>
        loadModel('loop_process_2.xml').then(model => {
            const link = getBlockById('SequenceFlow_0cl6jah', model);

            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;
            assert.deepStrictEqual(actualVars, []);
        })
    );

    it('if listener adds variable it should neutralize removing same from message', () =>
        loadModel('loop_process_3.xml').then(model => {
            const link = getBlockById('SequenceFlow_0cl6jah', model);

            const actualVars = VarsTracer.getVars(link, VARIABLES_INFO).input;
            assert.deepStrictEqual(actualVars, [MADE_UP_VARIABLE]);
        })
    );

    it('should return output variables', () =>
        loadModel('coin_process.xml').then(model => {
            const task = getBlockById('ServiceTask_062a3m1', model);

            const expectedVars = [COIN, MESSAGE_ID];
            const actualVars = VarsTracer.getVars(task, VARIABLES_INFO).output;

            assert.deepStrictEqual(actualVars, expectedVars);
        })
    );
});
