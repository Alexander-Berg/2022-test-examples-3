const pageObject = require('bem-page-object');
const Entity = pageObject.Entity;
const blocks = require('../blocks/common');

const ReactEntity = require('../Entity').ReactEntity;

const { methods } = require('../react-blocks/SForm');

blocks.mCalendar = require('../blocks/m-calendar');

const sForm = require('../blocks/s-form');

const getVacancyFormField = methods.getSFieldOfName;

/*
 * блоки страницы создания вакансии
 */
blocks.vacancyFormView = new ReactEntity({ block: 'VacancyFormView' });
blocks.vacancyFormView.header = new ReactEntity({ block: 'VacancyFormView', elem: 'Heading' });
blocks.vacancyFormView.message = new ReactEntity({ block: 'VacancyFormView', elem: 'ApplicationInfoPanel' });
blocks.vacancyFormView.buttonMore = new ReactEntity({ block: 'VacancyFormView', elem: 'ButtonMore' });
blocks.vacancyFormView.vacancyType = new ReactEntity({ block: 'SField', elem: 'Input' }).mods({ type: 'type' });

/*
 * блоки страницы редактирования вакансии
 */

blocks.vacancyForm = new ReactEntity({ block: 'VacancyForm' });

// название
blocks.vacancyForm.title = getVacancyFormField('name');

// выбор типа вакансии (международная или нет)
blocks.vacancyForm.geographyInternational = getVacancyFormField('geography-international');

// непосредственный руководитель
blocks.vacancyForm.head = new ReactEntity({ block: 'SRow' }).mods({ name: 'head' });

// нанимающий менеджер
blocks.vacancyForm.hiringManager = getVacancyFormField('hiring-manager');

// уходящий сторудник
blocks.vacancyForm.insteadOf = getVacancyFormField('instead-of');

// цель открытия вакансии
blocks.vacancyForm.reason = getVacancyFormField('reason');

// сервис abc
blocks.vacancyForm.abc = getVacancyFormField('abc-services');

// основной продукт
blocks.vacancyForm.valueStream = getVacancyFormField('value-stream');

// собеседующие
blocks.vacancyForm.interviewers = getVacancyFormField('interviewers');

// наблюдатели
blocks.vacancyForm.observers = getVacancyFormField('observers');

// навыки
blocks.vacancyForm.skills = getVacancyFormField('skills');

// подразделение
blocks.vacancyForm.department = getVacancyFormField('department');

// основной рекрутер
blocks.vacancyForm.mainRecruiter = getVacancyFormField('main-recruiter');

// рекрутеры
blocks.vacancyForm.recruiters = getVacancyFormField('recruiters');

// ответственные
blocks.vacancyForm.responsibles = getVacancyFormField('responsibles');

// проф направление
blocks.vacancyForm.profType = getVacancyFormField('professional-sphere');

// профессия
blocks.vacancyForm.profession = getVacancyFormField('profession');

// проф. уровень (до)
blocks.vacancyForm.proLevelMax = new ReactEntity({
    block: 'SField',
    elem: 'Element',
}).mods({ type: 'max' });

// локация для поиска и найма
blocks.vacancyForm.locations = new ReactEntity({ block: 'VacancyForm', elem: 'LocationField' });

// режим работы
blocks.vacancyForm.workMode = getVacancyFormField('work-mode');

// система оплаты
blocks.vacancyForm.wageSystem = getVacancyFormField('wage-system');

// желаемая дата закрытия
blocks.vacancyForm.deadline = getVacancyFormField('deadline');

// максимальный оклад
blocks.vacancyForm.maxSalary = getVacancyFormField('max-salary');

// причина замены
blocks.vacancyForm.replacementReason = getVacancyFormField('replacement-reason');

// дата увольнения или перевода
blocks.vacancyForm.quitDate = getVacancyFormField('quit-date');

// тип трудового договора
blocks.vacancyForm.contractType = getVacancyFormField('contract-type');

// скрытая вакансия
blocks.vacancyForm.isHidden = getVacancyFormField('is-hidden');

// заголовок объявления
blocks.vacancyForm.publicationTitle = getVacancyFormField('publication-title');

