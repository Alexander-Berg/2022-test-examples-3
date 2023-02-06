package ru.yandex.market.crm.campaign.test.utils;

import java.util.Collections;

import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingVariantConf;
import ru.yandex.market.crm.core.domain.sending.conf.BlockConf;

/**
 * @author apershukov
 */
public final class EmailSendingConfigUtils {

    public static EmailSendingVariantConf variant(String id,
                                                  int percent,
                                                  String templateId,
                                                  String subject,
                                                  BlockConf blockConf) {
        EmailSendingVariantConf variant = new EmailSendingVariantConf();
        variant.setId(id);
        variant.setTemplate(templateId);
        variant.setPercent(percent);
        variant.setBlocks(Collections.singletonList(blockConf));
        variant.setSubject(subject);

        return variant;
    }

    public static EmailSendingVariantConf variant(String id,
                                                  int percent,
                                                  String templateId,
                                                  BlockConf blockConf) {
        return variant(id, percent, templateId, "Test subject", blockConf);
    }
}
