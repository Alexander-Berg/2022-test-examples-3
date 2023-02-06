'use strict';

class mockModelRequest {
    constructor(_core, { name }) {
        this.name = name;
        this.sync = /^do-/.test(name);
    }
}

jest.mock('./model-request.js', () => mockModelRequest);

const ModelsQueue = require('./models-queue.js');

describe('models-queue', function() {

    it('поднимает do- хендлеры на верх очереди', function() {
        const models = [
            { name: 'do-1' },
            { name: 'some' },
            { name: 'do-2' },
            { name: 'no-do-1' }
        ];

        const queue = new ModelsQueue(models, void 0);

        expect(queue.models.map((model) => model.name)).toEqual([ 'do-1', 'do-2', 'some', 'no-do-1' ]);
    });

});
