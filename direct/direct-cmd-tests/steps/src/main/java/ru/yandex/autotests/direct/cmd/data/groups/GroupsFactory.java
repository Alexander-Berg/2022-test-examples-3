package ru.yandex.autotests.direct.cmd.data.groups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.interest.RelevanceMatch;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;

import static java.util.Collections.singletonList;
import static ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory.MAX_PHRASE_COUNT;
import static ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory.MAX_PHRASE_SIZE;
import static ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory.MAX_WORD_COUNT;
import static ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory.WORD_MAX_SIZE;
import static ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory.getDefaultPhrase;
import static ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory.getSortedMinusWords;

public class GroupsFactory {

    public static String getPhraseWithOneMaxWord() {
        return RandomStringUtils.randomAlphabetic(WORD_MAX_SIZE);
    }

    public static Group getDefaultTextGroup() {
        return new Group()
                .withAdGroupID("0")
                .withAdGroupName("Новая группа объявлений")
                .withAdGroupType("base")
                .withBanners(new ArrayList<>(singletonList(BannersFactory.getDefaultTextBanner())))
                .withPhrases(new ArrayList<>(singletonList(getDefaultPhrase())))
                .withMinusWords(Collections.emptyList())
                .withGeo("225")
                .withTags(new Object())
                .withRetargetings(Collections.emptyList())
                .withHierarchicalMultipliers(new HierarchicalMultipliers());
    }

    public static String getPhraseWithTooLongWord() {
        return getPhraseWithOneMaxWord() + RandomStringUtils.randomAlphabetic(1);
    }

    public static String getLongPhrase() {
        return getPhraseWithOneMaxWord() + getSortedMinusWords(MAX_PHRASE_SIZE - WORD_MAX_SIZE);
    }

    public static Group getCommonMobileAppGroup() {
        return BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_GROUP_MOBILE_DEFAULT2, Group.class);
    }

    public static Group getCommonDynamicGroup() {
        return BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_GROUP_DYNAMIC_DEFAULT2, Group.class);
    }


    public static Group getGroupWithLongPhrase() {
        Group group = getDefaultTextGroup();
        group.getPhrases().get(0).withPhrase(getLongPhrase());
        group.setPhrases(group.getPhrases().subList(0, 1));
        return group;
    }

    public static Group getGroupWithALotOfPhrases() {
        Group group = getDefaultTextGroup();
        List<Phrase> list = new ArrayList<>();
        for (int i = 0; i < MAX_PHRASE_COUNT; i++) {
            list.add(BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_PHRASE_DEFAULT2, Phrase.class)
                    .withPhrase(RandomStringUtils.randomAlphabetic(20)));
        }
        list.sort(Comparator.comparing(Phrase::getPhrase));
        group.setPhrases(list);
        return group;
    }

    public static Group getGroupWithPhraseWithALotOfWords() {
        Group group = getDefaultTextGroup();
        String phrase = "";
        for (int i = 0; i < MAX_WORD_COUNT - 1; i++) {
            phrase += RandomStringUtils.randomAlphabetic(2) + " ";
        }
        phrase += RandomStringUtils.randomAlphabetic(2);
        group.getPhrases().get(0).withPhrase(phrase);
        return group;
    }

    public static Group getGroupWithALotOfLongPhrases() {
        Group group = getDefaultTextGroup();
        List<Phrase> list = new ArrayList<>();
        for (int i = 0; i < MAX_WORD_COUNT; i++) {
            list.add(BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_PHRASE_DEFAULT2, Phrase.class)
                    .withPhrase(getLongPhrase()));
        }
        list.sort(Comparator.comparing(Phrase::getPhrase));
        group.setPhrases(list);
        return group;
    }

    public static Group getGroupWithPhraseWithSameKeyWordsAndMinusWords() {
        Group group = getDefaultTextGroup();
        group.getPhrases().get(0).withPhrase("колеса на авто -!на");
        return group;
    }

    public static Group getGroupWithTooLongWordPhrase() {
        Group group = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_GROUP_TEXT_DEFAULT2, Group.class);
        group.getPhrases().get(0).withPhrase(getPhraseWithTooLongWord());
        return group;
    }

    public static Group getGroupWithTooLongPhrase() {
        Group group = getDefaultTextGroup();
        group.getPhrases().get(0).withPhrase(getLongPhrase() + "-minus");
        return group;
    }

    public static Group getGroupWithPhraseFromSpaces() {
        Group group = getDefaultTextGroup();
        group.getPhrases().get(0).withPhrase("        ");
        return group;
    }

    public static Group getGroupWithTooMuchPhrases() {
        Group group = getDefaultTextGroup();
        Phrase phrase = group.getPhrases().get(0).withPhrase(RandomStringUtils.randomAlphabetic(20));
        group.setPhrases(Collections.nCopies(MAX_PHRASE_COUNT + 1, phrase));
        return group;
    }

    public static Group getGroupWithPhraseFromMinusWords() {
        Group group = getDefaultTextGroup();
        group.getPhrases().get(0).withPhrase("-раз -два");
        return group;
    }

    public static Group getGroupWithPhraseWithSeparateDots() {
        Group group = getDefaultTextGroup();
        group.getPhrases().get(0).withPhrase("абв . где");
        return group;
    }

    public static Group getGroupWithPhraseWithBannedWords() {
        Group group = getDefaultTextGroup();
        group.getPhrases().get(0).withPhrase("@#$%^&*() фраза с запрещенными символами");
        return group;
    }

    public static Group getGroupWithPhraseStartsFromDot() {
        Group group = getDefaultTextGroup();
        group.getPhrases().get(0).withPhrase(".абырвалг");
        return group;
    }

    public static Group getGroupWithPhraseWithMunisWordsInQuotes() {
        Group group = getDefaultTextGroup();
        group.getPhrases().get(0).withPhrase("фраза \"минус -слова кавычках\"'");
        return group;
    }

    public static Group getGroupWithPhraseWithEmptyAbPriority() {
        Group group = getDefaultTextGroup();
        group.getPhrases().get(0).withAutobudgetPriority(null);
        return group;
    }

    public static Group getGroupWithPhraseWithTooMuchWords() {
        Group group = getDefaultTextGroup();
        String phrase = "";
        for (int i = 0; i < MAX_WORD_COUNT; i++) {
            phrase += RandomStringUtils.randomAlphabetic(2) + " ";
        }
        phrase += RandomStringUtils.randomAlphabetic(2);
        group.getPhrases().get(0).withPhrase(phrase);
        return group;
    }

    public static RelevanceMatch getDefaultRelevanceMatch() {
        return new RelevanceMatch()
                .withBidId(0L)
                .withPrice(0.78d);
    }
}
