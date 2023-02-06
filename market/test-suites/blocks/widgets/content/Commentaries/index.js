import {makeSuite, mergeSuites} from 'ginny';

import commentsFirstLevelSuites from './firstLevel';
import commentsSecondLevelSuites from './secondMiddleLevel';
import commentsSecondLevelCommonSuites from './secondLevel';
import commentsThirdLevelSuites from './thirdLevel';

export default makeSuite('Комментарии с тремя уровнями.', {
    story: mergeSuites(
        makeSuite('1ый уровень', {
            story: commentsFirstLevelSuites,
        }),
        makeSuite('2ой уровень', {
            story: mergeSuites(
                commentsSecondLevelCommonSuites,
                commentsSecondLevelSuites
            ),
        }),
        makeSuite('3ий уровень', {
            story: mergeSuites(commentsThirdLevelSuites),
        })
    ),
    params: {
        pageTemplate: 'Шаблон урла страницы',
        pageParams: 'Параметры страницы - объект',
        entityId: 'Ид сущности, на которую оставлен комментарий',
    },
});
