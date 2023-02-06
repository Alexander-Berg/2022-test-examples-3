import {makeSuite, mergeSuites} from 'ginny';
import titleSuite from './__more-offers-title';
import moreButtonSuite from './__more-offers-button';
import snippets from './__snippets';

/**
 * Тест на блок n-search-similar.
 * Требования: в блоке "Похожие товары" должны присутствовать и модели, и оффера.
 * @param {PageObject.SearchSimilar} searchSimilar
 */
export default makeSuite('Похожие товары', {
    feature: 'Похожие товары',
    params: {
        goalsPageId: 'Префикс событий Метрики, соответствующий текущей странице (см. goalsPoints.js) – ' +
            'OFFER | SEARCH | ...',
        moreButtonText: 'Текст, ожидаемый в кнопке перехода на выдачу',
        moreButtonLinkPath: 'Path ссылки в кнопке перехода на выдачу',
        moreButtonLinkQuery: 'Параметры ссылки в в кнопке перехода на выдачу',
        titleText: 'Текст, ожидаемый в заголовке блока',
        titleLinkPath: 'Path ссылки в заголовке блока',
        titleLinkQuery: 'Параметры ссылки в заголовке блока',
        snippetsCount: 'Количество сниппетов в списке товаров',
        zonePrefix: 'Префикс зоны Метрики, в которой находится текущий блок',
    },
    story: mergeSuites(
        titleSuite,
        moreButtonSuite,
        snippets
    ),
});
