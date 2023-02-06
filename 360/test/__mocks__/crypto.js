'use strict';

const crypto = {
    timingSafeEqual: jest.fn(),
    update: jest.fn().mockReturnThis(),
    digest: jest.fn(),
    createHmac: jest.fn().mockReturnThis()
};

module.exports = crypto;
