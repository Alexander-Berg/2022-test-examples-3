package ru.yandex.direct.internaltools.tools.testusers.preprocessors;

import com.google.common.collect.ImmutableSet;
import one.util.streamex.StreamEx;
import org.springframework.stereotype.Component;

import ru.yandex.direct.internaltools.core.container.InternalToolParameter;
import ru.yandex.direct.internaltools.core.input.InternalToolInput;
import ru.yandex.direct.internaltools.core.input.InternalToolInputPreProcessor;
import ru.yandex.direct.rbac.RbacRole;

import static ru.yandex.direct.validation.constraint.CommonConstraints.inSet;

@Component
public class TestUserRbacRolePreProcessor implements InternalToolInputPreProcessor<RbacRole> {

    @Override
    public <T extends InternalToolParameter> InternalToolInput.Builder<T, RbacRole> preCreate(
            InternalToolInput.Builder<T, RbacRole> inputBuilder) {
        var roles = StreamEx.of(RbacRole.values()).filter(RbacRole::isInternal).toList();
        return inputBuilder
                .withAllowedValues(roles)
                .addValidator(inSet(ImmutableSet.<RbacRole>builder().addAll(roles).build()));
    }

}
