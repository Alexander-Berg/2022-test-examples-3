package ru.yandex.autotests.direct.cmd.data.representative;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;

public enum RepresentativeErrors implements ITextResource {
    AUTOPAYMENT_REPRESENTATIVE_DELETE;

    @Override
    public String getBundle() {
        return "cmd.representative.delete";
    }

    public String getErrorText() {
        return TextResourceFormatter.resource(this).
                locale(DirectTestRunProperties.getInstance().getDirectCmdLocale()).toString();
    }

}
