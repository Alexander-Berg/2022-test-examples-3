package ru.yandex.common.framework.http;

import ru.yandex.common.framework.ca.ConfigAuditor;
import ru.yandex.common.framework.ca.DeploymentGroup;
import ru.yandex.common.framework.ca.PingResult;

/**
 * Created by IntelliJ IDEA.
 * User: alex-kovalenko
 * Date: 06.08.2009
 * Time: 16:07:20
 */
public class TestConfigAuditor implements ConfigAuditor {

    @Override
    public boolean audit(final DeploymentGroup group, final StringBuilder comment) {
        return true;
    }

    @Override
    public PingResult ping(final DeploymentGroup group, final StringBuilder pingReply) {
        return PingResult.OK;
    }
}
