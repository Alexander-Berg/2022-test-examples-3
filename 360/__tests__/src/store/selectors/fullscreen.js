jest.mock('../../../../src/lib/helpers/fullscreen');

import { FULLSCREEN_TYPES, canOpenFullscreen, getFullscreenType, showOnlyOnePage } from 'store/selectors/fullscreen';

describe('store/selectors/fullscreen', () => {
    beforeEach(() => {
        global.ALLOW_FULLSCREEN = true;
    });

    afterEach(() => {
        global.ALLOW_FULLSCREEN = false;
    });

    describe('canOpenFullscreen', () => {
        it('should return false on server', () => {
            global.ALLOW_FULLSCREEN = false;
            expect(canOpenFullscreen({
                cfg: {
                    ua: { isMobile: false },
                    isMounted: true
                },
                doc: { iframe: false }
            })).toEqual(false);
        });
        it('should return true if not mobile, not iframe and browser allows fullscreen-api', () => {
            expect(canOpenFullscreen({
                cfg: {
                    ua: { isMobile: false },
                    isMounted: true
                },
                doc: { iframe: false }
            })).toEqual(true);
        });
        it('should return false if mobile', () => {
            expect(canOpenFullscreen({
                cfg: {
                    ua: { isMobile: true },
                    isMounted: true
                },
                doc: { iframe: false }
            })).toEqual(false);
        });
        it('should return false if iframe', () => {
            expect(canOpenFullscreen({
                cfg: {
                    ua: { isMobile: false },
                    isMounted: true
                },
                doc: { iframe: true }
            })).toEqual(false);
        });
        it('should return false if embed-DV', () => {
            expect(canOpenFullscreen({
                cfg: {
                    embed: true,
                    ua: {},
                    isMounted: true
                },
                doc: {}
            })).toEqual(false);
        });
    });

    describe('getFullscreenType', () => {
        it('should return null on server', () => {
            global.ALLOW_FULLSCREEN = false;
            expect(getFullscreenType({
                cfg: {
                    ua: {},
                    isMounted: true
                },
                doc: {}
            })).toEqual(null);
        });

        it('should return null on mobile', () => {
            expect(getFullscreenType({
                cfg: {
                    ua: { isMobile: true },
                    isMounted: true
                },
                doc: {}
            })).toEqual(null);
        });

        it('should return FULLSCREEN_TYPES.DOCUMENT for document without size', () => {
            expect(getFullscreenType({
                cfg: {
                    ua: {},
                    isMounted: true
                },
                doc: { withoutSize: true }
            })).toEqual(FULLSCREEN_TYPES.DOCUMENT);
        });

        it('should return FULLSCREEN_TYPES.DOCUMENT for excel', () => {
            expect(getFullscreenType({
                cfg: {
                    ua: {},
                    isMounted: true
                },
                doc: { contentFamily: 'spreadsheet' }
            })).toEqual(FULLSCREEN_TYPES.DOCUMENT);
        });

        it('should return FULLSCREEN_TYPES.PRESENTATION for presentation', () => {
            expect(getFullscreenType({
                cfg: {
                    ua: {},
                    isMounted: true
                },
                doc: { contentFamily: 'presentation' }
            })).toEqual(FULLSCREEN_TYPES.PRESENTATION);
        });

        it('should return null for embed presentation', () => {
            expect(getFullscreenType({
                cfg: {
                    embed: true,
                    ua: {},
                    isMounted: true
                },
                doc: {
                    contentFamily: 'presentation'
                }
            })).toEqual(null);
        });

        it('should return FULLSCREEN_TYPES.PRESENTATION for album PDF', () => {
            expect(getFullscreenType({
                cfg: {
                    ua: {},
                    isMounted: true
                },
                doc: { contentFamily: 'pdf', allPagesAreAlbum: true }
            })).toEqual(FULLSCREEN_TYPES.PRESENTATION);
        });

        it('should return FULLSCREEN_TYPES.DOCUMENT for non-album PDF', () => {
            expect(getFullscreenType({
                cfg: {
                    ua: {},
                    isMounted: true
                },
                doc: { contentFamily: 'pdf' }
            })).toEqual(FULLSCREEN_TYPES.DOCUMENT);
        });
    });

    describe('showOnlyOnePage', () => {
        it('should return true for presentation', () => {
            expect(showOnlyOnePage({
                cfg: {
                    ua: {}
                },
                doc: { contentFamily: 'presentation' }
            })).toEqual(true);
        });

        it('should return false for embed-DV', () => {
            expect(showOnlyOnePage({
                cfg: {
                    embed: true,
                    ua: {}
                },
                doc: { contentFamily: 'presentation' }
            })).toEqual(false);
        });

        it('should return true for album pdf', () => {
            expect(showOnlyOnePage({
                cfg: {
                    ua: {}
                },
                doc: { contentFamily: 'pdf', allPagesAreAlbum: true }
            })).toEqual(true);
        });

        it('should return true for regular document', () => {
            expect(showOnlyOnePage({
                cfg: {
                    ua: {}
                },
                doc: { contentFamily: 'text' }
            })).toEqual(false);
        });

        it('should return true for presentation if browser does not support fullscreen', () => {
            global.ALLOW_FULLSCREEN = false;
            expect(showOnlyOnePage({
                cfg: {
                    ua: {}
                },
                doc: { contentFamily: 'presentation' }
            })).toEqual(true);
        });
    });
});
