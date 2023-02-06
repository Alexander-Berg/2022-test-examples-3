package ru.yandex.direct.core.entity.freelancer.repository;

import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.Test;

import ru.yandex.direct.core.entity.freelancer.model.FreelancersCardDeclineReason;

import static org.assertj.core.api.Assertions.assertThat;

public class FreelancerCardMappingsTest {

    @Test
    public void declineReasonsToDb_success() {
        Set<FreelancersCardDeclineReason> freelancersCardDeclineReasons =
                StreamEx.of(FreelancersCardDeclineReason.values())
                        .toSet();
        String declineReasons = FreelancerCardMappings.declineReasonsToDb(freelancersCardDeclineReasons);
        String expected = "bad_description,bad_href,bad_image";
        assertThat(declineReasons).isEqualTo(expected);
    }

    @Test
    public void declineReasonsFromDb_success() {
        String declineReasons = "bad_description,bad_href,bad_image";
        Set<FreelancersCardDeclineReason> convertedReasons =
                FreelancerCardMappings.declineReasonsFromDb(declineReasons);
        List<FreelancersCardDeclineReason> allFreelancersCardDeclineReason =
                StreamEx.of(FreelancersCardDeclineReason.values()).toList();
        assertThat(convertedReasons).containsAll(allFreelancersCardDeclineReason);
    }
}
