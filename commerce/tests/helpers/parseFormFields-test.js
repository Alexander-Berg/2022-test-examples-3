const { expect } = require('chai');
const sinon = require('sinon');

const parseFormFields = require('helpers/parseFormFields');
const log = require('logger');

describe('`Parse fields from form`', () => {
    beforeEach(() => {
        sinon.spy(log, 'error');
    });

    afterEach(() => {
        log.error.restore();
    });

    it('should correct parse field', () => {
        const body = {
            'field_1': JSON.stringify({
                question: { slug: 'email' },
                value: 'expert@yandex.ru'
            }),
            'field_2': JSON.stringify({
                question: { slug: 'message' },
                value: 'Some message from user'
            })
        };

        const actual = parseFormFields(body);

        expect(actual).to.deep.equal({
            email: 'expert@yandex.ru',
            message: 'Some message from user'
        });
        expect(log.error.called).to.be.false;
    });

    it('should do not throw error when parsing fields was failed', () => {
        const actual = parseFormFields({
            field: 'not json'
        });

        expect(actual).to.deep.equal({});
        expect(log.error.calledOnce).to.be.true;
    });
});
