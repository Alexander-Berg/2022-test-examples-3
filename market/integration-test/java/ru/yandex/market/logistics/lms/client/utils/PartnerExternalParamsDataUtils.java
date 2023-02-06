package ru.yandex.market.logistics.lms.client.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamGroup;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;

@UtilityClass
@ParametersAreNonnullByDefault
@SuppressWarnings("HideUtilityClassConstructor")
public class PartnerExternalParamsDataUtils {

    public static final String GET_PARTNER_EXTERNAL_PARAMS_QUERY = "* FROM "
        + "[//home/2022-03-02T08:05:24Z/partner_external_param_dyn] " +
        "WHERE key IN ('DISABLE_AUTO_CANCEL_AFTER_SLA', 'IS_DROPOFF')";

    public static final List<PartnerExternalParamType> PARTNER_EXTERNAL_PARAM_TYPES = List.of(
        PartnerExternalParamType.DISABLE_AUTO_CANCEL_AFTER_SLA,
        PartnerExternalParamType.IS_DROPOFF
    );

    public static final Set<PartnerExternalParamType> PARTNER_EXTERNAL_PARAM_TYPES_SET =
        new HashSet<>(PARTNER_EXTERNAL_PARAM_TYPES);

    @Nonnull
    public List<PartnerExternalParamGroup> partnerExternalParams() {
        return List.of(
            externalParamGroup(1L, PARTNER_EXTERNAL_PARAM_TYPES.get(0)),
            externalParamGroup(2L, PARTNER_EXTERNAL_PARAM_TYPES.get(1))
        );
    }

    @Nonnull
    private PartnerExternalParamGroup externalParamGroup(Long partnerId, PartnerExternalParamType paramType) {
        return new PartnerExternalParamGroup(
            partnerId,
            List.of(
                new PartnerExternalParam(
                    paramType.name(),
                    null,
                    "true"
                )
            )
        );
    }
}
