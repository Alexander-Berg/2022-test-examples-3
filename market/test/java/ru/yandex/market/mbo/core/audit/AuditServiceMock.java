package ru.yandex.market.mbo.core.audit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditFilter;
import ru.yandex.market.mbo.http.MboAudit;

/**
 * @author s-ermakov
 */
public class AuditServiceMock extends AuditService {

    private final AtomicLong idGenerator = new AtomicLong();

    private final List<AuditAction> auditActions = new ArrayList<>();
    private final boolean emulateRealReading;

    public AuditServiceMock() {
        this(true);
    }

    public AuditServiceMock(boolean emulateRealReading) {
        super(null, null, "mock-environment");
        this.emulateRealReading = emulateRealReading;
    }

    public void clearActions() {
        auditActions.clear();
    }

    @Override
    public void writeActions(Collection<AuditAction> auditActions) {
        long eventId = idGenerator.incrementAndGet();
        auditActions.forEach(a -> {
            a.setEventId(eventId);
            if (a.getSource() == null) {
                a.setSource(AuditAction.Source.MBO);
            }
            if (a.getActionId() == null) {
                a.setActionId(idGenerator.incrementAndGet());
            }
            if (a.getDate() == null) {
                a.setDate(new Date());
            }
            //while reading real audit we receive empty strings instead of nulls
            if (emulateRealReading) {
                if (a.getOldValue() == null) {
                    a.setOldValue("");
                }
                if (a.getNewValue() == null) {
                    a.setNewValue("");
                }
            }
        });
        this.auditActions.addAll(auditActions);
    }

    @Override
    public void writeProtoActions(Collection<MboAudit.MboAction> actions) {
        writeActions(actions.stream()
            .map(protoAction -> AuditActionConverter.convert(protoAction))
            .collect(Collectors.toList())
        );
    }

    @Override
    public int getAuditCount(AuditFilter filter) {
        return (int) getFilterStream(filter).count();
    }

    @Override
    public List<AuditAction> loadAudit(int offset, int length, AuditFilter filter) {
        return loadAudit(offset, length, filter, true);
    }

    @Override
    public List<AuditAction> loadAudit(int offset, int length, AuditFilter filter, boolean criticalRead) {
        return getFilterStream(filter)
            .skip(offset)
            .limit(length)
            .collect(Collectors.toList());
    }

    private Stream<AuditAction> getFilterStream(AuditFilter filter) {
        Stream<AuditAction> stream = auditActions.stream();

        if (filter.getActionType() != null) {
            stream = stream.filter(a -> Objects.equals(a.getActionType(), filter.getActionType()));
        }
        if (filter.getCategoryId() != null) {
            stream = stream.filter(a -> Objects.equals(a.getCategoryId(), filter.getCategoryId()));
        }
        if (filter.getEntityId() != null) {
            stream = stream.filter(a -> Objects.equals(a.getEntityId(), filter.getEntityId()));
        }
        if (filter.getEntityType() != null) {
            stream = stream.filter(a -> Objects.equals(a.getEntityType(), filter.getEntityType()));
        }
        if (filter.getEventId() != null) {
            stream = stream.filter(a -> Objects.equals(a.getEventId(), filter.getEventId()));
        }
        if (filter.getFinishDate() != null) {
            stream = stream.filter(a -> a.getDate().getTime() < filter.getFinishDate().getTime());
        }
        if (filter.getStartDate() != null) {
            stream = stream.filter(a -> a.getDate().getTime() >= filter.getStartDate().getTime());
        }
        if (filter.getFinishTimestamp() != null) {
            stream = stream.filter(a -> a.getDate().getTime() < filter.getFinishTimestamp().getTime());
        }
        if (filter.getStartTimestamp() != null) {
            stream = stream.filter(a -> a.getDate().getTime() >= filter.getStartTimestamp().getTime());
        }
        if (filter.getParameterId() != null) {
            stream = stream.filter(a -> Objects.equals(a.getParameterId(), filter.getParameterId()));
        }
        if (filter.getPropertyName() != null) {
            stream = stream.filter(a -> Objects.equals(a.getPropertyName(), filter.getPropertyName()));
        }
        if (filter.getSource() != null) {
            stream = stream.filter(a -> Objects.equals(a.getSource(), filter.getSource()));
        }
        if (filter.getSourceId() != null) {
            stream = stream.filter(a -> Objects.equals(a.getSourceId(), filter.getSourceId()));
        }
        if (filter.getActionId() != null) {
            stream = stream.filter(a -> Objects.equals(a.getActionId(), filter.getActionId()));
        }
        //По логину и uid ищем с условием ИЛИ
        if (filter.getStaffLogin() != null && filter.getUserId() != null) {
            stream = stream.filter(a -> Objects.equals(a.getUserId(), filter.getUserId())
                || Objects.equals(a.getStaffLogin(), filter.getStaffLogin()));
        } else {
            if (filter.getUserId() != null) {
                stream = stream.filter(a -> Objects.equals(a.getUserId(), filter.getUserId()));
            }
            if (filter.getStaffLogin() != null) {
                stream = stream.filter(a -> Objects.equals(a.getStaffLogin(), filter.getStaffLogin()));
            }
        }
        stream = stream.sorted((o1, o2) -> o2.getDate().compareTo(o1.getDate())); // desc sort by timestamp

        return stream;
    }
}
