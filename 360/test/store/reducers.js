"use strict";

const _ = require('lodash');
const assert = require('assert');
const expect = require('expect.js');
const freeze = require('deep-freeze');


const reducers = require('store/reducers');
const actions = require('store/actions');
const constants = require('helpers/constants');

describe('reducers', () => {
    before(() => {
        for (let key in reducers) {
            const reducer = reducers[key];
            reducers[key] = (store, action) => freeze(reducer(freeze(store), action));
        };
    });

    describe('user', () => {
        beforeEach(function() {
            this.state = {auth: false};
        });

        describe('update user', function() {
            beforeEach(function() {
                this.newUser = {uid: 1111};
                this.newState = reducers.user(this.state, actions.updateUser(this.newUser));
            });

            it('should update user', function() {
                expect(this.newState).not.to.be.eql(this.state);
            });
            it('new user should be equal to payload user', function() {
                expect(this.newState).to.be.eql(this.newUser);

            })
        });

        describe('fail login', function() {

            describe('empty login', function() {
                beforeEach(function() {
                    const payload = {};
                    this.newState = reducers.user(this.state, actions.loginFail(payload));
                });

                it('should change state', function() {
                    expect(this.newState).not.to.be.eql(this.state);
                });
                it('auth should be false', function() {
                    expect(this.newState.auth).to.be(false);
                });
                it('should set correct status', function() {
                    expect(this.newState.status).to.be(actions.LOGIN_EMPTY);
                });

            });

            describe('empty password', function() {
                beforeEach(function() {
                    const payload = {
                        login: 'yo'
                    };
                    this.newState = reducers.user(this.state, actions.loginFail(payload));
                })
                it('should change state', function() {
                    expect(this.newState).not.to.be.eql(this.state);
                });
                it('auth should be false', function() {
                    expect(this.newState.auth).to.be(false);
                });
                it('should set correct status', function() {
                    expect(this.newState.status).to.be(actions.PASSWORD_EMPTY);
                });
            });
        });

    });

    describe('resources', () => {
        const state = {
            '/disk/': {
                children: ['disk/test']
            },
            'disk/test': {}
        };

        it('should update', () => {
            const newState = reducers.resources(state, actions.updateResources([
                {
                    ctime: 1333569600,
                    mtime: 1333569600,
                    path: "/disk",
                    utime: 0,
                    type: "dir",
                    id: "/disk/",
                    name: "disk"
                }, {
                    ctime: 1454591628,
                    mtime: 1454591628,
                    path: "/disk/test2",
                    utime: 1454591628,
                    type: "dir",
                    id: "/disk/test2/",
                    name: "test2"
                }
            ]));
            expect(newState).not.to.be.eql(state);
        });

        describe('create folder', () => {
            const parent = '/disk/';
            const name = 'test2';
            const path = `${parent}${name}/`

            beforeEach(function() {
                this.newState = reducers.resources(state, actions.mkdir(parent, name));
            });

            it('should change state', function() {
                expect(this.newState).not.to.be.eql(state)
            });
            it('should add new subfolder', function() {
                expect(this.newState[parent].children).to.have.length(2)
            });
            it('should apply correct path of new subfolder', function() {
                expect(this.newState[parent].children[0]).to.be(path)
            });
            it('should mark a new subfolder with fake flag', function(){
                expect(this.newState[path].fake).to.be(true)
            });

        });

        describe('unfake folder', () => {
            const parent = '/disk/';
            const name = 'test2';
            const path = `${parent}${name}/`

            beforeEach(function() {
                this.newState = reducers.resources(
                    // add fake resource
                    reducers.resources(state, actions.mkdir(parent, name)),
                    // unfake it
                    actions.unfakeResource(path)
                );
            });

            it('should change state', function() {
                expect(this.newState).not.to.be.eql(state)
            });
            it('should keep correct path of new subfolder', function() {
                expect(this.newState[parent].children[0]).to.be(path)
            });
            it('should keep subfolders', function() {
                expect(this.newState[parent].children).to.have.length(2)
            });
            it('should set fake flag to false', function() {
                expect(this.newState[path].fake).to.be(false)
            });

        });
    });

    describe('current', () => {
        const state = '/disk/';

        it('should change current', () => {
            const newCurrent = '/disk/new';
            const newState = reducers.current(state, actions.chdir(newCurrent));
            expect(newState).to.be.eql(newCurrent);
        });
    });

    describe('resource', () => {
        const reducer = reducers.resource;
        const STATE = constants.RESOURCE_STATE;
        const resId = '/disk/foo';
        const resName = 'foo';
        const dirname = 'disk';
        const url = 'http://disk.yandex.ru/disk|select/disk/foo';


        it('should default to saving mode', () => {
            expect(reducer().state).to.be.eql(STATE.SAVING);
        });

        describe('saving -> saved', () => {
            beforeEach(function() {
                this.newState = reducer(this.state, actions.resourceSaved({
                    id: resId,
                    name: resName,
                    dirname: dirname
                }));

                this.state = {
                    state: STATE.SAVING
                };
            });

            it('should change state to saved', function() {
                expect(this.newState.state).to.be.eql(STATE.SAVED);
            });

            it('should add {id, name, dirname, url, recentUrl} to state', function() {
                const newState = reducer(this.state, actions.resourceSaved({
                    id: resId,
                    name: resName,
                    dirname,
                    url,
                    recentUrl: 'https://mail.yandex.ru'
                }));

                expect(newState).to.be.eql({
                    state: STATE.SAVED,
                    id: resId,
                    name: resName,
                    dirname,
                    url,
                    recentUrl: 'https://mail.yandex.ru'
                });
            });
        });


        describe('saving -> error', () => {
            beforeEach(function() {
                this.state = {
                    state: STATE.SAVING
                };
            });

            it('should change state to error', function() {
                expect(reducer(this.state, actions.resourceError({
                    code: 0
                })).state).to.be.equal(STATE.ERROR);
            });

            it('should change state to no space error if status code is 59', function() {
                expect(reducer(this.state, actions.resourceError({
                    code: 59
                })).state).to.be.equal(STATE.ERROR_NO_SPACE);
            });

            it('should change state to no space error if status code is 85', function() {
                expect(reducer(this.state, actions.resourceError({
                    code: 85
                })).state).to.be.equal(STATE.ERROR_NO_SPACE);
            });
        });


        describe('saved -> moving', function() {
            beforeEach(function() {
                this.state = {
                    state: STATE.SAVED
                };
                this.dst = '/disk/foo/';
                this.newState = reducer(this.state, actions.resourceMove(this.dst));
                console.log(this.newState);
            });
            it('should change state to moving', function() {
                expect(this.newState.state).to.be.equal(STATE.MOVING);
            });

            it('should add {dst} to state', function() {
                expect(this.newState.dst).to.be.equal(this.dst);
            });
        });

        describe('moving -> moved', function() {
            beforeEach(function() {
                this.state = {
                    state: STATE.MOVING
                };
                this.dest = {
                    id: '/disk/foo/a',
                    name: 'a',
                    dirname: 'foo',
                    url
                }

                this.newState = reducer(this.state, actions.resourceMoved(this.dest));
            });
            it('should change state to moved', function() {
                expect(this.newState.state).to.be.equal(STATE.MOVED);
            });

            it('should update {id, name, dirname, url}', function() {
                expect(this.newState.id).to.be.equal(this.dest.id);
                expect(this.newState.name).to.be.equal(this.dest.name);
                expect(this.newState.dirname).to.be.equal(this.dest.dirname);
                expect(this.newState.url).to.be.equal(this.dest.url);
            });
        });

        describe('moving -> error', function() {
            beforeEach(function() {
                this.state = {
                    state: STATE.MOVING,
                    id: '/disk/Downloads/a',
                    name: 'a',
                    dirname: 'Downloads',
                    dst: '/disk/foo/'
                };
            });

            it('should change state to error', function() {
                expect(reducer(this.state, actions.resourceError({ code: 0 })).state).to.be.equal(STATE.ERROR);
            });

            it('should change state to resource exists', function() {
                expect(reducer(this.state, actions.resourceError({ code: 32 })).state).to.be.equal(STATE.EXISTS);
            });

            it('should not change fields other than status', function() {
                expect(
                    reducer(
                        this.state,
                        actions.resourceError({ code: 0 })
                    )
                ).to.be.eql(
                    _.extend(this.state, {
                        state: STATE.ERROR
                    })
                );
            });
        });
    });
});
