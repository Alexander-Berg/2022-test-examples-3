package ru.yandex.market.markup2.utils.traits;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.yandex.market.markup2.utils.users.UsersService;
import ru.yandex.market.mbo.users.MboUsers;
import ru.yandex.market.toloka.TolokaApi;
import ru.yandex.market.toloka.TolokaApiStub;
import ru.yandex.market.toloka.model.Skill;
import ru.yandex.market.toloka.model.UserSkill;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author york
 * @since 20.01.2020
 */
public class TraitsServiceTest {
    private static final String WORKER1_ID = "worker1";
    private static final String WORKER2_ID = "worker2";
    private static final int WORKER1_UID = 1;
    private static final int WORKER2_UID = 2;
    private static final ImmutableMap<String, Integer> MAPPING = ImmutableMap.<String, Integer>builder()
        .put(WORKER1_ID, WORKER1_UID)
        .put(WORKER2_ID, WORKER2_UID)
        .build();

    private static final int CATEGORY1_ID = 1;
    private static final int CATEGORY2_ID = 2;
    private static final int CATEGORY3_ID = 3;

    private static final MboUsers.ProjectType PROJECT_TYPE = MboUsers.ProjectType.MAPPING_MODERATION;

    private TraitsAndSkillsService traitsAndSkillsService;

    private TolokaApi tolokaApi;

    private UsersService usersService;

    private Multimap<String, Integer> operatorCategoriesByUID;

    private Multimap<String, Integer> superoperatorCategoriesByUID;

    @Before
    public void setUp() {
        tolokaApi = Mockito.spy(new TolokaApiStub(null));
        usersService = Mockito.mock(UsersService.class);
        traitsAndSkillsService = new TraitsAndSkillsService();
        traitsAndSkillsService.setTolokaApi(tolokaApi);
        traitsAndSkillsService.setUsersService(usersService);
        operatorCategoriesByUID = ArrayListMultimap.create();
        superoperatorCategoriesByUID = ArrayListMultimap.create();

        when(usersService.getAllUsersMap()).thenAnswer(invocation -> {
            Map<String, MboUsers.MboUser.Builder> userMap = Stream.concat(
                operatorCategoriesByUID.keySet().stream(),
                superoperatorCategoriesByUID.keySet().stream()
            ).collect(Collectors.toMap(w -> w,
                w -> MboUsers.MboUser.newBuilder().setUid(MAPPING.get(w)), (a, b) -> a));

            operatorCategoriesByUID.asMap().forEach((u, c) -> {
                userMap.get(u).addAllCategoryRoles(
                        c.stream().map(this::toOperatorBinding).collect(Collectors.toSet()));

            });
            superoperatorCategoriesByUID.asMap().forEach((u, c) -> {
                userMap.get(u).addAllCategoryRoles(
                        c.stream().map(this::toSuperOperatorBinding).collect(Collectors.toSet()));
            });

            Map<String, MboUsers.MboUser> result = new HashMap<>();
            userMap.forEach((w, u) -> result.put(w, u.build()));
            return result;
        });
    }

    @Test
    public void testInitBeforeUpdating() {
        traitsAndSkillsService.doUpdateTraitsAndSkills();
        verify(tolokaApi).getAllTraits();
        verify(tolokaApi).getOrCreateSkillByName(eq("operator_skill"));
        verify(tolokaApi).getOrCreateSkillByName(eq("superoperator_skill"));
    }

    @Test
    public void testForCurrentSkills() {
        Skill operator = tolokaApi.getOrCreateSkillByName("operator_skill");
        Skill superoperator = tolokaApi.getOrCreateSkillByName("superoperator_skill");
        UserSkill skill = tolokaApi.addUserSkill(WORKER1_ID, operator.getId(), 1d);
        superoperatorCategoriesByUID.put(WORKER1_ID, CATEGORY1_ID);
        traitsAndSkillsService.doUpdateTraitsAndSkills();
        verify(tolokaApi).addTrait(eq(sup(CATEGORY1_ID)));
        verify(tolokaApi).addUserSkill(eq(WORKER1_ID), eq(superoperator.getId()), eq(1d));
        verify(tolokaApi).deleteUserSkill(eq(skill));
    }

