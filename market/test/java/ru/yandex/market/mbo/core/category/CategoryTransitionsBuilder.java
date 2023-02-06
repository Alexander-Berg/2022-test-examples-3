package ru.yandex.market.mbo.core.category;

import ru.yandex.market.mbo.common.db.jooq.generated.category.enums.CategoryTransitionType;
import ru.yandex.market.mbo.common.db.jooq.generated.category.enums.EntityType;
import ru.yandex.market.mbo.common.db.jooq.generated.category.tables.pojos.CategoryTransitions;
import ru.yandex.market.mbo.common.db.jooq.generated.category.udt.pojos.RecipeFilterValue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author tokhmanva
 */
class CategoryTransitionsBuilder {
    private final List<Transition> transitions = new ArrayList<>();

    private CategoryTransitionsBuilder() {
    }

    static CategoryTransitionsBuilder create() {
        return new CategoryTransitionsBuilder();
    }

    Transition startTransition() {
        Transition transition = new Transition();
        transitions.add(transition);
        return transition;
    }

    List<CategoryTransitions> build() {
        return transitions.stream().map(Transition::build).collect(Collectors.toList());
    }

    class Transition {
        private final CategoryTransitions transition;
        private final List<RecipeFilterValue> oldRecipeFilters = new ArrayList<>();
        private final List<RecipeFilterValue> newRecipeFilters = new ArrayList<>();

        private Transition() {
            transition = new CategoryTransitions();
        }

        Transition withId(Long id) {
            transition.setId(id);
            return this;
        }

        Transition withActionId(Long actionId) {
            transition.setActionId(actionId);
            return this;
        }

        Transition withDate(LocalDateTime ldt) {
            transition.setDate(ldt);
            return this;
        }

        Transition withType(CategoryTransitionType type) {
            transition.setType(type);
            return this;
        }

        Transition withEntityType(EntityType entityType) {
            transition.setEntityType(entityType);
            return this;
        }

        Transition withPrimaryTransition(boolean primary) {
            transition.setPrimaryTransition(primary);
            return this;
        }

        Transition withOldCategoryId(Long categoryId) {
            transition.setOldCategoryId(categoryId);
            return this;
        }

        Transition withNewCategoryId(Long categoryId) {
            transition.setNewCategoryId(categoryId);
            return this;
        }

        Transition withOldLandingId(Long landingId) {
            transition.setOldLandingId(landingId);
            return this;
        }

        Transition withNewLandingId(Long landingId) {
            transition.setNewLandingId(landingId);
            return this;
        }

        Transition withOldRecipeFilter(RecipeFilterValue filterValue) {
            oldRecipeFilters.add(filterValue);
            return this;
        }

        Transition withNewRecipeFilter(RecipeFilterValue filterValue) {
            newRecipeFilters.add(filterValue);
            return this;
        }

        CategoryTransitionsBuilder endTransition() {
            return CategoryTransitionsBuilder.this;
        }

        private CategoryTransitions build() {
            int size = oldRecipeFilters.size();
            if (size > 0) {
                transition.setOldRecipeFilters(oldRecipeFilters.toArray(new RecipeFilterValue[size]));
            }
            size = newRecipeFilters.size();
            if (size > 0) {
                transition.setNewRecipeFilters(newRecipeFilters.toArray(new RecipeFilterValue[size]));
            }
            return transition;
        }
    }
}
