import * as selectors from 'store/selectors';

const getStore = (store) => Object.assign({
    doc: {
        actions: {}
    },
    user: {
        features: {}
    }
}, store, {
    cfg: Object.assign({
        ua: {}
    }, store && store.cfg, {
        experiments: {
            flags: Object.assign({}, store && store.cfg && store.cfg.experiments && store.cfg.experiments.flags)
        }
    })
});

describe('store/selectors', () => {
    describe('hasAds', () => {
        it('has advertisement by default', () => {
            expect(selectors.hasAds(getStore())).toEqual(true);
        });

        it('no advertisement in embed-DV', () => {
            expect(selectors.hasAds(getStore({
                cfg: { embed: true }
            }))).toEqual(false);
        });

        it('no advertisement in corp', () => {
            expect(selectors.hasAds(getStore({
                cfg: { isCorp: true }
            }))).toEqual(false);
        });

        it('no advertisement for Disk.Pro user', () => {
            expect(selectors.hasAds(getStore({
                user: {
                    features: {
                        advertising: false
                    }
                }
            }))).toEqual(false);
        });

        it('has advertisement for iframe', () => {
            expect(selectors.hasAds(getStore({
                doc: {
                    iframe: true
                }
            }))).toEqual(true);
        });
    });

    describe('hasBottomAds', () => {
        it('no bottom advertisement by default', () => {
            expect(selectors.hasBottomAds(getStore())).toEqual(false);
        });

        it('has bottom advertisement if document READY', () => {
            expect(selectors.hasBottomAds(getStore({
                doc: {
                    state: 'READY'
                },
                cfg: {
                    isMounted: true
                }
            }))).toEqual(true);
        });

        it('has bottom advertisement if ARCHIVE', () => {
            expect(selectors.hasBottomAds(getStore({
                doc: {
                    state: 'ARCHIVE'
                },
                cfg: {
                    isMounted: true
                }
            }))).toEqual(true);
        });

        it('has bottom advertisement if document FAIL', () => {
            expect(selectors.hasBottomAds(getStore({
                doc: {
                    state: 'FAIL'
                },
                cfg: {
                    isMounted: true
                }
            }))).toEqual(true);
        });

        it('no bottom advertisement if document WAIT', () => {
            expect(selectors.hasBottomAds(getStore({
                doc: {
                    state: 'WAIT'
                }
            }))).toEqual(false);
        });

        it('no touch bottom advertisement by default', () => {
            expect(selectors.hasBottomAds(getStore({
                cfg: {
                    ua: {
                        isMobile: true
                    }
                }
            }))).toEqual(false);
        });

        it('has touch bottom advertisement', () => {
            expect(selectors.hasBottomAds(getStore({
                doc: {
                    state: 'READY'
                },
                cfg: {
                    isMounted: true,
                    ua: {
                        isMobile: true
                    }
                }
            }))).toEqual(true);
        });

        it('no bottom advertisement in embed-DV', () => {
            expect(selectors.hasBottomAds(getStore({
                doc: {
                    state: 'READY'
                },
                cfg: {
                    embed: true
                }
            }))).toEqual(false);
        });

        it('no bottom advertisement in corp', () => {
            expect(selectors.hasBottomAds(getStore({
                doc: {
                    state: 'READY'
                },
                cfg: {
                    isCorp: true
                }
            }))).toEqual(false);
        });

        it('no bottom advertisement for Disk.Pro user', () => {
            expect(selectors.hasBottomAds(getStore({
                doc: {
                    state: 'READY'
                },
                user: {
                    features: {
                        advertising: false
                    }
                }
            }))).toEqual(false);
        });

        it('no bottom advertisement for iframe', () => {
            expect(selectors.hasBottomAds(getStore({
                doc: {
                    state: 'READY',
                    iframe: true
                }
            }))).toEqual(false);
        });
    });

    describe('hasTopAds', () => {
        it('has top advertisement by default', () => {
            expect(selectors.hasTopAds(getStore())).toEqual(true);
        });

        it('no top advertisement in embed-DV', () => {
            expect(selectors.hasTopAds(getStore({
                cfg: { embed: true }
            }))).toEqual(false);
        });

        it('no top advertisement in corp', () => {
            expect(selectors.hasTopAds(getStore({
                cfg: { isCorp: true }
            }))).toEqual(false);
        });

        it('no top advertisement for Disk.Pro user', () => {
            expect(selectors.hasTopAds(getStore({
                user: {
                    features: {
                        advertising: false
                    }
                }
            }))).toEqual(false);
        });

        it('has top advertisement on mobile', () => {
            expect(selectors.hasTopAds(getStore({
                cfg: {
                    ua: {
                        isMobile: true
                    }
                }
            }))).toEqual(true);
        });

        it('no top advertisement when edit promo is shown', () => {
            expect(selectors.hasTopAds(getStore({
                doc: {
                    protocol: 'ya-browser:',
                    state: 'READY',
                    actions: {
                        edit: {
                            allow: true,
                            url: 'some-edit-url'
                        }
                    }
                }
            }))).toEqual(false);
        });
    });

    describe('hasEditPromo', () => {
        it('no edit-promo by default', () => {
            expect(selectors.hasEditPromo(getStore())).toEqual(false);
        });

        it('show edit-promo', () => {
            expect(selectors.hasEditPromo(getStore({
                doc: {
                    protocol: 'ya-browser:',
                    state: 'READY',
                    actions: {
                        edit: {
                            allow: true,
                            url: 'some-edit-url'
                        }
                    }
                }
            }))).toEqual(true);
        });

        it('no edit-promo for authorized', () => {
            expect(selectors.hasEditPromo(getStore({
                doc: {
                    protocol: 'ya-browser:',
                    state: 'READY',
                    actions: {
                        edit: {
                            allow: true,
                            url: 'some-edit-url'
                        }
                    }
                },
                user: {
                    auth: true
                }
            }))).toEqual(false);
        });

        it('no edit-promo for mobile', () => {
            expect(selectors.hasEditPromo(getStore({
                doc: {
                    protocol: 'ya-browser:',
                    state: 'READY',
                    actions: {
                        edit: {
                            allow: true,
                            url: 'some-edit-url'
                        }
                    }
                },
                cfg: {
                    ua: {
                        isMobile: true
                    }
                }
            }))).toEqual(false);
        });

        it('no edit-promo if disabled', () => {
            expect(selectors.hasEditPromo(getStore({
                doc: {
                    protocol: 'ya-browser:',
                    state: 'READY',
                    actions: {
                        edit: {
                            allow: true,
                            url: 'some-edit-url',
                            editPromoDisabled: true
                        }
                    }
                },
                cfg: {
                    ua: {
                        isMobile: true
                    }
                }
            }))).toEqual(false);
        });
    });
});
