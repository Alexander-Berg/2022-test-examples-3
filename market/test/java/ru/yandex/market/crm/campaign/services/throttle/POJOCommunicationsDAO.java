package ru.yandex.market.crm.campaign.services.throttle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zloddey
 */
public class POJOCommunicationsDAO implements CommunicationsDAO {
    private final Map<ContactAttempt, List<Communication>> communications = new HashMap<>();

    @Override
    public void addMany(Map<ContactAttempt, Communication> communicationsToSave) {
        if (communicationsToSave.isEmpty()) {
            throw new IllegalArgumentException("Empty communications list is not allowed");
        }
        communicationsToSave.forEach(this::addInner);
    }

    private void addInner(ContactAttempt contactAttempt, Communication communication) {
        List<Communication> saved = communications.getOrDefault(contactAttempt, new ArrayList<>());
        saved.add(communication);
        communications.put(contactAttempt, saved);
    }

    @Override
    public Map<String, List<Communication>> get(Collection<ContactAttempt> contactAttempts) {
        if (contactAttempts.isEmpty()) {
            throw new IllegalArgumentException("Empty communications list is not allowed");
        }
        Map<String, List<Communication>> result = new HashMap<>();
        for (ContactAttempt c : contactAttempts) {
            result.put(c.getId(), communications.getOrDefault(c, new ArrayList<>()));
        }
        return result;
    }

    public boolean hasCommunication(ContactAttempt contactAttempt, String type, String label) {
        return communications.entrySet().stream()
                .filter(e -> e.getKey().equals(contactAttempt))
                .flatMap(e -> e.getValue().stream())
                .filter(c -> c.getType().equals(type))
                .anyMatch(c -> c.getLabel().equals(label));
    }

    public boolean hasNoCommunication(ContactAttempt contactAttempt, String type, String label) {
        return communications.entrySet().stream()
                .filter(e -> e.getKey().equals(contactAttempt))
                .flatMap(e -> e.getValue().stream())
                .filter(c -> c.getType().equals(type))
                .noneMatch(c -> c.getLabel().equals(label));
    }
}
