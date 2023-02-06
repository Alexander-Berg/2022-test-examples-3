'use strict';

describe('routes/send/uploader', () => {
    it('Calls multer', () => {
        jest.isolateModules(() => {
            require('./uploader.js');
            const multer = require('multer');
            expect(multer).toHaveBeenCalledTimes(1);
            expect(multer.mock.calls[0][0]).toMatchObject({
                limits: {
                    fileSize: expect.any(Number)
                }
            });
        });
    });
});
