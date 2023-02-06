import {mergeSuites, makeSuite} from 'ginny';
import commentsFirstLevelSuites from './firstLevel';
import commentsSecondLevelSuites from './secondLastLevel';
import commentsSecondLevelCommonSuites from './secondLevel';

export default makeSuite('Комментарии с двумя уровнями.', {
    story: mergeSuites(
        makeSuite('1ый уровень', {
            story: commentsFirstLevelSuites,
        }),
        makeSuite('2ой уровень', {
            story: mergeSuites(
                commentsSecondLevelCommonSuites,
                commentsSecondLevelSuites
            ),
        })
    ),
});
