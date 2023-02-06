package ru.yandex.chemodan.app.telemost.mock.mediator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.telemost.room.proto.MediatorOuterClass;
import ru.yandex.chemodan.app.telemost.room.proto.RoomGrpc;

public class RoomGrpcMockService extends RoomGrpc.RoomImplBase {

    private ListF<MediatorOuterClass.SendAppMessageRequest> sendAppRequests = Cf.arrayList();

    private final Map<String, List<MediatorOuterClass.Participant>> rooms = new ConcurrentHashMap<>();

    private final Set<String> peerIdsToFailAddingOnce = ConcurrentHashMap.newKeySet();

    @Override
    public void createRoom(MediatorOuterClass.CreateRoomRequest request,
            StreamObserver<MediatorOuterClass.CreateRoomResponse> responseObserver)
    {
        List<MediatorOuterClass.Participant> roomParticipants = rooms.get(request.getRoomId());
        if (roomParticipants != null) {
            responseObserver.onError(new StatusRuntimeException(Status.ALREADY_EXISTS));
            return;
        }
        rooms.put(request.getRoomId(), new CopyOnWriteArrayList<>());
        responseObserver.onNext(MediatorOuterClass.CreateRoomResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void addParticipant(MediatorOuterClass.AddParticipantRequest request,
            StreamObserver<MediatorOuterClass.AddParticipantResponse> responseObserver)
    {
        List<MediatorOuterClass.Participant> roomParticipants = rooms.get(request.getRoomId());
        if (roomParticipants == null) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND));
            return;
        }
        String participantId = request.getParticipantId();
        if (peerIdsToFailAddingOnce.contains(participantId)) {
            peerIdsToFailAddingOnce.remove(participantId);
            throw new RuntimeException(String.format("Peer id '%s' is added to fail once", participantId));
        }
        roomParticipants.add(MediatorOuterClass.Participant.newBuilder()
                .setParticipantId(request.getParticipantId()).build());
        responseObserver.onNext(MediatorOuterClass.AddParticipantResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void sendAppMessage(MediatorOuterClass.SendAppMessageRequest request,
            StreamObserver<MediatorOuterClass.SendAppMessageResponse> responseObserver)
    {
        sendAppRequests.add(request);
        responseObserver.onNext(MediatorOuterClass.SendAppMessageResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void getParticipants(MediatorOuterClass.GetParticipantsRequest request,
            StreamObserver<MediatorOuterClass.GetParticipantsResponse> responseObserver) {
        List<MediatorOuterClass.Participant> participants = rooms.get(request.getRoomId());
        if (participants == null) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND));
            return;
        }
        MediatorOuterClass.GetParticipantsResponse.Builder responseBuilder =
                MediatorOuterClass.GetParticipantsResponse.newBuilder();
        participants.forEach(responseBuilder::addParticipants);
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void removeParticipant(MediatorOuterClass.RemoveParticipantRequest request,
            StreamObserver<MediatorOuterClass.RemoveParticipantResponse> responseObserver) {
        Optional<MediatorOuterClass.Participant> participantToRemove = Optional.ofNullable(rooms.get(request.getRoomId()))
                .orElseGet(Collections::emptyList).stream()
                .filter(participant -> participant.getParticipantId().equals(request.getParticipantId()))
                .findFirst();
        if (!participantToRemove.isPresent()) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND));
            return;
        }
        MediatorOuterClass.Participant participant = participantToRemove.get().toBuilder().setIsRemoved(true).build();
        List<MediatorOuterClass.Participant> participants = rooms.get(request.getRoomId());
        participants.remove(participantToRemove.get());
        participants.add(participant);
        responseObserver.onNext(MediatorOuterClass.RemoveParticipantResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    public void cleanUp() {
        sendAppRequests.clear();
        rooms.clear();
    }

    public ListF<MediatorOuterClass.SendAppMessageRequest> getSendAppRequests() {
        return sendAppRequests;
    }

    public void makePeerIdFailToAddOnce(String peerId) {
        peerIdsToFailAddingOnce.add(peerId);
    }
}
