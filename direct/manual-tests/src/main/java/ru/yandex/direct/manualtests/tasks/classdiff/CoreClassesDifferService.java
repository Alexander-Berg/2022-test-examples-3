package ru.yandex.direct.manualtests.tasks.classdiff;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmDealsCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmYndxFrontpageCampaign;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalDistribCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalFreeCampaign;
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.manualtests.app.TestTasksRunner;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.multitype.typesupport.TypeSupport;

@Component
public class CoreClassesDifferService implements Runnable {

    @Autowired
    private List<TypeSupport<?>> allTypeSupports;

    public static void main(String[] args) {
        TestTasksRunner.runTask(CoreClassesDifferService.class, ClassDiffConfiguration.class, args);
    }

    @Override
    public void run() {
        List<Class<?>> classes = Arrays.asList(
                ContentPromotionCampaign.class,
                CpmBannerCampaign.class,
                CpmDealsCampaign.class,
                CpmPriceCampaign.class,
                CpmYndxFrontpageCampaign.class,
                DynamicCampaign.class,
                InternalAutobudgetCampaign.class,
                InternalDistribCampaign.class,
                InternalFreeCampaign.class,
                McBannerCampaign.class,
                MobileContentCampaign.class,
                SmartCampaign.class,
                TextCampaign.class);

        Set<ModelProperty<?, ?>> textCampaignFields = CoreClassesDiffer.getModelProperties(TextCampaign.class);
        Set<ModelProperty<?, ?>> otherUniqueFields = new HashSet<>();
        for (Class<?> klass: classes) {
            CoreClassesDiffResult diffResult = CoreClassesDiffer.diffClasses(
                    TextCampaign.class, klass, allTypeSupports, true, true);
            otherUniqueFields.addAll(diffResult.getSecondUniqueFields());
            System.out.println(diffResultToString(diffResult));
        }
        System.out.println(String.format("<{ Все поля не содержащиеся в %s:", TextCampaign.class.getSimpleName()));
        System.out.println("%%");
        System.out.print(getAllFieldsDiffAsString(textCampaignFields, otherUniqueFields));
        System.out.println("%%");
        System.out.println("}>");
    }

    private String getAllFieldsDiffAsString(
            Set<ModelProperty<?,?>> firstFields, Set<ModelProperty<?,?>> otherUniqueFields) {
        Set<ModelProperty<?,?>> firstUniqueFields = new HashSet<>(firstFields);
        firstUniqueFields.removeAll(otherUniqueFields);

        Set<String> firstUniqueFieldsNames = firstUniqueFields.stream()
                .map(ModelProperty::name).collect(Collectors.toSet());

        Map<String, List<ModelProperty<?, ?>>> otherFieldsByNameMap =
                otherUniqueFields.stream().collect(Collectors.groupingBy(ModelProperty::name));

        List<String> otherFieldsNames = otherFieldsByNameMap.keySet().stream().sorted().collect(Collectors.toList());
        StringBuilder sbld = new StringBuilder();
        for (int i = 0; i < otherFieldsNames.size(); i++) {
            String fieldName = otherFieldsNames.get(i);
            int duplicatesCount = otherFieldsByNameMap.get(fieldName).size() - 1 +
                    (firstUniqueFieldsNames.contains(fieldName) ? 1 : 0);
            sbld.append(String.format("%d. ", i + 1));
            if (duplicatesCount == 0) {
                sbld.append(fieldName);
            } else {
                sbld.append(String.format("%s (duplicates: %d)", fieldName, duplicatesCount));
            }
            sbld.append('\n');
        }
        return sbld.toString();
    }

