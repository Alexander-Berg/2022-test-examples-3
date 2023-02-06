import * as React from 'react';
import * as sinon from 'sinon';
import * as Chai from 'chai';
import { describe, it } from 'mocha';
import { StoryApi, ClientApiReturnFn } from '@storybook/addons';

import { ComponentStories } from '../src';
import { getPlatformClassList } from '../src/getters';
import { PlatformLevels, TLevel } from '../src/platforms';

const assert = Chai.assert as Chai.AssertStatic & sinon.SinonAssert;

sinon.assert.expose(assert, { prefix: '' });

class StubStory implements StoryApi {
    [k: string]: string | ClientApiReturnFn<unknown>;

    kind = '';
    add = sinon.stub().returnsThis();
    addLoader = sinon.stub().returnsThis();
    addDecorator = sinon.stub().returnsThis();
    addParameters = sinon.stub().returnsThis();
}

const ExampleComponent = () => null;
const ExampleComponent2 = () => null;
const ExampleDecorator = () => null;

describe('ComponentStories', () => {
    let storyRenderer: sinon.SinonStub;
    let superStoriesOf: sinon.SinonStub;

    let storyA: StubStory;
    let storyB: StubStory;

    beforeEach(() => {
        storyRenderer = sinon.stub().returns([]);
        superStoriesOf = sinon.stub();

        storyA = new StubStory();
        storyB = new StubStory();

        superStoriesOf.onFirstCall().returns(storyA);
        superStoriesOf.onSecondCall().returns(storyB);
    });

    afterEach(() => {
        superStoriesOf.reset();
    });

    describe('one platform', () => {
        let stories: ComponentStories;

        beforeEach(() => {
            stories = new ComponentStories(module, 'example', ExampleComponent, undefined, superStoriesOf);
        });

        it('should create one story', () => {
            assert.calledOnce(superStoriesOf);
        });

        it('should add decorator with desktop class', () => {
            assert.calledTwice(storyA.addDecorator);

            const [decorator] = storyA.addDecorator.getCall(1).args;
            const result = decorator(ExampleComponent) as React.ReactElement<{ className: string }>;
            const actual = result.props.className.split(' ');
            const expected = getPlatformClassList(PlatformLevels.desktop as TLevel[]);

            assert.sameMembers(actual, expected);
        });

        describe('methods', () => {
            describe('add', () => {
                it('should add story and return this', () => {
                    const result = stories.add('some-story', storyRenderer);

                    assert.calledOnce(storyA.add);
                    assert.strictEqual(result, stories);
                });

                it('should add story with actual name', () => {
                    stories.add('some-story', storyRenderer);

                    const [name] = storyA.add.getCall(0).args;

                    assert.strictEqual(name, 'some-story');
                });

                it('should add story with correct renderer', () => {
                    stories.add('some-story', storyRenderer);

                    const [, renderer] = storyA.add.getCall(0).args;

                    assert.notCalled(storyRenderer);

                    renderer();

                    assert.calledOnce(storyRenderer);

                    const [component] = storyRenderer.getCall(0).args;

                    assert.strictEqual(component, ExampleComponent);
                });
            });

            describe('addDecorator', () => {
                it('should add decorator and return this', () => {
                    assert.calledTwice(storyA.addDecorator); // two in constructor

                    const result = stories.addDecorator(ExampleDecorator);

                    assert.calledThrice(storyA.addDecorator); // 3rd call
                    assert.strictEqual(result, stories);
                });

                it('should add actual decorator', () => {
                    stories.addDecorator(ExampleDecorator);

                    const [decorator] = storyA.addDecorator.getCall(2).args;

                    assert.strictEqual(decorator, ExampleDecorator);
                });
            });
        });
    });

    describe('multiple platforms', () => {
        let stories: ComponentStories;

        beforeEach(() => {
            stories = new ComponentStories(module, 'example', {
                desktop: ExampleComponent,
                'touch-phone': ExampleComponent2,
            }, undefined, superStoriesOf);
        });

        it('should create two stories', () => {
            assert.calledTwice(superStoriesOf);
        });

        it('should add desktop-decorator to 1st story', () => {
            assert.calledTwice(storyA.addDecorator);

            const [decorator] = storyA.addDecorator.getCall(1).args;
            const result = decorator(ExampleComponent) as React.ReactElement<{ className: string }>;
            const actual = result.props.className.split(' ');
            const expected = getPlatformClassList(PlatformLevels.desktop as TLevel[]);

            assert.sameMembers(actual, expected);
        });

        it('should add phone-decorator to 2nd story', () => {
            assert.calledTwice(storyB.addDecorator);

            const [decorator] = storyB.addDecorator.getCall(1).args;
            const result = decorator(ExampleComponent) as React.ReactElement<{ className: string }>;
            const actual = result.props.className.split(' ');
            const expected = getPlatformClassList(PlatformLevels['touch-phone'] as TLevel[]);

            assert.sameMembers(actual, expected);
        });

        describe('methods', () => {
            describe('add', () => {
                it('should add story to all items and return this', () => {
                    const result = stories.add('some-story', storyRenderer);

                    assert.calledOnce(storyA.add);
                    assert.calledOnce(storyB.add);
                    assert.strictEqual(result, stories);
                });

                it('should add stories with actual names', () => {
                    stories.add('some-story', storyRenderer);

                    const [nameA] = storyA.add.getCall(0).args;
                    const [nameB] = storyB.add.getCall(0).args;

                    assert.strictEqual(nameA, 'some-story');
                    assert.strictEqual(nameB, 'some-story');
                });

                it('should add stories with correct renderers', () => {
                    stories.add('some-story', storyRenderer);

                    const [, rendererA] = storyA.add.getCall(0).args;
                    const [, rendererB] = storyA.add.getCall(0).args;

                    assert.notCalled(storyRenderer);

                    rendererA();
                    rendererB();

                    assert.calledTwice(storyRenderer);

                    const [component1] = storyRenderer.getCall(0).args;
                    const [component2] = storyRenderer.getCall(1).args;

                    assert.strictEqual(component1, ExampleComponent);
                    assert.strictEqual(component2, ExampleComponent);
                });
            });

            describe('addDecorator', () => {
                it('should add decorator and return this', () => {
                    assert.calledTwice(storyA.addDecorator); // in constructor
                    assert.calledTwice(storyB.addDecorator); // in constructor

                    const result = stories.addDecorator(ExampleDecorator);

                    assert.calledThrice(storyA.addDecorator); // 3rd call
                    assert.calledThrice(storyB.addDecorator); // 3rd call
                    assert.strictEqual(result, stories);
                });

                it('should add actual decorator', () => {
                    stories.addDecorator(ExampleDecorator);

                    const [decoratorA] = storyA.addDecorator.getCall(2).args;
                    const [decoratorB] = storyB.addDecorator.getCall(2).args;

                    assert.strictEqual(decoratorA, ExampleDecorator);
                    assert.strictEqual(decoratorB, ExampleDecorator);
                });
            });
        });
    });
});
