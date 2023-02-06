import Backend from '../../app/backend';

jest.mock('../../app/secrets');
jest.mock('@ps-int/ufo-server-side-commons/tskv/default-logger');
jest.mock('asker-as-promised', () => jest.fn());
import mockedAsker from 'asker-as-promised';

describe('app/backend', () => {
    let backend;
    const req = {
        ua: {
            isMobile: false,
            BrowserName: 'YandexBrowser',
            BrowserVersion: '19.9.0.1768'
        },
        parsedUrl: {},
        token: {},
        user: {},
        tvmTickets: {}
    };
    beforeEach(() => {
        backend = new Backend(req);
    });

    describe('urlinfo', () => {
        it('should return `state: FAIL` if 500', (done) => {
            mockedAsker.mockResolvedValue({
                statusCode: 500
            });
            backend.urlinfo('test-url').then((result) => {
                expect(result).toEqual({
                    code: 'HTTP_500',
                    error: true,
                    statusCode: 500,
                    data: {
                        state: 'FAIL',
                        errorCode: 500,
                        reason: 'HTTP_500'
                    }
                });
                done();
            });
        });

        it('should return `state: RESTART_WITH_NOIFRAME_NEEDED` if preferHTMLWithImages got', (done) => {
            mockedAsker.mockResolvedValue({
                statusCode: 200,
                data: JSON.stringify({
                    state: {
                        prefer_html_with_images: true
                    }
                })
            });
            backend.urlinfo('test-url').then((result) => {
                expect(result).toEqual({
                    error: false,
                    statusCode: 200,
                    code: '',
                    data: {
                        preferHTMLWithImages: true,
                        state: 'RESTART_WITH_NOIFRAME_NEEDED',
                        reason: 'PREFER_HTML_WITH_IMAGES'
                    }
                });
                done();
            });
        });

        it('should return `WAIT / COPY` if `state: COPYING` got', (done) => {
            mockedAsker.mockResolvedValue({
                statusCode: 200,
                data: JSON.stringify({
                    state: {
                        state: 'COPYING'
                    }
                })
            });
            backend.urlinfo('test-url').then((result) => {
                expect(result).toEqual({
                    error: false,
                    statusCode: 200,
                    code: '',
                    data: {
                        state: 'WAIT',
                        reason: 'COPY'
                    }
                });
                done();
            });
        });

        it('should return `NOT_STARTED` if `state: NOT_STARTED` got', (done) => {
            mockedAsker.mockResolvedValue({
                statusCode: 200,
                data: JSON.stringify({
                    state: {
                        state: 'NOT_STARTED'
                    }
                })
            });
            backend.urlinfo('test-url').then((result) => {
                expect(result).toEqual({
                    error: false,
                    statusCode: 200,
                    code: '',
                    data: {
                        state: 'NOT_STARTED'
                    }
                });
                done();
            });
        });

        it('should return `WAIT_URL` if `state: COPIED` got and native viewer enabled', (done) => {
            mockedAsker.mockResolvedValue({
                statusCode: 200,
                data: JSON.stringify({
                    state: {
                        state: 'COPIED',
                        'detected-content-type': 'application/epub+zip'
                    }
                })
            });
            backend.urlinfo('test-url').then((result) => {
                expect(result).toEqual({
                    error: false,
                    statusCode: 200,
                    code: '',
                    data: {
                        contentType: 'application/epub+zip',
                        state: 'WAIT_URL',
                        iframe: true
                    }
                });
                done();
            });
        });

        it('should return `FAIL` if got copy error', (done) => {
            mockedAsker.mockResolvedValue({
                statusCode: 200,
                data: JSON.stringify({
                    state: {
                        state: 'COPY_FAIL',
                        'copy-error-code': 'SOME_COPY_ERROR'
                    }
                })
            });
            backend.urlinfo('test-url').then((result) => {
                expect(result).toEqual({
                    error: false,
                    statusCode: 200,
                    code: '',
                    data: {
                        state: 'FAIL',
                        reason: 'COPY_FAIL',
                        errorCode: 'SOME_COPY_ERROR'
                    }
                });
                done();
            });
        });

        it('should return `WAIT / CONVERT` if `convert-state: CONVERTING` got', (done) => {
            mockedAsker.mockResolvedValue({
                statusCode: 200,
                data: JSON.stringify({
                    state: {
                        state: 'COPIED',
                        'convert-state': [{
                            target: 'HTML_WITH_IMAGES',
                            state: 'CONVERTING'
                        }]
                    }
                })
            });
            backend.urlinfo('test-url').then((result) => {
                expect(result).toEqual({
                    error: false,
                    statusCode: 200,
                    code: '',
                    data: {
                        state: 'WAIT',
                        reason: 'CONVERT'
                    }
                });
                done();
            });
        });

        it('should return `NOT_STARTED` if `convert-state: NOT_STARTED` got', (done) => {
            mockedAsker.mockResolvedValue({
                statusCode: 200,
                data: JSON.stringify({
                    state: {
                        state: 'COPIED',
                        'convert-state': [{
                            target: 'HTML_WITH_IMAGES',
                            state: 'NOT_STARTED'
                        }]
                    }
                })
            });
            backend.urlinfo('test-url').then((result) => {
                expect(result).toEqual({
                    error: false,
                    statusCode: 200,
                    code: '',
                    data: {
                        state: 'NOT_STARTED'
                    }
                });
                done();
            });
        });

        it('should return `ARCHIVE` if `convert-state: AVAILABLE, result-type: ARCHIVE_LISTING` got', (done) => {
            mockedAsker.mockResolvedValue({
                statusCode: 200,
                data: JSON.stringify({
                    state: {
                        state: 'COPIED',
                        'convert-state': [{
                            target: 'HTML_WITH_IMAGES',
                            state: 'AVAILABLE',
                            'result-type': 'ARCHIVE_LISTING'
                        }]
                    }
                })
            });
            backend.urlinfo('test-url').then((result) => {
                expect(result).toEqual({
                    error: false,
                    statusCode: 200,
                    code: '',
                    data: {
                        state: 'ARCHIVE'
                    }
                });
                done();
            });
        });

        it('should return `FAIL` if got convert-state not AVAILABLE', (done) => {
            mockedAsker.mockResolvedValue({
                statusCode: 200,
                data: JSON.stringify({
                    state: {
                        state: 'COPIED',
                        'convert-state': [{
                            target: 'HTML_WITH_IMAGES',
                            state: 'CONVERT_FAIL',
                            'convert-error-code': 'SOME_CONVERT_ERROR'
                        }]
                    }
                })
            });
            backend.urlinfo('test-url').then((result) => {
                expect(result).toEqual({
                    error: false,
                    statusCode: 200,
                    code: '',
                    data: {
                        state: 'FAIL',
                        reason: 'CONVERT_FAIL',
                        errorCode: 'SOME_CONVERT_ERROR'
                    }
                });
                done();
            });
        });

        it('should return `READY` if conversion was finished', (done) => {
            mockedAsker.mockResolvedValue({
                statusCode: 200,
                data: JSON.stringify({
                    state: {
                        state: 'COPIED',
                        'convert-state': [{
                            target: 'HTML_WITH_IMAGES',
                            state: 'AVAILABLE',
                            pages: {
                                count: 1
                            }
                        }]
                    }
                })
            });
            backend.urlinfo('test-url').then((result) => {
                expect(result).toEqual({
                    error: false,
                    statusCode: 200,
                    code: '',
                    data: {
                        state: 'READY',
                        pages: [{
                            index: 1
                        }],
                        withoutSize: true,
                        allPagesAreAlbum: false
                    }
                });
                done();
            });
        });
    });
});
