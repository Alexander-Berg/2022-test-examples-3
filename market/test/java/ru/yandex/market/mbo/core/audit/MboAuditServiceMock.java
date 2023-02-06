package ru.yandex.market.mbo.core.audit;

import org.apache.commons.lang.time.DateUtils;
import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mbo.http.MboAuditService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author s-ermakov
 */
public class MboAuditServiceMock implements MboAuditService {
    private int idGenerator = 0;
    private long timestampGenerator = 10000;
    private List<MboAudit.MboAction> auditActions = new ArrayList<>();

    @Override
    public MboAudit.FindActionsResponse findActions(MboAudit.FindActionsRequest findActionsRequest) {
        MboAudit.FindActionsResponse.Builder response = MboAudit.FindActionsResponse.newBuilder();
        getFilterStream(findActionsRequest)
            .forEach(response::addActions);
        return response.build();
    }

    @Override
    public MboAudit.CountActionsResponse countActions(MboAudit.FindActionsRequest findActionsRequest) {
        return MboAudit.CountActionsResponse.newBuilder()
            .addCount(getFilterStream(findActionsRequest).count())
            .build();
    }

    @Override
    public MboAudit.VoidResponse writeActions(MboAudit.WriteActionsRequest writeActionsRequest) {
        long eventId = ++idGenerator;
        long currentDatetime = timestampGenerator = timestampGenerator + 100;

        writeActionsRequest.getActionsList().stream()
            .map(action -> {
                MboAudit.MboAction.Builder builder = MboAudit.MboAction.newBuilder(action);
                if (!builder.hasEventId()) {
                    builder.setEventId(eventId);
                }
                if (!builder.hasDate()) {
                    builder.setDate(currentDatetime);
                }
                return builder.build();
            })
            .forEach(a -> auditActions.add(a));
        return MboAudit.VoidResponse.newBuilder().build();
    }

    @Override
    public MboAudit.VoidResponse writeActionsWithServiceFields(MboAudit.WriteActionsRequest writeActionsRequest) {
        long eventId = ++idGenerator;
        long currentDatetime = timestampGenerator = timestampGenerator + 100;

        writeActionsRequest.getActionsList().stream()
            .map(action -> {
                MboAudit.MboAction.Builder builder = MboAudit.MboAction.newBuilder(action);
                if (!builder.hasEventId()) {
                    builder.setEventId(eventId);
                }
                if (!builder.hasDate()) {
                    builder.setDate(currentDatetime);
                }
                if (!builder.hasActionId()) {
                    builder.setActionId(++idGenerator);
                }
                return builder.build();
            })
            .forEach(a -> auditActions.add(a));
        return MboAudit.VoidResponse.newBuilder().build();
    }

    @Override
    public MboAudit.FindPropertyNamesResponse findPropertyNames(MboAudit.FindPropertyNamesRequest request) {
        return MboAudit.FindPropertyNamesResponse.newBuilder()
            .addAllItems(auditActions.stream()
                .filter(action ->
                    action.getEntityType() == request.getEntityType()
                        && new Date(action.getDate()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        .isAfter(LocalDate.now().minusDays(request.getDateWindowSize() + 1)))
                .map(MboAudit.MboAction::getPropertyName)
                .distinct()
                .sorted()
                .map(propertyName ->
                    MboAudit.FindPropertyNamesResponse.Item.newBuilder()
                        .setPropertyName(propertyName)
                        .build())
                .collect(Collectors.toList())
            )
            .build();
    }

    @Override
    public MonitoringResult ping() {
        return null;
    }

    @Override
    public MonitoringResult monitoring() {
        return null;
    }

    private Stream<MboAudit.MboAction> getFilterStream(MboAudit.FindActionsRequest filter) {
        Stream<MboAudit.MboAction> stream = auditActions.stream();

        if (filter.getActionTypeCount() > 0) {
            stream = stream.filter(a -> filter.getActionTypeList().contains(a.getActionType()));
        }
        if (filter.hasCategoryId()) {
            stream = stream.filter(a -> Objects.equals(a.getCategoryId(), filter.getCategoryId()));
        }
        if (filter.hasEntityId()) {
            stream = stream.filter(a -> Objects.equals(a.getEntityId(), filter.getEntityId()));
        }
        if (filter.getEntityTypeCount() > 0) {
            stream = stream.filter(a -> filter.getEntityTypeList().contains(a.getEntityType()));
        }
        if (filter.hasEventId()) {
            stream = stream.filter(a -> Objects.equals(a.getEventId(), filter.getEventId()));
        }
        if (filter.hasFinishDate()) {
            stream = stream.filter(a -> getDate(a.getDate()) < filter.getFinishDate());
        }
        if (filter.hasStartDate()) {
            stream = stream.filter(a -> getDate(a.getDate()) >= filter.getStartDate());
        }
        if (filter.hasFinishTimestamp()) {
            stream = stream.filter(a -> getDateAndTime(a.getDate()) < filter.getFinishTimestamp());
        }
        if (filter.hasStartTimestamp()) {
            stream = stream.filter(a -> getDateAndTime(a.getDate()) >= filter.getStartTimestamp());
        }
        if (filter.hasParameterId()) {
            stream = stream.filter(a -> Objects.equals(a.getParameterId(), filter.getParameterId()));
        }
        if (filter.getPropertyNameCount() > 0) {
            stream = stream.filter(a -> filter.getPropertyNameList().contains(a.getPropertyName()));
        }
        if (filter.getUserIdCount() > 0 && filter.getStaffLoginCount() > 0) {
            stream = stream.filter(a ->
                filter.getUserIdList().contains(a.getUserId())
                    || filter.getStaffLoginList().contains(a.getStaffLogin()));
        } else if (filter.getUserIdCount() > 0) {
            stream = stream.filter(a -> filter.getUserIdList().contains(a.getUserId()));
        } else if (filter.getStaffLoginCount() > 0) {
            stream = stream.filter(a -> filter.getStaffLoginList().contains(a.getStaffLogin()));
        }

        stream = stream.skip(filter.getOffset());
        stream = stream.limit(filter.getLength());

        stream = stream.sorted(Comparator.comparing(MboAudit.MboAction::getDate).reversed()); // desc sort by timestamp

        return stream;
    }

    private long getDate(long timestamp) {
        return DateUtils.truncate(new Date(timestamp), java.util.Calendar.DAY_OF_MONTH).getTime();
    }

    private long getDateAndTime(long timestamp) {
        return timestamp;
    }
}