    public String diffResultToString(CoreClassesDiffResult diffResult) {
        int maxFirstSupersLength = diffResult.getFirstUniqueSupers().stream()
                .map(klass -> klass.getSimpleName().length()).max(Comparator.naturalOrder()).orElse(0);
        int maxFirstFields = diffResult.getFirstUniqueFields().stream()
                .map(field -> field.name().length()).max(Comparator.naturalOrder()).orElse(0);
        int maxFirstLength = Math.max(diffResult.getFirst().getSimpleName().length(), maxFirstSupersLength);
        maxFirstLength = Math.max(maxFirstFields, maxFirstLength);

        int maxSecondSupersLength = diffResult.getSecondUniqueSupers().stream()
                .map(klass -> klass.getSimpleName().length()).max(Comparator.naturalOrder()).orElse(0);
        int maxSecondFields = diffResult.getSecondUniqueFields().stream()
                .map(field -> field.name().length()).max(Comparator.naturalOrder()).orElse(0);
        int maxSecondLength = Math.max(diffResult.getSecond().getSimpleName().length(), maxSecondSupersLength);
        maxSecondLength = Math.max(maxSecondFields, maxSecondLength);

        StringBuilder sbld = new StringBuilder();
        sbld.append(String.format("<{ %s <-> %s%n",
                diffResult.getFirst().getSimpleName(), diffResult.getSecond().getSimpleName()));
        sbld.append("%%\n");
        drawDividerLine(sbld, maxFirstLength, maxSecondLength);
        drawStrings(sbld,
                maxFirstLength, diffResult.getFirst().getSimpleName(),
                maxSecondLength, diffResult.getSecond().getSimpleName());
        drawDividerLine(sbld, maxFirstLength, maxSecondLength);
        Set<String> firstNames = diffResult.getFirstUniqueSupers().stream()
                .map(Class::getSimpleName).collect(Collectors.toSet());
        Set<String> secondNames = diffResult.getSecondUniqueSupers().stream()
                .map(Class::getSimpleName).collect(Collectors.toSet());
        drawStringsList(sbld, maxFirstLength, firstNames, maxSecondLength, secondNames);
        drawDividerLine(sbld, maxFirstLength, maxSecondLength);
        firstNames = diffResult.getFirstUniqueFieldsNames();
        secondNames = diffResult.getSecondUniqueFieldsNames();
        drawStringsList(sbld, maxFirstLength, firstNames, maxSecondLength, secondNames);
        drawDividerLine(sbld, maxFirstLength, maxSecondLength);
        sbld.append("%%\n");
        sbld.append(String.format("<{ %s unique TypeSupports: %n", diffResult.getSecond().getSimpleName()));
        sbld.append("%%\n");
        List<String> secondNamesList = diffResult.getSecondUniqueTypeSupports().stream()
                .map(Class::getSimpleName).sorted().collect(Collectors.toList());
        for (String name: secondNamesList) {
            sbld.append(name);
            sbld.append('\n');
        }
        sbld.append("%%\n");
        sbld.append("}>\n");
        sbld.append("}>\n");
        return sbld.toString();
    }

    private void drawStringsList(
            StringBuilder sbld,
            int maxFirstLength, Set<String> firstNamesSet,
            int maxSecondLength, Set<String> secondNamesSet) {
        List<String> firstNames = firstNamesSet.stream().sorted().collect(Collectors.toList());
        List<String> secondNames = secondNamesSet.stream().sorted().collect(Collectors.toList());
        int linesCount = Math.max(firstNames.size(), secondNames.size());
        for (int i = 0; i < linesCount; i++) {
            String first = i >= firstNames.size() ? "" : firstNames.get(i);
            String second = i >= secondNames.size() ? "" : secondNames.get(i);
            if (secondNamesSet.contains(first)) {
                first = first + " (duplicate)";
            }
            if (firstNamesSet.contains(second)) {
                second = second + " (duplicate)";
            }
            drawStrings(sbld, maxFirstLength, first, maxSecondLength, second);
        }
    }

    public static void drawDividerLine(StringBuilder sbld, int firstLength, int secondLength) {
        sbld.append('+');
        drawLine(sbld,'-', firstLength + 2);
        sbld.append('+');
        drawLine(sbld,'-', secondLength + 2);
        sbld.append('+');
        sbld.append('\n');
    }

    public static void drawStrings(
            StringBuilder sbld, int firstLength, String firstString, int secondLength, String secondString) {
        if (firstString == null) {
            firstString = "";
        }
        if (secondString == null) {
            secondString = "";
        }

        sbld.append('|');
        sbld.append(' ');
        sbld.append(firstString);
        drawLine(sbld,' ', firstLength - firstString.length());
        sbld.append(' ');
        sbld.append('|');
        sbld.append(' ');
        sbld.append(secondString);
        drawLine(sbld,' ', secondLength - secondString.length());
        sbld.append(' ');
        sbld.append('|');
        sbld.append('\n');
    }

    public static void drawLine(StringBuilder sbld, char c, int length) {
        for (int i = 0; i < length; i++) {
            sbld.append(c);
        }
    }

}
