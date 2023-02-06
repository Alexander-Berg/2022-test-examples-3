package ru.yandex.calendar.frontend.webNew;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import lombok.val;
import one.util.streamex.EntryStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.LayerInvitation;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class WebNewLayerManagerSmallTest {
    private static Stream<Arguments> provideArgumentsForConstructPermissionsMap() {
        val viewUid = new PassportUid(111L);
        val editUid = new PassportUid(222L);
        val ownerUid = new PassportUid(333L);
        val notInvitedUid = new PassportUid(444L);

        val viewInvitation = layerInvitation(viewUid, LayerActionClass.VIEW);
        val inconsistentInvitation = layerInvitation(viewUid, LayerActionClass.CREATE);
        val editInvitation = layerInvitation(editUid, LayerActionClass.EDIT);

        val ownerPerms = Map.entry(ownerUid, LayerActionClass.ADMIN);
        val viewPerms = Map.entry(viewUid, LayerActionClass.VIEW);
        val editPerms = Map.entry(editUid, LayerActionClass.EDIT);
        val notInvitedPerms = Map.entry(notInvitedUid, LayerActionClass.ACCESS);

        return Stream.of(
                Arguments.of("Simple consistent case", Map.ofEntries(viewPerms, ownerPerms), singletonList(viewInvitation), Map.ofEntries(viewPerms)),
                Arguments.of("Intentional inconsistency", Map.ofEntries(viewPerms, ownerPerms), singletonList(inconsistentInvitation), Map.ofEntries(viewPerms)),
                Arguments.of("Just owner", Map.ofEntries(ownerPerms), emptyList(), emptyMap()),
                Arguments.of("Owner and not invited", Map.ofEntries(notInvitedPerms), emptyList(), emptyMap()),
                Arguments.of("Not accepted invitation", Map.ofEntries(ownerPerms), singletonList(viewInvitation), Map.ofEntries(viewPerms)),
                Arguments.of("Complex case", Map.ofEntries(viewPerms, notInvitedPerms, ownerPerms), List.of(editInvitation, viewInvitation), Map.ofEntries(viewPerms, editPerms))
        );
    }

    private static Email uidToEmail(PassportUid uid) {
        return new Email(uid.getUid() + "@external.ru");
    }

    private static LayerInvitation layerInvitation(PassportUid uid, LayerActionClass layerActionClass) {
        val layerInvitation = new LayerInvitation();
        layerInvitation.setEmail(uidToEmail(uid));
        layerInvitation.setUid(uid);
        layerInvitation.setPerm(layerActionClass);
        return layerInvitation;
    }

    @ParameterizedTest(name = "{index}. {0}")
    @DisplayName("Prepare permissions for get-layer by layerUsers and layerInvitations.")
    @MethodSource("provideArgumentsForConstructPermissionsMap")
    public void constructPermissionsMap(@SuppressWarnings("unused") String comment,
                                        Map<PassportUid, LayerActionClass> permsByLayerUsers,
                                        List<LayerInvitation> invitations,
                                        Map<PassportUid, LayerActionClass> expectedResult) {
        val realPermMap = EntryStream.of(WebNewLayerManager.constructPermissionsMap(permsByLayerUsers, invitations))
                .mapKeys(ParticipantId::getEmailIfExternalUser)
                .flatMapKeys(Option::stream)
                .toImmutableMap();
        val expectedPermMap = EntryStream.of(expectedResult)
                .mapKeys(WebNewLayerManagerSmallTest::uidToEmail)
                .toImmutableMap();
        assertThat(realPermMap).isEqualTo(expectedPermMap);
    }
}
