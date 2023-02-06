const {createTicketGetter} = require('./index');

describe('config', () => {
    it('_config is not enumerable', () => {
        const config = {
            tvmFile: `${process.cwd()}/tvm.test.json`,
            asker: {
                url: 'http://example.com/ololo',
            }
        };
        const client = createTicketGetter(config);

        expect(Object.keys(client)).not.toContain('_config');
    });

    it('tvm is not enumerable', () => {
        const config = {
            tvmFile: `${process.cwd()}/tvm.test.json`,
            asker: {
                url: 'http://example.com/ololo',
            }
        };
        const client = createTicketGetter(config);

        expect(Object.keys(client._config)).not.toContain('tvm');
    });
});