    @Test
    public void notDeletingOtherSkill() {
        Skill operator = tolokaApi.getOrCreateSkillByName("operator_skill");
        Skill other = tolokaApi.getOrCreateSkillByName("other_skill");
        UserSkill otherSkill = tolokaApi.addUserSkill(WORKER1_ID, other.getId(), 1d);
        operatorCategoriesByUID.put(WORKER1_ID, CATEGORY1_ID);
        traitsAndSkillsService.doUpdateTraitsAndSkills();
        verify(tolokaApi).addUserSkill(eq(WORKER1_ID), eq(operator.getId()), eq(1d));
        verify(tolokaApi, never()).deleteUserSkill(any());
    }

    @Test
    public void testForCurrentTraits() {
        tolokaApi.modifyUserTraits(WORKER1_ID, Arrays.asList(op(CATEGORY1_ID)), Collections.emptyList());
        operatorCategoriesByUID.putAll(WORKER1_ID, Arrays.asList(CATEGORY2_ID, CATEGORY3_ID));
        traitsAndSkillsService.doUpdateTraitsAndSkills();
        assertAddedTraits(true, CATEGORY2_ID, CATEGORY3_ID);
        verify(tolokaApi).modifyUserTraits(eq(WORKER1_ID),
            eq(new HashSet<>(Arrays.asList(op(CATEGORY2_ID), op(CATEGORY3_ID)))),
            eq(Collections.singleton(op(CATEGORY1_ID))));
    }

