package ru.yandex.market.tpl.mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import one.util.streamex.StreamEx;
import org.springframework.web.client.RestClientException;

import ru.yandex.market.tpl.common.dsm.client.ApiClient;
import ru.yandex.market.tpl.common.dsm.client.api.EmployerApi;
import ru.yandex.market.tpl.common.dsm.client.model.EmployerDto;
import ru.yandex.market.tpl.common.dsm.client.model.EmployerTypeDto;
import ru.yandex.market.tpl.common.dsm.client.model.EmployerUpsertDto;
import ru.yandex.market.tpl.common.dsm.client.model.EmployersSearchResultDto;

public class EmployerApiEmulator extends EmployerApi {

    private final Map<String, EmployerDto> byId;
    private final AtomicLong cabinetIdSeq;

    public EmployerApiEmulator() {
        super();
        this.byId = new ConcurrentHashMap<>();
        this.cabinetIdSeq = new AtomicLong(0);
    }

    public EmployerApiEmulator(ApiClient apiClient) {
        super(apiClient);
        this.byId = new ConcurrentHashMap<>();
        this.cabinetIdSeq = new AtomicLong(0);
    }

    public void clear() {
        this.byId.clear();
        this.cabinetIdSeq.set(0);
    }

    public long nextCabinetMbiIdTest() {
        return this.cabinetIdSeq.getAndIncrement();
    }

    public EmployerDto getByIdTest(String employerDsmId) {
        return byId.get(employerDsmId);
    }

    @Override
    public EmployersSearchResultDto employersGet(
            Integer pageNumber,
            Integer pageSize,
            List<String> ids,
            String name,
            String nameSubstring,
            String login,
            String cabinetMbiId,
            EmployerTypeDto type,
            Boolean active
    ) throws RestClientException {
        List<EmployerDto> toReturn = new ArrayList<>(byId.values());

        if (ids != null && !ids.isEmpty()) {
            var idSet = new HashSet<>(ids);
            toReturn = StreamEx.of(toReturn)
                    .filter(employer -> idSet.contains(employer.getId()))
                    .toList();
        }

        if (name != null && !name.isBlank()) {
            toReturn = StreamEx.of(toReturn)
                    .filter(employer -> employer.getName().toLowerCase().contains(name.toLowerCase()))
                    .toList();
        }

        if (nameSubstring != null && !nameSubstring.isBlank()) {
            toReturn = StreamEx.of(toReturn)
                    .filter(employer -> employer.getName().toLowerCase().contains(nameSubstring.toLowerCase()))
                    .toList();
        }

        if (login != null && !login.isBlank()) {
            toReturn = StreamEx.of(toReturn)
                    .filter(employer -> employer.getLogin().toLowerCase().contains(login.toLowerCase()))
                    .toList();
        }

        if (cabinetMbiId != null && !cabinetMbiId.isBlank()) {
            toReturn = StreamEx.of(toReturn)
                    .filter(employer -> employer.getCompanyCabinetMbiId().equalsIgnoreCase(cabinetMbiId))
                    .toList();
        }

        if (type != null) {
            toReturn = StreamEx.of(toReturn)
                    .filter(employer -> type == employer.getType())
                    .toList();
        }

        if (active != null) {
            toReturn = StreamEx.of(toReturn)
                    .filter(employer -> active.equals(employer.getActive()))
                    .toList();
        }

        var reminder = toReturn.size() % pageSize;
        return new EmployersSearchResultDto()
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalPages((toReturn.size() / pageSize) + reminder > 0 ? 1 : 0)
                .totalElements((long) toReturn.size())
                .content(
                        StreamEx.of(toReturn)
                                .map(this::copy)
                                .toList()
                );
    }

    @Override
    public EmployerDto employersIdGet(String id) throws RestClientException {
        return copy(byId.get(id));
    }

    @Override
    public EmployerDto employersPut(EmployerUpsertDto employerUpsertDto) throws RestClientException {
        var employerDto = map(employerUpsertDto);
        if (employerDto.getId() == null) {
            employerDto.setId(UUID.randomUUID().toString());
        }
        return putInternal(copy(employerDto));
    }

    private EmployerDto putInternal(EmployerDto dto) {
        byId.put(dto.getId(), dto);
        return copy(dto);
    }

    private EmployerDto copy(EmployerDto dto) {
        return new EmployerDto()
                .id(dto.getId())
                .companyMbiId(dto.getCompanyMbiId())
                .type(dto.getType())
                .name(dto.getName())
                .login(dto.getLogin())
                .phoneNumber(dto.getPhoneNumber())
                .taxpayerNumber(dto.getTaxpayerNumber())
                .juridicalAddress(dto.getJuridicalAddress())
                .naturalAddress(dto.getNaturalAddress())
                .ogrn(dto.getOgrn())
                .legalForm(dto.getLegalForm())
                .companyCabinetMbiId(dto.getCompanyCabinetMbiId())
                .active(dto.getActive())
                .postCode(dto.getPostCode())
                .longName(dto.getLongName())
                .kpp(dto.getKpp())
                .bik(dto.getBik())
                .account(dto.getAccount())
                .legalAddressPostCode(dto.getLegalAddressPostCode())
                .legalAddressCity(dto.getLegalAddressCity())
                .legalAddressStreet(dto.getLegalAddressStreet())
                .legalAddressHome(dto.getLegalAddressHome())
                .legalFiasGuid(dto.getLegalFiasGuid())
                .nds(dto.getNds())
                .balanceClientId(dto.getBalanceClientId())
                .balancePersonId(dto.getBalancePersonId())
                .balanceContractId(dto.getBalanceContractId())
                .balanceRegistrationStatus(dto.getBalanceRegistrationStatus())
                .employerContactInfo(dto.getEmployerContactInfo());
    }

    private EmployerDto map(EmployerUpsertDto dto) {
        return new EmployerDto()
                .id(dto.getId())
                .companyMbiId(dto.getCompanyMbiId())
                .type(dto.getType())
                .name(dto.getName())
                .login(dto.getLogin())
                .phoneNumber(dto.getPhoneNumber())
                .taxpayerNumber(dto.getTaxpayerNumber())
                .juridicalAddress(dto.getJuridicalAddress())
                .naturalAddress(dto.getNaturalAddress())
                .ogrn(dto.getOgrn())
                .legalForm(dto.getLegalForm())
                .companyCabinetMbiId(dto.getCompanyCabinetMbiId())
                .active(dto.getActive())
                .postCode(dto.getPostCode())
                .longName(dto.getLongName())
                .kpp(dto.getKpp())
                .bik(dto.getBik())
                .account(dto.getAccount())
                .legalAddressPostCode(dto.getLegalAddressPostCode())
                .legalAddressCity(dto.getLegalAddressCity())
                .legalAddressStreet(dto.getLegalAddressStreet())
                .legalAddressHome(dto.getLegalAddressHome())
                .legalFiasGuid(dto.getLegalFiasGuid())
                .nds(dto.getNds())
                .balanceClientId(dto.getBalanceClientId())
                .balancePersonId(dto.getBalancePersonId())
                .balanceContractId(dto.getBalanceContractId())
                .balanceRegistrationStatus(dto.getBalanceRegistrationStatus())
                .employerContactInfo(dto.getEmployerContactInfo());
    }
}
