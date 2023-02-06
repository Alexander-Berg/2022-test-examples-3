package ru.yandex.market.psku.postprocessor.service.migration.convertor;

import java.io.File;
import java.io.FileWriter;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.common.util.StringUtils;
import ru.yandex.market.psku.postprocessor.common.db.dao.ConvertedPskuGroupDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuConvertStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ConvertedPskuGroup;

/**
 * @author Nur-Magomed Dzhamiev <a href="mailto:n-mago@yandex-team.ru"></a>
 */
public class ConvertedPskuGroupDaoMock extends ConvertedPskuGroupDao {

    private static final Logger log = LoggerFactory.getLogger(ConvertedPskuGroupDaoMock.class);

    private final ConcurrentMap<Long, ConvertedPskuGroup> pskuIdToConvertedGroup = new ConcurrentHashMap<>();

    private final String fileName;

    public ConvertedPskuGroupDaoMock() {
        super(null);
        fileName = new Timestamp(System.currentTimeMillis()).toString();
    }

    private final AtomicInteger count = new AtomicInteger(0);


    @Override
    public void insertOrUpdateWithCurrentTimeStamp(List<ConvertedPskuGroup> convertedPskuGroups) {
        count.addAndGet(convertedPskuGroups.size());
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        Map<Long, ConvertedPskuGroup> toAdd = convertedPskuGroups.stream().peek(group -> {
            Preconditions.checkArgument(group.getPskuId() != null,
                "psku id should not be null");
            Preconditions.checkArgument(group.getGroupId() != null,
                "group id should not be null for psku %d",
                group.getPskuId());
            Preconditions.checkArgument(group.getConvertStatus()!= null,
                "convert status should not be null for psku %d",
                group.getPskuId());
            Preconditions.checkArgument(!pskuIdToConvertedGroup.containsKey(group.getPskuId()),
                "Psku %d already was inserted",
                group.getPskuId());
            group.setConvertedOnTs(currentTimestamp);
        }).collect(Collectors.toMap(ConvertedPskuGroup::getPskuId, Function.identity()));
        storeToFile(toAdd);
        pskuIdToConvertedGroup.putAll(toAdd);
    }

    private synchronized void storeToFile(Map<Long, ConvertedPskuGroup> toAdd) {
        try {
            File file = new File("/Users/n-mago/temp/" + fileName + ".csv");
            FileWriter writer = new FileWriter(file, true);
            for (ConvertedPskuGroup group : toAdd.values()) {
                writer.write(String.format("%d;%d;%s;%s;%s\n", group.getPskuId(),
                    group.getGroupId(), group.getConvertStatus().toString(), group.getConvertedOnTs().toString(),
                    group.getFailedReason()));
            }
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateOfferDataForPsku(long pskuId, int businessId, String offerId) {
        Preconditions.checkArgument(businessId > 0,
            "Invalid bizId %d for psku %d",
            pskuId);
        Preconditions.checkArgument(StringUtils.isNotEmpty(offerId),
            "offer id %s is empty for psku %d",
            pskuId);
        if (pskuIdToConvertedGroup.containsKey(pskuId)) {
            pskuIdToConvertedGroup.get(pskuId).setBusinessId(businessId);
            pskuIdToConvertedGroup.get(pskuId).setOfferId(offerId);
        } else {
            log.warn("Inserting instead of updating for psku {}", pskuId);
            ConvertedPskuGroup group = new ConvertedPskuGroup();
            group.setPskuId(pskuId);
            group.setOfferId(offerId);
            group.setBusinessId(businessId);
            group.setConvertStatus(PskuConvertStatus.OK);
            group.setGroupId(-1);
            insertOrUpdateWithCurrentTimeStamp(Collections.singletonList(group));
        }
    }

    public long countErrors() {
       return pskuIdToConvertedGroup.values().stream()
            .filter(g -> g.getConvertStatus() == PskuConvertStatus.FAILED)
            .count();
    }

    public long countOk() {
        return pskuIdToConvertedGroup.values().stream()
            .filter(g -> g.getConvertStatus() == PskuConvertStatus.OK)
            .count();
    }

    public long countTotal() {
        System.out.println("count total: " + count);
        return pskuIdToConvertedGroup.size();
    }
}
