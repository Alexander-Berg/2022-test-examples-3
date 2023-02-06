'use strict';

const modifyWidgetFavicons = require('./modify-widget-favicons');

const coreConsole = {
    log: jest.fn()
};

describe('should not modify favicon url', function() {
    it('if widget doesn\'t have field "controls"', function() {
        const widget = {
            test: 'test-field'
        };

        modifyWidgetFavicons(widget, coreConsole);

        expect(widget).toEqual({ test: 'test-field' });
    });

    it('if control doesn\'t have url of link', function() {
        const widget = {
            controls: [
                {
                    attributes: {
                        favicon: 'https://yastat.net/favicon/v2/https%3A%2F%2Fwww.ozon.ru%2Fdetails%3Fv%3D7tmvVZVckZ4'
                    }
                }
            ]
        };

        modifyWidgetFavicons(widget, coreConsole);

        expect(widget).toEqual({
            controls: [
                {
                    attributes: {
                        favicon: 'https://yastat.net/favicon/v2/https%3A%2F%2Fwww.ozon.ru%2Fdetails%3Fv%3D7tmvVZVckZ4'
                    }
                }
            ]
        });
    });

    it('if control doesn\'t have favicon', function() {
        const widget = {
            controls: [
                {
                    attributes: {
                        url: 'https://www.ozon.ru/details/id/123'
                    }
                }
            ]
        };

        modifyWidgetFavicons(widget, coreConsole);

        expect(widget).toEqual({
            controls: [
                {
                    attributes: {
                        url: 'https://www.ozon.ru/details/id/123'
                    }
                }
            ]
        });
    });

    it('if it is link to service, which has different favicon depends on url', function() {
        const widget = {
            controls: [
                {
                    attributes: {
                        url: 'https://www.yandex.com.am/details/id/123',
                        favicon: 'https://yastat.net/favicon/v2/https%3A%2F%2Fwww.yandex.com.am%2F' +
                            'details%3Fv%3D7tmvVZVckZ4'
                    }
                }
            ]
        };

        modifyWidgetFavicons(widget, coreConsole);

        expect(widget).toEqual({
            controls: [
                {
                    attributes: {
                        url: 'https://www.yandex.com.am/details/id/123',
                        favicon: 'https://yastat.net/favicon/v2/https%3A%2F%2Fwww.yandex.com.am%2F' +
                            'details%3Fv%3D7tmvVZVckZ4'
                    }
                }
            ]
        });
    });

    it('if url of link is incorrect', function() {
        const widget = {
            controls: [
                {
                    attributes: {
                        url: 'incorrect_link',
                        favicon: 'https://yastat.net/favicon/v2/https%3A%2F%2Fwww.yandex.com.am%2F' +
                            'details%3Fv%3D7tmvVZVckZ4'
                    }
                }
            ]
        };

        modifyWidgetFavicons(widget, coreConsole);

        expect(widget).toEqual({
            controls: [
                {
                    attributes: {
                        url: 'incorrect_link',
                        favicon: 'https://yastat.net/favicon/v2/https%3A%2F%2Fwww.yandex.com.am%2F' +
                            'details%3Fv%3D7tmvVZVckZ4'
                    }
                }
            ]
        });
        expect(coreConsole.log).toHaveBeenCalledWith(
            'CAN_NOT_MODIFY_FAVICON', { message: expect.stringContaining('') }
        );
    });
});

describe('should modify url to favicon', function() {
    it('if widget passed all conditions', function() {
        const widget = {
            controls: [
                {
                    attributes: {
                        url: 'https://www.ozon.ru/details/id/123',
                        favicon: 'https://yastat.net/favicon/v2/https%3A%2F%2Fwww.ozon.ru%2Fdetails%3Fv%3D7tmvVZVckZ4'
                    }
                },
                {
                    attributes: {
                        url: 'https://www.yandex.com.am/details/id/123',
                        favicon: 'https://yastat.net/favicon/v2/https%3A%2F%2Fwww.yandex.com.am%2F' +
                            'details%3Fv%3D7tmvVZVckZ4'
                    }
                }
            ]
        };

        modifyWidgetFavicons(widget, coreConsole);

        expect(widget).toEqual({
            controls: [
                {
                    attributes: {
                        url: 'https://www.ozon.ru/details/id/123',
                        favicon: 'https://yastat.net/favicon/v2/www.ozon.ru'
                    }
                },
                {
                    attributes: {
                        url: 'https://www.yandex.com.am/details/id/123',
                        favicon: 'https://yastat.net/favicon/v2/https%3A%2F%2Fwww.yandex.com.am%2F' +
                            'details%3Fv%3D7tmvVZVckZ4'
                    }
                }
            ]
        });
    });
});
