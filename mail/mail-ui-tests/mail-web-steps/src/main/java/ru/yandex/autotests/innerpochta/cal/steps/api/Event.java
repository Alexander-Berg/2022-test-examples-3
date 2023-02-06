package ru.yandex.autotests.innerpochta.cal.steps.api;

import ru.yandex.autotests.innerpochta.steps.beans.event.Actions;
import ru.yandex.autotests.innerpochta.steps.beans.event.Attendees;
import ru.yandex.autotests.innerpochta.steps.beans.event.Eventreceived;
import ru.yandex.autotests.innerpochta.steps.beans.event.Notification;
import ru.yandex.autotests.innerpochta.steps.beans.event.Repetition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Event extends Eventreceived {

    private List<String> attendeesArray;

    public Event() {
        this.attendeesArray = new ArrayList<String>();
    }

    public List<String> getAttendeesArray() {
        return this.attendeesArray;
    }

    public void setAttendeesArray(List<String> attendees) {
        this.attendeesArray = attendees;
    }

    public Event withAttendeesArray(String... attendees) {
        this.attendeesArray = Arrays.asList(attendees);
        return this;
    }

    public Event withId(Long id) {
        this.setId(id);
        return this;
    }

    public Event withExternalId(String externalId) {
        this.setExternalId(externalId);
        return this;
    }

    public Event withSequence(Long sequence) {
        this.setSequence(sequence);
        return this;
    }

    public Event withType(String type) {
        this.setType(type);
        return this;
    }
    
    public Event withName(String name) {
        this.setName(name);
        return this;
    }

    public Event withDescription(String description) {
        this.setDescription(description);
        return this;
    }
    
    public Event withLocation(String location) {
        this.setLocation(location);
        return this;
    }

    public Event withDescriptionHtml(String descriptionHtml) {
        this.setDescriptionHtml(descriptionHtml);
        return this;
    }

    public Event withLocationHtml(String locationHtml) {
        this.setLocationHtml(locationHtml);
        return this;
    }

    public Event withStartTs(String startTs) {
        this.setStartTs(startTs);
        return this;
    }

    public Event withEndTs(String endTs) {
        this.setEndTs(endTs);
        return this;
    }

    public Event withInstanceStartTs(String instanceStartTs) {
        this.setInstanceStartTs(instanceStartTs);
        return this;
    }

    public Event withIsAllDay(Boolean isAllDay) {
        this.setIsAllDay(isAllDay); ;
        return this;
    }

    public Event withIsRecurrence(Boolean isRecurrence) {
        this.setIsRecurrence(isRecurrence); ;
        return this;
    }

    public Event withAttendees(Attendees attendees) {
        this.setAttendees(attendees);
        return this;
    }

    public Event withResources(List<Object> resources) {
        this.setResources(resources);
        return this;
    }

    public Event withSubscribers(List<Object> subscribers) {
        this.setSubscribers(subscribers);
        return this;
    }

    public Event withNotifications(List<Notification> notifications) {
        this.setNotifications(notifications);
        return this;
    }

    public Event withRepetition(Repetition repetition) {
        this.setRepetition(repetition);
        return this;
    }

    public Event withActions(Actions actions) {
        this.setActions(actions);
        return this;
    }

    public Event withParticipantsCanInvite(Boolean participantsCanInvite) {
        this.setParticipantsCanInvite(participantsCanInvite);
        return this;
    }

    public Event withParticipantsCanEdit(Boolean participantsCanEdit) {
        this.setParticipantsCanEdit(participantsCanEdit);
        return this;
    }

    public Event withOthersCanView(Boolean othersCanView) {
        this.setOthersCanView(othersCanView);
        return this;
    }

    public Event withDecision(String decision) {
        this.setDecision(decision);
        return this;
    }

    public Event withAvailability(String availability) {
        this.setAvailability(availability);
        return this;
    }

    public Event withLayerId(Long layerId) {
        this.setLayerId(layerId);
        return this;
    }

    public Event withIsOnPrimaryLayer(Boolean isOnPrimaryLayer) {
        this.setIsOnPrimaryLayer(isOnPrimaryLayer);
        return this;
    }

    public Event withPrimaryLayerClosed(Boolean primaryLayerClosed) {
        this.setPrimaryLayerClosed(primaryLayerClosed);
        return this;
    }

    public Event withOrganizerLetToEditAnyMeeting(Boolean organizerLetToEditAnyMeeting) {
        this.setOrganizerLetToEditAnyMeeting(organizerLetToEditAnyMeeting);
        return this;
    }

    public Event withCanAdminAllResources(Boolean canAdminAllResources) {
        this.setCanAdminAllResources(canAdminAllResources);
        return this;
    }

    public Event withRepetitionNeedsConfirmation(Boolean repetitionNeedsConfirmation) {
        this.setRepetitionNeedsConfirmation(repetitionNeedsConfirmation);
        return this;
    }
}
