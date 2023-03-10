// @flow
// flowlint untyped-import:off
import {screen, waitFor, getByText, queryByText, queryByRole, getByLabelText, fireEvent} from '@testing-library/dom';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {mockLocation} from '@self/root/src/helpers/testament/mock';
import ContentUserInfo from '@self/root/src/components/ContentUserInfo/__pageObject';
import * as voteQuestionActions from '@self/platform/actions/questionsAnswers/question';
import PersonalCabinetCard from '@self/platform/components/PersonalCabinetCard/__pageObject';
import PersonalCabinetProductHeadline from '@self/platform/components/PersonalCabinetProductHeadline/__pageObject';
import PersonalCabinetQAFooter from '@self/platform/components/PersonalCabinetQAFooter/__pageObject';
import Votes from '@self/platform/spec/page-objects/components/Votes';
import DropdownMenu from '@self/platform/spec/page-objects/components/DropdownMenu';
import {buildUrl} from '@self/root/src/utils/router';

import type {Options} from '../index';
import {user, userId} from './__mocks__/user';
import {createProductQuestions, createCategoryQuestions, questionText, category} from './__mocks__/questions';
import {products, productId, slug as productSlug} from './__mocks__/products';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

const widgetPath = require.resolve('@self/platform/widgets/content/UserQuestions');

