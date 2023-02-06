package ru.yandex.market.arbiter.test.util;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import ru.yandex.market.arbiter.api.client.dto.ConversationSide;
import ru.yandex.market.arbiter.api.server.dto.CreateConversationRequestDto;
import ru.yandex.market.arbiter.api.server.dto.NotificationChannelDto;

import static io.github.benas.randombeans.FieldPredicates.inClass;
import static io.github.benas.randombeans.FieldPredicates.isAnnotatedWith;
import static io.github.benas.randombeans.FieldPredicates.named;
import static io.github.benas.randombeans.FieldPredicates.ofType;
import static io.github.benas.randombeans.randomizers.net.UrlRandomizer.aNewUrlRandomizer;
import static io.github.benas.randombeans.randomizers.range.LongRangeRandomizer.aNewLongRangeRandomizer;
import static io.github.benas.randombeans.randomizers.text.StringDelegatingRandomizer.aNewStringDelegatingRandomizer;
import static java.util.function.Predicate.not;
import static ru.yandex.market.arbiter.test.util.RandomUtil.allDifferentElementsRandomizer;
import static ru.yandex.market.arbiter.test.util.RandomUtil.allDifferentStringRandomizer;
import static ru.yandex.market.arbiter.test.util.RandomUtil.fixedOffsetDateTimeRandomizer;
import static ru.yandex.market.arbiter.test.util.RandomUtil.randomValuesRandomizer;

/**
 * @author moskovkin@yandex-team.ru
 * @since 30.04.2020
 */
public class RandomDataGenerator {
    private RandomDataGenerator() {
    }

    public static void allGenerators(long seed, RandomUtil.EnhancedRandomEnhancedBuilder builder) {
        builder
                // Be careful here. There are no guaranty of order of applying random customizations.
                // For example [exclude "creationTime"] and [custom OffsetDateTime] randomizers some time
                // fill "creationTime" (has OffsetDateTime type) but some time exclude (named "creationTime").
                .apply(seed, RandomDataGenerator::excludeSpecialFieldsGenerator)
                .apply(seed, RandomDataGenerator::commonFieldsGenerator);
    }

    public static void creationAndModificationTimeGenerator(
            long seed,
            RandomUtil.EnhancedRandomEnhancedBuilder builder
    ) {
        builder
                .randomize(named("creationTime"), RandomUtil.instantRangeRandomizer(
                        TestClock.INSTANCE.instant(),
                        TestClock.INSTANCE.instant().plus(1, ChronoUnit.DAYS),
                        seed
                ))
                .randomize(named("modificationTime"), RandomUtil.instantRangeRandomizer(
                        TestClock.INSTANCE.instant().plus(2, ChronoUnit.DAYS),
                        TestClock.INSTANCE.instant().plus(3, ChronoUnit.DAYS),
                        seed
                ));
    }

    @SuppressWarnings("unchecked")
    public static void excludeSpecialFieldsGenerator(long seed, RandomUtil.EnhancedRandomEnhancedBuilder builder
    ) {
        builder
                .excludeField(
                        // Exclude JPA managed, Transient and some special fields from generation
                        isAnnotatedWith(Transient.class, Id.class,
                                ManyToOne.class, OneToMany.class, OneToOne.class, ManyToMany.class
                        )
                        .or(named("id"))
                        .or(named("creationTime")).or(named("modificationTime"))
                );
    }

    public static void commonFieldsGenerator(long seed, RandomUtil.EnhancedRandomEnhancedBuilder builder) {
        builder
                .randomize(
                        // This will generate OffsetDateTime in TestClock timezone with second precision
                        ofType(OffsetDateTime.class).and(
                                not(named("creationTime").or(named("modificationTime")))
                        ),
                        fixedOffsetDateTimeRandomizer(
                                seed,
                                TestClock.INSTANCE.getZone().getRules().getOffset(TestClock.INSTANCE.instant())
                        )
                )
                .randomize(
                        named("inn"), // 10-12 digits
                        aNewStringDelegatingRandomizer(aNewLongRangeRandomizer(1_000_000_000L, 999_999_999_999L, seed))
                ).randomize(
                        named("ogrn"),// 13 digits
                        aNewStringDelegatingRandomizer(aNewLongRangeRandomizer(1_000_000_000_000L, 9_999_999_999_999L, seed))
                )
                .randomize(
                        named("price").or(named("totalCost").or(named("amount"))),
                        aNewLongRangeRandomizer(0L, Long.MAX_VALUE, seed)
                )
                .randomize(
                        named("idInService"),
                        allDifferentStringRandomizer(seed, "serviceId_")
                )
                .randomize(inClass(
                        ru.yandex.market.arbiter.api.client.dto.CreateConversationRequestDto.class)
                                .and(named("messages")),
                        randomValuesRandomizer(seed, 0, 5,
                                new ru.yandex.market.arbiter.api.client.dto.MessageDto()
                                        .sender(ConversationSide.USER)
                                        .recipient(ConversationSide.MERCHANT)
                                        .text("Верниете")
                                        .attachments(List.of(
                                                new ru.yandex.market.arbiter.api.client.dto.AttachmentDto()
                                                        .name("check`.jpg")
                                                        .url("http://yandex.ru"),
                                                new ru.yandex.market.arbiter.api.client.dto.AttachmentDto()
                                                        .name("check2.jpg")
                                                        .url("http://yandex.ru")
                                        )),
                                new ru.yandex.market.arbiter.api.client.dto.MessageDto()
                                        .sender(ConversationSide.USER)
                                        .recipient(ConversationSide.MERCHANT)
                                        .text("Нет не вернем")
                                        .attachments(List.of())
                        )
                )
                .randomize(
                        inClass(CreateConversationRequestDto.class).and(named("notificationChannels")),
                        allDifferentElementsRandomizer(seed, NotificationChannelDto.class, 2, 2,
                                (o1, o2) -> Comparator.comparing(NotificationChannelDto::getConversationSide)
                                        .thenComparing(NotificationChannelDto::getType).compare(o1, o2)
                        )
                )
                .randomize(
                    inClass(ru.yandex.market.arbiter.api.client.dto.CreateConversationRequestDto.class)
                        .and(named("notificationChannels")),
                    allDifferentElementsRandomizer(
                        seed, ru.yandex.market.arbiter.api.client.dto.NotificationChannelDto.class,
                            2, 2,
                        (o1, o2) -> Comparator.comparing(
                                ru.yandex.market.arbiter.api.client.dto.NotificationChannelDto::getConversationSide
                        )
                        .thenComparing(ru.yandex.market.arbiter.api.client.dto.NotificationChannelDto::getType)
                                .compare(o1, o2)
                    )
                )
                .randomize(
                        named("url"),
                        aNewStringDelegatingRandomizer(aNewUrlRandomizer(seed))
                );
    }

    public static RandomUtil.EnhancedRandomEnhancedBuilder dataRandom(Class<?> clazz) {
        long seed = RandomUtil.seed(clazz);
        return dataRandom(seed);
    }

    public static RandomUtil.EnhancedRandomEnhancedBuilder dataRandom(long seed) {
        return RandomUtil.baseRandom(seed).apply(seed, RandomDataGenerator::allGenerators);
    }
}
