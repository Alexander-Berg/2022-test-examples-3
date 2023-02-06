package ru.yandex.market.tpl.mock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.apache.commons.collections4.SetUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;

import ru.yandex.market.tpl.common.dsm.client.ApiClient;
import ru.yandex.market.tpl.common.dsm.client.MarketDeliveryStaffManagerException;
import ru.yandex.market.tpl.common.dsm.client.api.DriverApi;
import ru.yandex.market.tpl.common.dsm.client.model.DriverDto;
import ru.yandex.market.tpl.common.dsm.client.model.DriverPersonalDataDto;
import ru.yandex.market.tpl.common.dsm.client.model.DriverSearchResultDto;
import ru.yandex.market.tpl.common.dsm.client.model.DriverUpsertDto;
import ru.yandex.market.tpl.common.dsm.client.model.TagDto;
import ru.yandex.market.tpl.mock.driver.Driver;
import ru.yandex.market.tpl.mock.driver.PassportData;
import ru.yandex.market.tpl.mock.driver.PersonalData;

public class DriverApiEmulator extends DriverApi {

    private final Map<String, Driver> byId;
    private final Map<String, Set<String>> employerIdToDriverId;
    private final Map<String, Set<String>> driversIdByName;
    private final Map<LocalDate, Set<String>> driverIdByBirthday;
    private final Map<String, String> driverIdByPassportNumber;
    private final Map<String, String> driverIdByUid;
    private final Map<String, String> driverIdByPhone;

    public DriverApiEmulator() {
        this.byId = new ConcurrentHashMap<>();
        this.employerIdToDriverId = new ConcurrentHashMap<>();
        this.driverIdByPassportNumber = new ConcurrentHashMap<>();
        this.driverIdByUid = new ConcurrentHashMap<>();
        this.driverIdByPhone = new ConcurrentHashMap<>();
        this.driversIdByName = new ConcurrentHashMap<>();
        this.driverIdByBirthday = new ConcurrentHashMap<>();
    }

    public DriverApiEmulator(ApiClient apiClient) {
        super(apiClient);
        this.byId = new ConcurrentHashMap<>();
        this.employerIdToDriverId = new ConcurrentHashMap<>();
        this.driverIdByPassportNumber = new ConcurrentHashMap<>();
        this.driverIdByUid = new ConcurrentHashMap<>();
        this.driverIdByPhone = new ConcurrentHashMap<>();
        this.driversIdByName = new ConcurrentHashMap<>();
        this.driverIdByBirthday = new ConcurrentHashMap<>();
    }

    public void clear() {
        this.byId.clear();
        this.employerIdToDriverId.clear();
        this.driverIdByPassportNumber.clear();
        this.driverIdByUid.clear();
        this.driverIdByPhone.clear();
        this.driversIdByName.clear();
        this.driverIdByBirthday.clear();
    }

    @Override
    public void driversIdBlackListedDelete(String id) throws RestClientException {
        findByIdOrThrow(id).setBlackListed(false);
    }

    @Override
    public void driversIdBlackListedPut(String id) throws RestClientException {
        findByIdOrThrow(id).setBlackListed(true);
    }

    @Override
    public void driversIdEmployersEmployerIdDelete(String id, String employerId) throws RestClientException {
        findByIdOrThrow(id).getEmployerIds().remove(employerId);
        if (employerIdToDriverId.containsKey(employerId)) {
            employerIdToDriverId.get(employerId).remove(id);
        }
    }

    @Override
    public void driversIdEmployersEmployerIdPut(String id, String employerId) throws RestClientException {
        var employers = findByIdOrThrow(id).getEmployerIds();
        if (!employers.contains(employerId)) {
            employers.add(employerId);
        }
        if (!employerIdToDriverId.containsKey(employerId)) {
            employerIdToDriverId.put(employerId, new HashSet<>());
        }
        employerIdToDriverId.get(employerId).add(id);
    }

