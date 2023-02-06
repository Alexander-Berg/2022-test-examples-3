package ru.yandex.direct.core.testing.steps;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.retargeting.model.CryptaGoalScope;
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;

public class CryptaGoalsSteps {
    @Autowired
    protected TestCryptaSegmentRepository testCryptaSegmentRepository;

    /* Добавляет все социал-демо цели как в базе
     */
    public void addAllSocialDemoGoals() {

        List<Goal> goals = Arrays.asList(
                getGoal(2499000001L, 2499000021L, "Мужчины", "616", "0"),
                getGoal(2499000002L, 2499000021L, "Женщины", "616", "1"),
                getGoal(2499000003L, 2499000022L, "<18", "617", "0"),
                getGoal(2499000004L, 2499000022L, "18-24", "617", "1"),
                getGoal(2499000005L, 2499000022L, "25-34", "617", "2"),
                getGoal(2499000006L, 2499000022L, "35-44", "617", "3"),
                getGoal(2499000007L, 2499000022L, "45-54", "617", "4"),
                getGoal(2499000008L, 2499000022L, "55+", "617", "5"),
                getGoal(2499000009L, 2499000023L, "Низкий", "618", "0"),
                getGoal(2499000010L, 2499000023L, "Средний", "618", "1"),
                getGoal(2499000011L, 2499000023L, "Выше среднего", "618", "2"),
                getGoal(2499000012L, 2499000023L, "Высокий", "618", "3"),
                getGoal(2499000013L, 2499000023L, "Премиум", "618", "4"),
                getGoal(2499000021L, 0L, "Пол", "0", "0"),
                getGoal(2499000022L, 0L, "Возраст", "0", "0"),
                getGoal(2499000023L, 0L, "Доход", "0", "0")
        );

        testCryptaSegmentRepository.addAll(goals);
    }


    public static final Long CONTENT_GENRE_GOAL_ID = 4294970296L;
    public static final Long CONTENT_CATEGORY_GOAL_ID = 4294968305L;

    /**
     * Добавляет по категории с типами "content_category" и "content_genre"
     */
    public void addCryptaContentCategoriesGoals() {
        var contentGenreGoal = (Goal) getGoal(CONTENT_GENRE_GOAL_ID, 0, "Советское кино", "983", null)
                .withType(GoalType.CONTENT_GENRE);
        var contentCategoryGoal = (Goal) getGoal(CONTENT_CATEGORY_GOAL_ID, 0, "Домашние животные", "982", null)
                .withType(GoalType.CONTENT_CATEGORY);
        testCryptaSegmentRepository.addAll(List.of(contentGenreGoal, contentCategoryGoal));
    }

    private Goal getGoal(long id, long parentId, String name, String keyword,
                         String keywordValue) {
        return (Goal) new Goal()
                .withId(id)
                .withParentId(parentId)
                .withName(name)
                .withInterestType(CryptaInterestType.long_term)
                .withKeyword(keyword)
                .withKeywordValue(keywordValue);
    }

    public void addGoals(Goal goal) {
        testCryptaSegmentRepository.addAll(List.of(goal));
    }

    public void addGoals(List<Goal> goals, Set<CryptaGoalScope> scope) {
        testCryptaSegmentRepository.addAll(goals, scope);
    }

    public void reset() {
        testCryptaSegmentRepository.clean();
    }
}
