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

    describe('вопросы отсутствуют', () => {
        let container: HTMLElement;
        beforeAll(async () => {
            await makeContext();
            await kadavrLayer.setState('report', products);
            await kadavrLayer.setState('schema', {users: [user]});
            await kadavrLayer.setState('storage.modelQuestions', createProductQuestions(0));
            const {container: _container} = await apiaryLayer.mountWidget(widgetPath, widgetProps);
            container = _container;
        });

        it('отображаться заголовок', async () => {
            expect(screen.queryByText('Задайте вопрос')).toBeInTheDocument();
        });

        it('нет ни одного снипета вопроса', async () => {
            expect(container.querySelectorAll(PersonalCabinetCard.root)).toHaveLength(0);
        });
    });

    describe('меньше 10 вопросов', () => {
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

        it('кнопка "Показать ещё" не должна отображаться', async () => {
            expect(screen.queryByText('Показать ещё')).not.toBeInTheDocument();
        });
        it('отображается верное кол-во вопросов', async () => {
            expect(container.querySelectorAll(PersonalCabinetCard.root)).toHaveLength(questionsCount);
        });
    });

    describe('больше 10 вопросов', () => {
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

        it('кнопка "Показать ещё" должна отображаться', async () => {
            expect(screen.queryByText('Показать ещё')).toBeInTheDocument();
        });

        it('отображается 10 вопросов', () => {
            expect(container.querySelectorAll(PersonalCabinetCard.root)).toHaveLength(10);
        });

        it('есть возможность загрузить ещё вопросы', async () => {
            screen.queryByText('Показать ещё').click();

            await waitFor(() => {
                expect(screen.queryByText('Показать ещё')).not.toBeInTheDocument();
            });

            await waitFor(() => {
                expect(container.querySelectorAll(PersonalCabinetCard.root)).toHaveLength(questionsCount);
            });
        });
    });

    describe('сниппет вопроса', () => {
        const questionsCount = 9;

        describe.each([
            ['на товар', createProductQuestions(questionsCount), {
                headerLink: buildUrl('market:product', {
                    productId: String(productId),
                    slug: productSlug,
                }),
                questionLink: 'market:product-question',
            }],
            ['на категорию', createCategoryQuestions(questionsCount), {
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

            describe('информация о пользователе', () => {
                it('содержит аватарку пользователя', async () => {
                    expect(container.querySelector(ContentUserInfo.root)).toBeTruthy();
                });

                it('содержит имя пользователя', async () => {
                    expect(screen.queryAllByText(user.display_name.public_name)).toHaveLength(questionsCount);
                });

                it('содержит дату создания', async () => {
                    expect(screen.queryAllByText('Год назад')).toHaveLength(questionsCount);
                });

                it('содержит ссылку на товар', async () => {
                    expect(container.querySelector(PersonalCabinetProductHeadline.root)).toHaveAttribute('href', links.headerLink);
                });

                it('содержит картинку товара', async () => {
                    expect(container.querySelector(PersonalCabinetProductHeadline.image)).toBeTruthy();
                });
            });

            describe('футер', () => {
                it('содержит информацию о кол-ве ответов', async () => {
                    expect(container).toHaveTextContent('8 ответов');
                });

                it('содержит ссылку на ответы', async () => {
                    expect(container.querySelector(PersonalCabinetQAFooter.link)).toHaveAttribute('href', links.questionLink);
                });

                it('содержит кол-во лайков', async () => {
                    expect(container.querySelector(Votes.UpVote)).toHaveTextContent('7');
                });

                it('при клике меняется кол-во лайков', async () => {
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

            describe('содержимое сниппета вопроса', () => {
                it('содержит текст вопроса', async () => {
                    expect(container).toHaveTextContent(questionText);
                });

                it('содержит ссылку на ответы', async () => {
                    const textLink = screen.getAllByText(questionText);

                    expect(textLink[0]).toHaveAttribute('href', links.questionLink);
                });
            });

            describe('редактирование вопроса', () => {
                async function openDialog() {
                    const menu = container.querySelector(DropdownMenu.root);

                    fireEvent.click(queryByRole(menu, 'button'));

                    await waitFor(() => {
                        expect(queryByRole(menu, 'menu')).toBeVisible();
                    });

                    fireEvent.click(getByText(menu, 'Удалить'));

                    return screen.findByRole('dialog');
                }

                it('кнопка «Отменить» диалога удаления закрывает диалог, не удаляя сниппет', async () => {
                    const dialogContainer = await openDialog();

                    fireEvent.click(queryByText(dialogContainer, 'Отменить'));

                    await waitFor(() => {
                        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
                    });

                    expect(container.querySelectorAll(PersonalCabinetCard.root)).toHaveLength(questionsCount);
                });

                it('крестик диалога удаления закрывает диалог, не удаляя сниппет', async () => {
                    const dialogContainer = await openDialog();

                    fireEvent.click(getByLabelText(dialogContainer, 'Close'));

                    await waitFor(() => {
                        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
                    });

                    expect(container.querySelectorAll(PersonalCabinetCard.root)).toHaveLength(questionsCount);
                });

                it('кнопка «Удалить» диалога удаления закрывает диалог, и удаляет сниппет', async () => {
                    const dialogContainer = await openDialog();

                    fireEvent.click(getByText(dialogContainer, 'Удалить вопрос'));

                    await waitFor(() => {
                        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
                    });

                    expect(container.querySelectorAll(PersonalCabinetCard.root)).toHaveLength(questionsCount - 1);
                });
            });
        });
    });
});