    @Override
    public DriverPersonalDataDto driverByUidUidPersonalDataGet(String uid) throws RestClientException {
        return toPersonalData(findByUIDOrThrow(uid));
    }

    @Override
    public DriverPersonalDataDto driversIdPersonalDataGet(String id) throws RestClientException {
        return toPersonalData(findByIdOrThrow(id));
    }

    @Override
    public DriverDto driversIdGet(String id) throws RestClientException {
        return toDto(findByIdOrThrow(id));
    }

    @Override
    public DriverSearchResultDto driversGet(
            Integer pageNumber,
            Integer pageSize,
            List<String> id,
            List<String> uid,
            String employerId,
            List<String> employerIds,
            String phonePart,
            String namePart,
            LocalDate birthday,
            Boolean blackListed,
            String sort
    ) throws RestClientException {
        Set<String> driverIds = new HashSet<>(byId.keySet());

        if (id != null && !id.isEmpty()) {
            var idSet = new HashSet<>(id);
            driverIds = driverIds.stream()
                    .filter(idSet::contains)
                    .collect(Collectors.toSet());
        }

        if (employerIds != null && !employerIds.isEmpty()) {
            Set<String> byEmployerId = new HashSet<>();
            employerIds.forEach(curEmpId -> {
                        if (employerIdToDriverId.containsKey(curEmpId)) {
                            byEmployerId.addAll(employerIdToDriverId.get(curEmpId));
                        }
                    }
            );
            driverIds = SetUtils.intersection(driverIds, byEmployerId);
        }

        if (phonePart != null && !phonePart.isBlank()) {
            Set<String> byPhone = StreamEx.of(driverIdByPhone.entrySet())
                    .filter(entry -> entry.getKey().contains(phonePart))
                    .map(Map.Entry::getValue)
                    .toSet();
            driverIds = SetUtils.intersection(driverIds, byPhone);
        }

        if (namePart != null && !namePart.isBlank()) {
            Set<String> byName = StreamEx.of(driversIdByName.entrySet())
                    .filter(entry -> entry.getKey().toLowerCase().contains(namePart.toLowerCase()))
                    .map(Map.Entry::getValue)
                    .flatMap(Collection::stream)
                    .toSet();
            driverIds = SetUtils.intersection(driverIds, byName);
        }

        if (birthday != null) {
            Set<String> byBirthday = StreamEx.of(driverIdByBirthday.entrySet())
                    .filter(entry -> birthday.equals(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .flatMap(Collection::stream)
                    .toSet();
            driverIds = SetUtils.intersection(driverIds, byBirthday);
        }

        List<Driver> filteredDrivers = StreamEx.of(driverIds)
                .filter(byId::containsKey)
                .map(byId::get)
                .toList();

        if (blackListed != null) {
            filteredDrivers = StreamEx.of(filteredDrivers)
                    .filterBy(Driver::isBlackListed, blackListed)
                    .toList();
        }

        var reminder = filteredDrivers.size() % pageSize;
        return new DriverSearchResultDto()
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalPages((filteredDrivers.size() / pageSize) + reminder > 0 ? 1 : 0)
                .totalElements((long) filteredDrivers.size())
                .content(
                        StreamEx.of(filteredDrivers)
                                .map(DriverApiEmulator::toDto)
                                .toList()
                );
    }

    @Override
    public DriverDto driverByUidUidGet(String uid) throws RestClientException {
        return toDto(findByUIDOrThrow(uid));
    }

    @Override
    public DriverDto driverByPassportSerialNumberGet(String serialNumber) throws RestClientException {
        return toDto(findByPassportOrThrow(serialNumber));
    }

    @Override
    public DriverDto driversIdUpdateUidUidPut(String id, String uid) throws RestClientException {
        var driver = findByIdOrThrow(id);
        driver.setUid(uid);
        return toDto(driver);
    }

    @Override
    public DriverDto driversPut(DriverUpsertDto upsert) throws RestClientException {
        Driver driver;
        if (upsert.getId() == null) {
            driver = toDriver(upsert);
            driver.setId(UUID.randomUUID().toString());
            putDriver(driver);
        } else {
            driver = toDriver(upsert);
            putDriver(driver);
        }
        return toDto(driver);
    }

    private Driver findByIdOrThrow(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }
        if (!byId.containsKey(id)) {
            throw new IllegalArgumentException("does not contain id " + id);
        }
        return byId.get(id);
    }

    private Driver findByPassportOrThrow(String serial) {
        if (serial == null) {
            throw new IllegalArgumentException("serial is null");
        }
        if (!driverIdByPassportNumber.containsKey(serial)) {
            throw new MarketDeliveryStaffManagerException(
                    HttpStatus.NOT_FOUND, "does not contain serial " + serial, new HttpHeaders());
        }
        return findByIdOrThrow(driverIdByPassportNumber.get(serial));
    }

    private Driver findByUIDOrThrow(String uid) {
        if (uid == null) {
            throw new IllegalArgumentException("uid is null");
        }
        if (!driverIdByUid.containsKey(uid)) {
            throw new IllegalArgumentException("does not contain uid " + uid);
        }
        return findByIdOrThrow(driverIdByUid.get(uid));
    }

    private void putDriver(Driver driver) {
        String driverDsmId = driver.getId();
        if (byId.containsKey(driverDsmId)) {
            removeDriverById(driverDsmId);
        }

        byId.put(driverDsmId, driver);

        Optional.ofNullable(driver.getPersonalData())
                .map(PersonalData::getPassportData)
                .map(PassportData::getSerialNumber)
                .ifPresent(serial -> driverIdByPassportNumber.put(serial, driverDsmId));

        driverIdByUid.put(driverDsmId, driver.getId());

        if (driver.getEmployerIds() != null) {
            driver.getEmployerIds().forEach(employerId -> {
                if (!employerIdToDriverId.containsKey(employerId)) {
                    employerIdToDriverId.put(employerId, new HashSet<>());
                }
                employerIdToDriverId.get(employerId).add(driverDsmId);
            });
        }

        Optional.ofNullable(driver.getPersonalData())
                .map(PersonalData::getPhone)
                .ifPresent(phone -> driverIdByPhone.put(phone, driverDsmId));

        String name = driver.getPersonalData().getName();
        if (!driversIdByName.containsKey(name)) {
            driversIdByName.put(name, new HashSet<>());
        }
        driversIdByName.get(name).add(driverDsmId);

        var birthday = driver.getPersonalData().getPassportData().getBirthDate();
        if (birthday != null) {
            if (!driverIdByBirthday.containsKey(birthday)) {
                driverIdByBirthday.put(birthday, new HashSet<>());
            }
            driverIdByBirthday.get(birthday).add(driverDsmId);
        }
    }

    private void removeDriverById(String driverDsmId) {
        Driver old = byId.get(driverDsmId);

        Optional.ofNullable(old.getPersonalData())
                .map(PersonalData::getPassportData)
                .map(PassportData::getSerialNumber)
                .ifPresent(driverIdByPassportNumber::remove);

        driverIdByUid.remove(old.getUid());

        if (old.getEmployerIds() != null) {

            old.getEmployerIds().forEach(employerId -> {
                if (employerIdToDriverId.containsKey(employerId)) {
                    employerIdToDriverId.get(employerId).remove(driverDsmId);
                }
            });
        }

        Optional.ofNullable(old.getPersonalData())
                .map(PersonalData::getPhone)
                .ifPresent(driverIdByPhone::remove);

        String name = old.getPersonalData().getName();
        if (driversIdByName.containsKey(name)) {
            driversIdByName.get(name).remove(driverDsmId);
        }

        var birthday = old.getPersonalData().getPassportData().getBirthDate();
        if (birthday != null && driverIdByBirthday.containsKey(birthday)) {
            driverIdByBirthday.get(birthday).remove(driverDsmId);
        }

        byId.remove(driverDsmId);
    }

    public Driver getByIdTest(String dsmId) {
        return byId.get(dsmId);
    }

    public void removePassportTest(String dsmId) {
        if (byId.containsKey(dsmId)) {

            var passport = byId.get(dsmId).getPersonalData().getPassportData();
            var serial = passport.getSerialNumber();
            if (serial != null) {
                driverIdByPassportNumber.remove(serial);
                passport.setSerialNumber(null);
            }
        }
    }

    public int size() {
        return byId.size();
    }

    private static Driver toDriver(DriverUpsertDto upsert) {
        List<String> employerIds = new ArrayList<>();
        if (upsert.getEmployerId() != null) {
            employerIds.add(upsert.getEmployerId());
        }

        var passport = new PassportData();
        passport.setCitizenship(upsert.getPersonalData().getNationality());
        passport.setSerialNumber(upsert.getPersonalData().getPassportNumber());
        passport.setFirstName(upsert.getPersonalData().getFirstName());
        passport.setLastName(upsert.getPersonalData().getLastName());
        passport.setPatronymic(upsert.getPersonalData().getPatronymicName());
        passport.setBirthDate(upsert.getPersonalData().getBirthday());
        passport.setIssuer(upsert.getPersonalData().getIssuer());
        passport.setIssueDate(upsert.getPersonalData().getIssuedAt());

        String name = upsert.getPersonalData().getLastName() + " " + upsert.getPersonalData().getFirstName();
//        String patronymic = upsert.getPersonalData().getPatronymicName();
//        if (patronymic != null && !patronymic.isBlank()) {
//            name += " " + patronymic;
//        }

        var personal = new PersonalData();
        personal.setEmail(upsert.getPersonalData().getEmail());
        personal.setName(name);
        personal.setPhone(upsert.getPersonalData().getPhone());
        personal.setTelegram(upsert.getPersonalData().getTelegramLogin());
        personal.setPassportData(passport);

        var driver = new Driver();
        driver.setId(upsert.getId());
        driver.setUid(upsert.getUid());
        driver.setEmployerIds(employerIds);
        driver.setBlackListed(false);
        driver.setPersonalData(personal);

        return driver;
    }

    private static DriverDto toDto(Driver driver) {
        List<TagDto> tags = new ArrayList<>();
        Optional.ofNullable(driver.getPersonalData())
                .map(PersonalData::getPassportData)
                .map(PassportData::getSerialNumber)
                .ifPresentOrElse(s -> {
                }, () -> tags.add(TagDto.NO_PASSPORT));
        return new DriverDto()
                .id(driver.getId())
                .uid(driver.getUid())
                .employerIds(new ArrayList<>(driver.getEmployerIds()))
                .name(driver.getPersonalData().getName())
                .lastName(driver.getPersonalData().getPassportData().getLastName())
                .firstName(driver.getPersonalData().getPassportData().getFirstName())
                .patronymic(driver.getPersonalData().getPassportData().getPatronymic())
                .phone(driver.getPersonalData().getPhone())
                .blackListed(driver.isBlackListed())
                .birthday(driver.getPersonalData().getPassportData().getBirthDate())
                .tags(tags);
    }

    private static DriverPersonalDataDto toPersonalData(Driver driver) {
        return new DriverPersonalDataDto()
                .id(driver.getId())
                .email(driver.getPersonalData().getEmail())
                .name(driver.getPersonalData().getName())
                .lastName(driver.getPersonalData().getPassportData().getLastName())
                .firstName(driver.getPersonalData().getPassportData().getFirstName())
                .patronymic(driver.getPersonalData().getPassportData().getPatronymic())
                .phone(driver.getPersonalData().getPhone())
                .passportNumber(driver.getPersonalData().getPassportData().getSerialNumber())
                .nationality(driver.getPersonalData().getPassportData().getCitizenship())
                .birthday(driver.getPersonalData().getPassportData().getBirthDate())
                .issuedAt(driver.getPersonalData().getPassportData().getIssueDate())
                .issuer(driver.getPersonalData().getPassportData().getIssuer());
    }
}
