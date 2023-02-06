package ru.yandex.market.loyalty.core.stub;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import ru.yandex.market.loyalty.core.dao.ydb.UserReferralPromocodeDao;
import ru.yandex.market.loyalty.core.model.ydb.UserReferralPromocode;

public class YdbUserReferralPromocodeDaoStub implements StubDao, UserReferralPromocodeDao {

    private final ConcurrentMap<Long, List<UserReferralPromocode>> storageByUid = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<UserReferralPromocode>> storageByCode = new ConcurrentHashMap<>();

    @Override
    public void clear() {
        storageByUid.clear();
        storageByCode.clear();
    }

    @Override
    public List<UserReferralPromocode> getUserReferralPromocodes(@NotNull Long uid, @NotNull Instant activeOnTime) {
        return storageByUid.getOrDefault(uid, List.of()).stream()
                .filter(e -> !e.getAssignTime().isAfter(activeOnTime))
                .filter(e -> !e.getExpireTime().isBefore(activeOnTime))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserReferralPromocode> getUserReferralPromocodeCreatedOnDay(@NotNull Long uid,
                                                                                @NotNull Instant createdOnDay) {
        LocalDate createdOnDate = LocalDate.ofInstant(createdOnDay, ZoneId.of("UTC"));
        return storageByUid.getOrDefault(uid, List.of()).stream()
                .filter(e -> !e.getExpireTime().isBefore(createdOnDay))
                .filter(e -> LocalDate.ofInstant(e.getAssignTime(), ZoneId.of("UTC")).equals(createdOnDate))
                .max(Comparator.comparing(UserReferralPromocode::getAssignTime));
    }

    @Override
    public List<UserReferralPromocode> getPromocodeInfoByCode(String code) {
        return storageByCode.getOrDefault(code, List.of());
    }

    @Override
    public void insertNewEntry(@NotNull UserReferralPromocode userReferralPromocode) {
        storageByUid.merge(userReferralPromocode.getUid(), List.of(userReferralPromocode),
                (currentList, newValue) ->
                        Stream.concat(
                                currentList.stream(),
                                newValue.stream()
                        ).collect(Collectors.toList())
        );
        storageByCode.merge(userReferralPromocode.getPromocode(), List.of(userReferralPromocode),
                (currentList, newValue) ->
                        Stream.concat(
                                currentList.stream(),
                                newValue.stream()
                        ).collect(Collectors.toList())
        );
    }

    @Override
    public List<UserReferralPromocode> getUserReferralPromocodes(@NotNull Long uid, @NotNull Instant assignTimeFrom,
                                                                 @NotNull Instant assignTimeTo) {
        return storageByUid.getOrDefault(uid, List.of()).stream()
                .filter(e -> !e.getAssignTime().isBefore(assignTimeFrom))
                .filter(e -> !e.getAssignTime().isAfter(assignTimeTo))
                .collect(Collectors.toList());
    }
}
