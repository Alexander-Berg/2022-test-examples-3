package ru.yandex.direct.core.testing.steps.campaign.repository0.strategy;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Component;

import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxImpressionsCustomPeriodStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxImpressionsStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxReachCustomPeriodStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxReachStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageBidStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageCpaPerCampStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageCpaStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.ManualStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.Strategy;

@Component
public class StrategyWriterFactory {

    private enum WriterType {

        MANUAL(ManualStrategy.class) {
            @Override
            public StrategyWriter getWriter() {
                return new ManualStrategyWriter();
            }
        },
        AVERAGE_BID(AverageBidStrategy.class) {
            @Override
            public StrategyWriter getWriter() {
                return new AverageBidStrategyWriter();
            }
        },
        AVERAGE_CPA(AverageCpaStrategy.class) {
            @Override
            public StrategyWriter getWriter() {
                return new AverageCpaStrategyWriter();
            }
        },
        AUTOBUDGET_AVG_CPA_PER_CAMP(AverageCpaPerCampStrategy.class) {
            @Override
            public StrategyWriter getWriter() {
                return new AverageCpaPerCampStrategyWriter();
            }
        },
        AUTOBUDGET_MAX_REACH(AutobudgetMaxReachStrategy.class) {
            @Override
            public StrategyWriter getWriter() {
                return new AutobudgetStrategyWriter(StrategyName.AUTOBUDGET_MAX_REACH);
            }
        },
        AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD(AutobudgetMaxReachCustomPeriodStrategy.class) {
            @Override
            public StrategyWriter getWriter() {
                return new AutobudgetCustomPeriodStrategyWriter(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD);
            }
        },
        AUTOBUDGET_MAX_IMPRESSIONS(AutobudgetMaxImpressionsStrategy.class) {
            @Override
            public StrategyWriter getWriter() {
                return new AutobudgetStrategyWriter(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS);
            }
        },
        AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD(AutobudgetMaxImpressionsCustomPeriodStrategy.class) {
            @Override
            public StrategyWriter getWriter() {
                return new AutobudgetCustomPeriodStrategyWriter(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD);
            }
        };

        private Class<? extends Strategy> supportedType;

        WriterType(Class<? extends Strategy> supportedType) {
            this.supportedType = supportedType;
        }

        public Class<? extends Strategy> getSupportedType() {
            return supportedType;
        }

        public abstract StrategyWriter getWriter();

        private static final Map<Class<? extends Strategy>, WriterType> lookup;

        static {
            ImmutableMap.Builder<Class<? extends Strategy>, WriterType> builder =
                    ImmutableMap.builder();
            for (WriterType writerType : values()) {
                builder.put(writerType.getSupportedType(), writerType);
            }
            lookup = builder.build();
        }

        public static WriterType getByType(Class<? extends Strategy> strategyType) {
            return lookup.get(strategyType);
        }
    }

    // todo unit-тест на возможность получить StrategyWriter для всех существующих типов стратегий (reflection)
    public StrategyWriter getStrategyWriter(Class<? extends Strategy> strategyType) {
        WriterType writerType = WriterType.getByType(strategyType);
        if (writerType == null) {
            throw new IllegalArgumentException("there is no registered writer for strategy of type " + strategyType);
        }
        return writerType.getWriter();
    }
}
