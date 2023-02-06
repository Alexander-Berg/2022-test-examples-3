package ru.yandex.market.mboc.common.services.jooq_test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;

import ru.yandex.market.mbo.jooq.repo.AscDesc;
import ru.yandex.market.mbo.jooq.repo.HasField;
import ru.yandex.market.mbo.jooq.repo.JooqRepository;
import ru.yandex.market.mbo.jooq.repo.JooqUtils;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.JooqTestEntity;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.records.JooqTestEntityRecord;

import static ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.Tables.JOOQ_TEST_ENTITY;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 25.06.2019
 */
public class JooqTestRepository extends JooqRepository<JooqTestEntity, JooqTestEntityRecord,
    Long, JooqTestRepository.Filter, JooqTestRepository.SortBy> {

    public JooqTestRepository(DSLContext create) {
        super(create, JOOQ_TEST_ENTITY, JOOQ_TEST_ENTITY.ID, JooqTestEntity.class);
    }

    @Override
    protected Condition createCondition(@Nonnull Filter filter) {
        Condition condition = JooqUtils.intersectRangeCondition(
            JOOQ_TEST_ENTITY.FROM_DATE, filter.getFromDate(),
            JOOQ_TEST_ENTITY.TO_DATE, filter.getToDate()
        );
        if (filter.hasDescription) {
            condition = condition.and(JOOQ_TEST_ENTITY.DESCRIPTION.isNotNull());
        }
        if (filter.ids != null) {
            condition = condition.and(JOOQ_TEST_ENTITY.ID.in(filter.ids));
        }
        return condition;
    }

    @Override
    protected List<Long> generateIds(int count) {
        return super.generateIds(count);
    }

    @Getter
    @RequiredArgsConstructor
    public enum SortBy implements AscDesc<SortBy>, HasField {
        ID(JOOQ_TEST_ENTITY.ID),
        DESCRIPTION(JOOQ_TEST_ENTITY.DESCRIPTION);
        private final Field<?> field;
    }

    public static class Filter {
        private boolean hasDescription = false;
        private Collection<Long> ids;
        private LocalDate fromDate;
        private LocalDate toDate;

        public Filter() {
        }

        public static Filter withIds(Collection<Long> ids) {
            Filter filter = new Filter();
            filter.ids = new ArrayList<>(ids);
            return filter;
        }

        public static Filter hasDescription() {
            Filter filter = new Filter();
            filter.hasDescription = true;
            return filter;
        }

        public static Filter all() {
            return new Filter();
        }

        public LocalDate getFromDate() {
            return fromDate;
        }

        public Filter setFromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public LocalDate getToDate() {
            return toDate;
        }

        public Filter setToDate(LocalDate toDate) {
            this.toDate = toDate;
            return this;
        }
    }
}
