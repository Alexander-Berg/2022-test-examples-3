package ru.yandex.market.fintech.banksint.controller.installment;

import java.util.List;

import ru.yandex.market.fintech.banksint.mybatis.installment.model.SupplierType;
import ru.yandex.market.fintech.instalment.model.InstallmentAvailabilityDto;
import ru.yandex.market.fintech.instalment.model.InstallmentDto;

import static ru.yandex.market.fintech.instalment.model.InstallmentDto.SupplierTypeEnum;

public class InstallmentUtils {
    private InstallmentUtils() {
    }

    @SuppressWarnings("ParameterNumber")
    public static InstallmentDto createInstallmentDto(
            String name,
            String description,
            int durationInDays,
            String id,
            float percentage,
            int minPrice,
            int maxPrice,
            SupplierType supplierType
    ) {
        var installment = new InstallmentDto();
        installment.setId(id);
        installment.setName(name);
        installment.setDescription(description);
        installment.setDuration(durationInDays);
        installment.setPercentage(percentage);
        installment.setMinPrice(minPrice);
        installment.setMaxPrice(maxPrice);
        installment.setSupplierType(SupplierTypeEnum.fromValue(supplierType.name()));
        return installment;
    }

    public static InstallmentAvailabilityDto createInstallmentAvailabilityDto(
            boolean available,
            List<String> reasons,
            InstallmentDto installmentDto
    ) {
        var installmentAvailabilityDto = new InstallmentAvailabilityDto();
        installmentAvailabilityDto.setAvailable(available);
        installmentAvailabilityDto.setReasonСodes(reasons);
        installmentAvailabilityDto.setInstallment(installmentDto);
        return installmentAvailabilityDto;
    }
}