beforeAll(async () => {
    mockLocation();

    mirror = await makeMirrorDesktop({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {asLibrary: true},
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');

    await jestLayer.doMock(
        require.resolve('@self/project/src/utils/router'),
        () => ({buildURL: pageId => pageId, buildUrl: pageId => pageId})
    );
});

async function makeContext() {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};
    return mandrelLayer.initContext({user, request: {cookie}});
}

afterAll(() => {
    mirror.destroy();
});

describe('UserQuestions', () => {
    const widgetProps: Options = {userUid: userId, isPublic: false};

    describe('?????????????? ??????????????????????', () => {
        let container: HTMLElement;
        beforeAll(async () => {
            await makeContext();
            await kadavrLayer.setState('report', products);
            await kadavrLayer.setState('schema', {users: [user]});
            await kadavrLayer.setState('storage.modelQuestions', createProductQuestions(0));
            const {container: _container} = await apiaryLayer.mountWidget(widgetPath, widgetProps);
            container = _container;
        });

        it('???????????????????????? ??????????????????', async () => {
            expect(screen.queryByText('?????????????? ????????????')).toBeInTheDocument();
        });

        it('?????? ???? ???????????? ?????????????? ??????????????', async () => {
            expect(container.querySelectorAll(PersonalCabinetCard.root)).toHaveLength(0);
        });
    });

    describe('???????????? 10 ????????????????', () => {
        let container: HTMLElement;
        const questionsCount = 9;
        beforeAll(async () => {
            await makeContext();
            await kadavrLayer.setState('report', products);
            await kadavrLayer.setState('schema', {users: [user]});
            await kadavrLayer.setState('storage.modelQuestions', createProductQuestions(questionsCount));
            const {container: _container} = await apiaryLayer.mountWidget(widgetPath, widgetProps);
            container = _container;
        });

        it('???????????? "???????????????? ??????" ???? ???????????? ????????????????????????', async () => {
            expect(screen.queryByText('???????????????? ??????')).not.toBeInTheDocument();
        });
        it('???????????????????????? ???????????? ??????-???? ????????????????', async () => {
            expect(container.querySelectorAll(PersonalCabinetCard.root)).toHaveLength(questionsCount);
        });
    });

    describe('???????????? 10 ????????????????', () => {
        let container: HTMLElement;
        const questionsCount = 15;
        beforeAll(async () => {
            await makeContext();
            await kadavrLayer.setState('report', products);
            await kadavrLayer.setState('schema', {users: [user]});
            await kadavrLayer.setState('storage.modelQuestions', createProductQuestions(questionsCount));
            const {container: _container} = await apiaryLayer.mountWidget(widgetPath, widgetProps);
            container = _container;
        });

        it('???????????? "???????????????? ??????" ???????????? ????????????????????????', async () => {
            expect(screen.queryByText('???????????????? ??????')).toBeInTheDocument();
        });

        it('???????????????????????? 10 ????????????????', () => {
            expect(container.querySelectorAll(PersonalCabinetCard.root)).toHaveLength(10);
        });

        it('???????? ?????????????????????? ?????????????????? ?????? ??????????????', async () => {
            screen.queryByText('???????????????? ??????').click();

            await waitFor(() => {
                expect(screen.queryByText('???????????????? ??????')).not.toBeInTheDocument();
            });

            await waitFor(() => {
                expect(container.querySelectorAll(PersonalCabinetCard.root)).toHaveLength(questionsCount);
            });
        });
    });

    describe('?????????????? ??????????????', () => {
        const questionsCount = 9;

        describe.each([
            ['???? ??????????', createProductQuestions(questionsCount), {
                headerLink: buildUrl('market:product', {
                    productId: String(productId),
                    slug: productSlug,
                }),
                questionLink: 'market:product-question',
            }],
            ['???? ??????????????????', createCategoryQuestions(questionsCount), {
                headerLink: buildUrl('market:category-questions', {
                    hid: category.id,
                    slug: category.slug,
                }),
                questionLink: 'market:category-question',
            }],
        ])('%s', (_, questionsState, links) => {
            let container: HTMLElement;
            const addVoteQuestionSpy = jest.spyOn(voteQuestionActions, 'addVoteQuestion');

            beforeAll(async () => {
                await makeContext();
                await kadavrLayer.setState('report', products);
                await kadavrLayer.setState('schema', {users: [user]});
                await kadavrLayer.setState('storage.modelQuestions', questionsState);
                const {container: _container} = await apiaryLayer.mountWidget(widgetPath, widgetProps);
                container = _container;
            });

            describe('???????????????????? ?? ????????????????????????', () => {
                it('???????????????? ???????????????? ????????????????????????', async () => {
                    expect(container.querySelector(ContentUserInfo.root)).toBeTruthy();
                });

                it('???????????????? ?????? ????????????????????????', async () => {
                    expect(screen.queryAllByText(user.display_name.public_name)).toHaveLength(questionsCount);
                });

                it('???????????????? ???????? ????????????????', async () => {
                    expect(screen.queryAllByText('?????? ??????????')).toHaveLength(questionsCount);
                });

                it('???????????????? ???????????? ???? ??????????', async () => {
                    expect(container.querySelector(PersonalCabinetProductHeadline.root)).toHaveAttribute('href', links.headerLink);
                });

                it('???????????????? ???????????????? ????????????', async () => {
                    expect(container.querySelector(PersonalCabinetProductHeadline.image)).toBeTruthy();
                });
            });

            describe('??????????', () => {
                it('???????????????? ???????????????????? ?? ??????-???? ??????????????', async () => {
                    expect(container).toHaveTextContent('8 ??????????????');
                });

                it('???????????????? ???????????? ???? ????????????', async () => {
                    expect(container.querySelector(PersonalCabinetQAFooter.link)).toHaveAttribute('href', links.questionLink);
                });

                it('???????????????? ??????-???? ????????????', async () => {
                    expect(container.querySelector(Votes.UpVote)).toHaveTextContent('7');
                });

                it('?????? ?????????? ???????????????? ??????-???? ????????????', async () => {
                    fireEvent.click(container.querySelector(`${Votes.UpVote} button`));

                    await waitFor(() => {
                        expect(addVoteQuestionSpy).toHaveBeenCalledWith({questionId: 0});
                    });

                    await waitFor(() => {
                        expect(container.querySelector(`${Votes.UpVote} button`)).toHaveTextContent('8');
                    });

                    fireEvent.click(container.querySelector(`${Votes.UpVote} button`));

                    await waitFor(() => {
                        expect(container.querySelector(`${Votes.UpVote} button`)).toHaveTextContent('7');
                    });
                });
            });

            describe('???????????????????? ???????????????? ??????????????', () => {
                it('???????????????? ?????????? ??????????????', async () => {
                    expect(container).toHaveTextContent(questionText);
                });

                it('???????????????? ???????????? ???? ????????????', async () => {
                    const textLink = screen.getAllByText(questionText);

                    expect(textLink[0]).toHaveAttribute('href', links.questionLink);
                });
            });

            describe('???????????????????????????? ??????????????', () => {
                async function openDialog() {
                    const menu = container.querySelector(DropdownMenu.root);

                    fireEvent.click(queryByRole(menu, 'button'));

                    await waitFor(() => {
                        expect(queryByRole(menu, 'menu')).toBeVisible();
                    });

                    fireEvent.click(getByText(menu, '??????????????'));

                    return screen.findByRole('dialog');
                }

                it('???????????? ???????????????????? ?????????????? ???????????????? ?????????????????? ????????????, ???? ???????????? ??????????????', async () => {
                    const dialogContainer = await openDialog();

                    fireEvent.click(queryByText(dialogContainer, '????????????????'));

                    await waitFor(() => {
                        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
                    });

                    expect(container.querySelectorAll(PersonalCabinetCard.root)).toHaveLength(questionsCount);
                });

                it('?????????????? ?????????????? ???????????????? ?????????????????? ????????????, ???? ???????????? ??????????????', async () => {
                    const dialogContainer = await openDialog();

                    fireEvent.click(getByLabelText(dialogContainer, 'Close'));

                    await waitFor(() => {
                        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
                    });

                    expect(container.querySelectorAll(PersonalCabinetCard.root)).toHaveLength(questionsCount);
                });

                it('???????????? ?????????????????? ?????????????? ???????????????? ?????????????????? ????????????, ?? ?????????????? ??????????????', async () => {
                    const dialogContainer = await openDialog();

                    fireEvent.click(getByText(dialogContainer, '?????????????? ????????????'));

                    await waitFor(() => {
                        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
                    });

                    expect(container.querySelectorAll(PersonalCabinetCard.root)).toHaveLength(questionsCount - 1);
                });
            });
        });
    });
});