    private void assertAddedTraits(boolean operator, Integer... categoryIds) {
        Function<Integer, String> mapper = operator ? this::op : this::sup;
        ArgumentCaptor<String> traitCaptor = ArgumentCaptor.forClass(String.class);
        verify(tolokaApi, times(categoryIds.length)).addTrait(traitCaptor.capture());
        assertThat(traitCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(
            Arrays.stream(categoryIds).map(mapper).collect(Collectors.toList()));
    }

    @Test
    public void testUserCategoriesComplex() {
        operatorCategoriesByUID.putAll(WORKER1_ID, Arrays.asList(CATEGORY1_ID, CATEGORY2_ID));
        traitsAndSkillsService.doUpdateTraitsAndSkills();
        Skill operator = tolokaApi.getOrCreateSkillByName("operator_skill");
        Skill superoperator = tolokaApi.getOrCreateSkillByName("superoperator_skill");
        verify(tolokaApi).addUserSkill(eq(WORKER1_ID), eq(operator.getId()), eq(1d));
        verify(tolokaApi, times(0)).addUserSkill(eq(WORKER1_ID), eq(superoperator.getId()), eq(1d));
        verify(tolokaApi).addTrait(eq(op(CATEGORY1_ID)));
        verify(tolokaApi).addTrait(eq(op(CATEGORY2_ID)));
        verify(tolokaApi).modifyUserTraits(eq(WORKER1_ID),
            eq(new HashSet<>(Arrays.asList(op(CATEGORY1_ID), op(CATEGORY2_ID)))), eq(Collections.emptySet()));

        operatorCategoriesByUID.remove(WORKER1_ID, CATEGORY1_ID);
        operatorCategoriesByUID.put(WORKER1_ID, CATEGORY3_ID);
        superoperatorCategoriesByUID.put(WORKER1_ID, CATEGORY3_ID);

        traitsAndSkillsService.doUpdateTraitsAndSkills();
        verify(tolokaApi).addUserSkill(eq(WORKER1_ID), eq(superoperator.getId()), eq(1d));

        verify(tolokaApi).modifyUserTraits(eq(WORKER1_ID),
            eq(new HashSet<>(Arrays.asList(op(CATEGORY3_ID), sup(CATEGORY3_ID)))),
            eq(Collections.singleton(op(CATEGORY1_ID))));

        UserSkill superSkill = tolokaApi.getUserSkills(WORKER1_ID).stream()
            .filter(uc -> uc.getSkillId().equals(superoperator.getId()))
            .findFirst().get();
        superoperatorCategoriesByUID.removeAll(WORKER1_ID);

        traitsAndSkillsService.doUpdateTraitsAndSkills();
        verify(tolokaApi).deleteUserSkill(eq(superSkill));
    }

    @Test
    public void testComputeCurrentTraitsOperator() {
        MboUsers.MboUser.Builder builder = MboUsers.MboUser.newBuilder()
                .addCategoryRoles(
                        MboUsers.MboUserBinding.newBuilder()
                                .setCategoryId(CATEGORY1_ID)
                                .setMboRole(MboUsers.MboRole.OPERATOR)
                                .addAllProjects(Arrays.asList(
                                        MboUsers.ProjectType.values()
                                ))
                )
                .addCategoryRoles(
                        MboUsers.MboUserBinding.newBuilder()
                                .setCategoryId(CATEGORY2_ID)
                                .setMboRole(MboUsers.MboRole.OPERATOR)
                                .addAllProjects(Arrays.asList(
                                        MboUsers.ProjectType.BLUE_LOGS,
                                        MboUsers.ProjectType.MBO,
                                        MboUsers.ProjectType.MAPPING_MODERATION
                                ))
                )
                .addCategoryRoles(
                        MboUsers.MboUserBinding.newBuilder()
                                .setCategoryId(CATEGORY3_ID)
                                .setMboRole(MboUsers.MboRole.OPERATOR)
                                .addAllProjects(Arrays.asList(
                                        MboUsers.ProjectType.DEEPMATCHER_LOGS,
                                        MboUsers.ProjectType.WHITE_LOGS
                                ))
                );
        traitsAndSkillsService.initTraitsAndSkills();
        Set<String> traits = traitsAndSkillsService.calculateUserTraits(builder.build());

        assertThat(traits).containsExactlyInAnyOrder(
                "operator_1", // see TraitsAndSkillsService.addDistinctModeration
                "operator_1_all", "operator_2_BLUE_LOGS", "operator_2", "operator_3_WHITE_LOGS"
        );
    }

    @Test
    public void testComputeCurrentTraitsSuper() {
        MboUsers.MboUser.Builder builder = MboUsers.MboUser.newBuilder()
                .addCategoryRoles(
                        MboUsers.MboUserBinding.newBuilder()
                                .setCategoryId(CATEGORY1_ID)
                                .setMboRole(MboUsers.MboRole.SUPER)
                                .addAllProjects(Arrays.asList(
                                        MboUsers.ProjectType.values()
                                )))
                .addCategoryRoles(
                        MboUsers.MboUserBinding.newBuilder()
                                .setCategoryId(CATEGORY2_ID)
                                .setMboRole(MboUsers.MboRole.SUPER)
                                .addAllProjects(Arrays.asList(
                                        MboUsers.ProjectType.BLUE_LOGS,
                                        MboUsers.ProjectType.MBO,
                                        MboUsers.ProjectType.MAPPING_MODERATION
                                )))
                .addCategoryRoles(
                        MboUsers.MboUserBinding.newBuilder()
                                .setCategoryId(CATEGORY3_ID)
                                .setMboRole(MboUsers.MboRole.SUPER)
                                .addAllProjects(Arrays.asList(
                                        MboUsers.ProjectType.DEEPMATCHER_LOGS,
                                        MboUsers.ProjectType.WHITE_LOGS
                                ))
                );
        traitsAndSkillsService.initTraitsAndSkills();
        Set<String> traits = traitsAndSkillsService.calculateUserTraits(builder.build());

        assertThat(traits).containsExactlyInAnyOrder(
                "superoperator_1_all", "superoperator_2_BLUE_LOGS", "superoperator_2", "superoperator_3_WHITE_LOGS"
        );
    }

    @Test
    public void testComputeCurrentTraitsContentManager() {
        MboUsers.MboUser.Builder builder = MboUsers.MboUser.newBuilder()
                .addContentManagerCategories(CATEGORY1_ID)
                .addContentManagerCategories(CATEGORY2_ID)
                .addCategoryRoles(
                        MboUsers.MboUserBinding.newBuilder()
                                .setCategoryId(CATEGORY1_ID)
                                .setMboRole(MboUsers.MboRole.SUPER)
                                .addAllProjects(Arrays.asList(
                                        MboUsers.ProjectType.BLUE_LOGS,
                                        MboUsers.ProjectType.WHITE_LOGS
                                ))
                );
        traitsAndSkillsService.initTraitsAndSkills();
        Set<String> traits = traitsAndSkillsService.calculateUserTraits(builder.build());

        assertThat(traits).containsExactlyInAnyOrder(
                "superoperator_1_all", "superoperator_2_all"
        );
    }

    @Test
    public void testComputeCurrentTraitsOtherRolesSkipped() {
        traitsAndSkillsService.initTraitsAndSkills();
        for (MboUsers.MboRole role: MboUsers.MboRole.values()) {
            if (!TraitsAndSkillsService.isOperator(role) && !TraitsAndSkillsService.isSuperOperator(role)) {
                MboUsers.MboUser.Builder builder = MboUsers.MboUser.newBuilder()
                        .addCategoryRoles(
                                MboUsers.MboUserBinding.newBuilder()
                                        .setCategoryId(CATEGORY1_ID)
                                        .setMboRole(role)
                                        .addAllProjects(Arrays.asList(
                                                MboUsers.ProjectType.BLUE_LOGS,
                                                MboUsers.ProjectType.WHITE_LOGS
                                        ))
                        );
                assertThat(traitsAndSkillsService.calculateUserTraits(builder.build())).isEmpty();
            }
        }
    }

    @Test
    public void testComputeCurrentSkills() {
        MboUsers.MboUser.Builder builder = MboUsers.MboUser.newBuilder();
        builder.addCategoryRoles(
                MboUsers.MboUserBinding.newBuilder()
                        .setCategoryId(CATEGORY1_ID)
                        .setMboRole(MboUsers.MboRole.ADMIN)
                        .addAllProjects(Arrays.asList(MboUsers.ProjectType.values()))
        );
        builder.addCategoryRoles(
                MboUsers.MboUserBinding.newBuilder()
                        .setCategoryId(CATEGORY2_ID)
                        .setMboRole(MboUsers.MboRole.OPERATOR)
                        .addAllProjects(Arrays.asList(
                                MboUsers.ProjectType.BLUE_LOGS,
                                MboUsers.ProjectType.MBO,
                                MboUsers.ProjectType.MAPPING_MODERATION
                        ))
        );
        builder.addCategoryRoles(
                MboUsers.MboUserBinding.newBuilder()
                        .setCategoryId(CATEGORY3_ID)
                        .setMboRole(MboUsers.MboRole.SUPER)
                        .addAllProjects(Arrays.asList(
                                MboUsers.ProjectType.DEEPMATCHER_LOGS,
                                MboUsers.ProjectType.WHITE_LOGS
                        ))
        );
        traitsAndSkillsService.initTraitsAndSkills();
        Set<String> skills = traitsAndSkillsService.calculateUserSkills(builder.build());
        assertThat(skills).containsExactlyInAnyOrderElementsOf(
                toSkills("operator_skill", "operator_skill_BLUE_LOGS", "superoperator_skill_WHITE_LOGS")
        );
    }

    @Test
    public void testComputeCurrentSkillsOtherRolesSkipped() {
        traitsAndSkillsService.initTraitsAndSkills();
        for (MboUsers.MboRole role: MboUsers.MboRole.values()) {
            if (!TraitsAndSkillsService.isOperator(role) && !TraitsAndSkillsService.isSuperOperator(role)) {
                MboUsers.MboUser.Builder builder = MboUsers.MboUser.newBuilder()
                        .addCategoryRoles(
                                MboUsers.MboUserBinding.newBuilder()
                                        .setCategoryId(CATEGORY1_ID)
                                        .setMboRole(role)
                                        .addAllProjects(Arrays.asList(
                                                MboUsers.ProjectType.BLUE_LOGS,
                                                MboUsers.ProjectType.WHITE_LOGS
                                        ))
                        );
                assertThat(traitsAndSkillsService.calculateUserSkills(builder.build())).isEmpty();
            }
        }
    }

    private Collection<String> toSkills(String... names) {
        return Arrays.stream(names)
                .map(tolokaApi::getOrCreateSkillByName)
                .map(s -> s.getId())
                .collect(Collectors.toList());
    }

    private MboUsers.MboUserBinding toOperatorBinding(Integer catId) {
        return toUserBinding(catId, MboUsers.MboRole.OPERATOR);
    }

    private MboUsers.MboUserBinding toSuperOperatorBinding(Integer catId) {
        return toUserBinding(catId, MboUsers.MboRole.SUPER);
    }

    private MboUsers.MboUserBinding toUserBinding(Integer catId, MboUsers.MboRole role) {
        return MboUsers.MboUserBinding.newBuilder()
                .setCategoryId(catId)
                .setMboRole(role)
                .addProjects(PROJECT_TYPE)
                .build();
    }

    private String op(int categoryId) {
        return TraitsAndSkillsService.OPERATOR_PREFIX + categoryId;
    }

    private String sup(int categoryId) {
        return TraitsAndSkillsService.SUPER_PREFIX + categoryId;
    }
}
