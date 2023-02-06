import fetchMock from 'fetch-mock-jest';

import { IApplication } from '~/interfaces/IApplication';
import { IDataRequestResults } from '~/interfaces/IDataRequestResults';

import { DashboardColumnFragment } from './fragments/DashboardColumnFragment';
import {
    getApplicationStub,
    getCandidateStub,
    getFormMock,
    getMockSFormData,
    getRequestStub,
} from './utils/mocks';
import { renderDashboardPage } from './utils/renderPage';

const getVacancyUrl = (vacancyId: number, stage: string) => {
    return `/api/applications/dashboard/?_experiment=FEMIDA-5278&page=1&page_size=10&stage=${stage}&vacancies=${vacancyId}`;
};

interface RequestMocks {
    draft?: IDataRequestResults<IApplication>,
    'new'?: IDataRequestResults<IApplication>,
    team_is_interested?: IDataRequestResults<IApplication>,
    invited_to_preliminary_interview?: IDataRequestResults<IApplication>,
    invited_to_onsite_interview?: IDataRequestResults<IApplication>,
    invited_to_final_interview?: IDataRequestResults<IApplication>,
    offer_agreement?: IDataRequestResults<IApplication>,
}

const mockVacanciesRequests = (vacancyId: number, mocks?: RequestMocks) => {
    fetchMock.get(getVacancyUrl(vacancyId, 'draft'), mocks?.draft ?? getRequestStub());
    fetchMock.get(getVacancyUrl(vacancyId, 'new'), mocks?.new ?? getRequestStub());
    fetchMock.get(getVacancyUrl(vacancyId, 'team_is_interested'), mocks?.team_is_interested ?? getRequestStub());
    fetchMock.get(getVacancyUrl(vacancyId, 'invited_to_preliminary_interview'), mocks?.invited_to_preliminary_interview ?? getRequestStub());
    fetchMock.get(getVacancyUrl(vacancyId, 'invited_to_onsite_interview'), mocks?.invited_to_onsite_interview ?? getRequestStub());
    fetchMock.get(getVacancyUrl(vacancyId, 'invited_to_final_interview'), mocks?.invited_to_final_interview ?? getRequestStub());
    fetchMock.get(getVacancyUrl(vacancyId, 'offer_agreement'), mocks?.offer_agreement ?? getRequestStub());
};

describe('Дашборд претендентов', function() {
    describe('Общий раздел', function() {
        it('При заходе на доску претендентов url изменяется на активные вакансии', async function() {
            fetchMock.get('/api/applications/dashboard/_filter_form/', getFormMock());

            const { history } = await renderDashboardPage({ initialEntries: ['/applications/dashboard/'] });

            expect(history.location.pathname).toBe('/applications/dashboard/active');
        });
    });
    describe('Таблица', function() {
        it('Проверка состава колонок для активных вакансий', async function() {
            fetchMock.get('/api/applications/dashboard/_filter_form/', getFormMock());

            const { content } = await renderDashboardPage({ initialEntries: ['/applications/dashboard/active/'] });
            const columnTitles = content.columns.map((column: DashboardColumnFragment) => column.title);

            expect(columnTitles.length).toBe(7);
            expect(columnTitles).toContain('Черновики (0)');
            expect(columnTitles).toContain('Новые (0)');
            expect(columnTitles).toContain('Интересные (0)');
            expect(columnTitles).toContain('Наши скайпы (0)');
            expect(columnTitles).toContain('Наши очки (0)');
            expect(columnTitles).toContain('Наши финалы (0)');
            expect(columnTitles).toContain('Наши офферы (0)');
        });

        it('Отображается карточка кандидата', async function() {
            const vacancyId = 55282;

            fetchMock.get(`/api/applications/dashboard/_filter_form/?vacancies=${vacancyId}`, getFormMock({ data: getMockSFormData([{
                id: vacancyId,
                data: {
                    status: 'in_progress',
                    name: 'Буткемп/разработчик интерфейсов',
                    caption: 'Буткемп/разработчик интерфейсов',
                },
            }]) }));

            mockVacanciesRequests(vacancyId, { draft: getRequestStub({ results: [
                getApplicationStub({ candidate: getCandidateStub({
                    first_name: 'Алексей',
                    last_name: 'Леонов',
                }) }),
            ] }) });

            const { content } = await renderDashboardPage({ initialEntries: [`/applications/dashboard/active?vacancies=${vacancyId}`] });

            await content.waitTableColumnLoaded('drafts');
            const candidateName = content.column('drafts').cards[0].name;

            expect(candidateName).toBe('Алексей Леонов');
        });
    });
});