// описание объявления
blocks.vacancyForm.publicationContent = getVacancyFormField('publication-content');

// опубликовать объявление
blocks.vacancyForm.isPublished = getVacancyFormField('is-published');

// сабмит
blocks.vacancyForm.submit = new ReactEntity({
    block: 'VacancyForm',
    elem: 'Button',
}).mods({ type: 'submit' });

blocks.vacancyForm.reset = new ReactEntity({
    block: 'VacancyForm',
    elem: 'Button',
}).mods({ type: 'reset' });

// блок сообщения об ошибке создании вакансии
blocks.vacancyForm.errorMessage = new ReactEntity({ block: 'SValidationMessage' }).mods({ type: 'error' });

/*
 * блоки страницы редактирования вакансии
 */
blocks.fPageVacancy = new Entity({ block: 'f-page-vacancy' });
blocks.fPageVacancy.actions = new Entity({ block: 'f-page-vacancy', elem: 'actions' });
blocks.fPageVacancy.actions.action = new Entity({ block: 'f-page-vacancy', elem: 'action' });
blocks.fPageVacancy.actions.actionActionUpdate = blocks.fPageVacancy.actions.action.copy().mods({ action: 'update' });
blocks.fPageVacancy.actions.menuSwitcher = new Entity({ block: 'f-menu' }).mods({ switcher: 'button2' });
blocks.fPageVacancy.header = new Entity({ block: 'f-page-vacancy', elem: 'header' });

// кнопка перевода в статус в 'работе'
blocks.vacancyApprove = new Entity({ block: 'f-vacancy-workflow' }).mods({ action: 'approve' });

// форма перевода вакнсии в другой статус
blocks.inProgressForm = new Entity({ block: 'f-vacancy-workflow', elem: 'form' }).mix(sForm);

// базовый блок филда формы
const fvwField = new Entity({ block: 'f-vacancy-workflow', elem: 'field' });

// основной рекрутер на форме
blocks.inProgressForm.main_recruiter = fvwField.mods({ type: 'main-recruiter' });
blocks.inProgressForm.main_recruiter.input = new Entity({ block: 'input', elem: 'control' });
blocks.mainRecruitersSuggest = blocks.bAutocompletePopup.copy();
blocks.mainRecruitersSuggest.items = new Entity({ block: 'input', elem: 'popup-items' });
blocks.mainRecruitersSuggest.midori = blocks.bAutocompletePopupItem.nthType(1);
blocks.mainRecruitersSuggest.marat = blocks.bAutocompletePopupItem.nthType(1);

// номер бюджетной позиции на форме
blocks.inProgressForm.bpId = fvwField.mods({ type: 'budget-position-id' });
blocks.inProgressForm.bpId.input = new Entity({ block: 'input', elem: 'control' });

// инфо по вакансии
blocks.vacancyInfo = new Entity({ block: 'f-page-vacancy', elem: 'pane' }).mods({ type: 'info' });

// рекрутер в инфо
blocks.vacancyInfo.recruiters = new Entity({ block: 'f-vacancy-field' }).mods({ type: 'recruiters' });
blocks.vacancyInfo.recruiters.input = new Entity({ block: 'input', elem: 'control' });
blocks.vacancyInfo.recruiters.username = new Entity({ block: 'staff-user', elem: 'name' });
blocks.recruitersSuggest = blocks.bAutocompletePopup.copy();
blocks.recruitersSuggest.items = new Entity({ block: 'input', elem: 'popup-items' });
blocks.recruitersSuggest.marat = blocks.bAutocompletePopupItem.nthType(1);

// главный рекрутер
blocks.vacancyInfo.mainRecruiter = new Entity({ block: 'f-vacancy-field' }).mods({ type: 'main-recruiter' });
blocks.vacancyInfo.mainRecruiter.username = new Entity({ block: 'staff-user', elem: 'name' });

// номер бюджетной позиции в инфо
blocks.vacancyInfo.bpId = new Entity({ block: 'f-vacancy-field' }).mods({ type: 'budget-position-id' });

module.exports = pageObject.create(blocks);
