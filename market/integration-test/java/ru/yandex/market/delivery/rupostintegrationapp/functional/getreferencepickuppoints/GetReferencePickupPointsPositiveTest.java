package ru.yandex.market.delivery.rupostintegrationapp.functional.getreferencepickuppoints;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import steps.pickuppointsteps.PickupPointSteps;
import utils.FixtureRepository;

import ru.yandex.market.delivery.rupostintegrationapp.BaseContextualTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.pickuppoint.PickuppointRepository;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GetReferencePickupPointsPositiveTest extends BaseContextualTest {
    // TODO: 31/03/17 выпилить, когда запилим тестовую БД
    @MockBean
    private PickuppointRepository repository;

    @Test
    void testService() throws Exception {
        //@todo это функциональный тест, как появится БД в тестах - убрать мок и дописать добавление пикапоинтов в бд
        when(repository.findPickupPoints("bname", "fname", "fvalue", null, Collections.emptySet()))
            .thenReturn(PickupPointSteps.getRussianPostPickupPoints());

        mockMvc.perform(post(
            "/ds/getReferencePickupPoints?boolean_flag=bname&enum_flag_name=fname&enum_flag_value=fvalue"
        )
            .contentType(MediaType.APPLICATION_XML)
            .content(FixtureRepository.getReferencePickupPointsEmptyRequest()))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString("type=\"getReferencePickupPoints\"")))
            .andExpect(content().string(Matchers.containsString("<isError>false</isError>")))
            .andExpect(content().string(Matchers.containsString("<pickupPoint><code>1</code>")))
            .andExpect(content().string(Matchers.containsString("<pickupPoint><code>2</code>")))
            .andExpect(content().string(Matchers.containsString("<pickupPoint><code>3</code>")));
    }

    @Test
    void testServiceWithEmptyCodes() throws Exception {
        when(repository.findPickupPoints("bname", "fname", "fvalue", null, Collections.emptySet()))
            .thenReturn(PickupPointSteps.getRussianPostPickupPoints());

        mockMvc.perform(post(
            "/ds/getReferencePickupPoints?boolean_flag=bname&enum_flag_name=fname&enum_flag_value=fvalue&codes="
        )
            .contentType(MediaType.APPLICATION_XML)
            .content(FixtureRepository.getReferencePickupPointsEmptyRequest()))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString("type=\"getReferencePickupPoints\"")))
            .andExpect(content().string(Matchers.containsString("<isError>false</isError>")))
            .andExpect(content().string(Matchers.containsString("<pickupPoint><code>1</code>")))
            .andExpect(content().string(Matchers.containsString("<pickupPoint><code>2</code>")))
            .andExpect(content().string(Matchers.containsString("<pickupPoint><code>3</code>")));
    }

    @Test
    void testServiceWithCode() throws Exception {

        String mockIndex = "1";
        when(repository.findPickupPoints(
            "bname",
            "fname",
            "fvalue",
            new HashSet<>(Collections.singletonList(mockIndex)),
            Collections.emptySet()
        ))
            .thenReturn(Collections.singletonList(PickupPointSteps.getRussianPostPickupPoint(mockIndex)));

        mockMvc.perform(post(
            "/ds/getReferencePickupPoints?boolean_flag=bname&enum_flag_name=fname&enum_flag_value=fvalue&codes=1"
        )
            .contentType(MediaType.APPLICATION_XML)
            .content(FixtureRepository.getReferencePickupPointsEmptyRequest()))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString("type=\"getReferencePickupPoints\"")))
            .andExpect(content().string(Matchers.containsString("<isError>false</isError>")))
            .andExpect(content().string(Matchers.containsString("<pickupPoint><code>1</code>")));
    }

    @Test
    void testServiceWithManyCodes() throws Exception {

        when(repository.findPickupPoints(
            "bname",
            "fname",
            "fvalue",
            Set.of("1", "2"),
            Collections.emptySet()
        ))
            .thenReturn(Arrays.asList(
                PickupPointSteps.getRussianPostPickupPoint("1"),
                PickupPointSteps.getRussianPostPickupPoint("2")
            ));

        mockMvc.perform(post(
            "/ds/getReferencePickupPoints?boolean_flag=bname&enum_flag_name=fname&enum_flag_value=fvalue&codes=1,2"
        )
            .contentType(MediaType.APPLICATION_XML)
            .content(FixtureRepository.getReferencePickupPointsEmptyRequest()))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString("type=\"getReferencePickupPoints\"")))
            .andExpect(content().string(Matchers.containsString("<isError>false</isError>")))
            .andExpect(content().string(Matchers.containsString("<pickupPoint><code>1</code>")))
            .andExpect(content().string(Matchers.containsString("<pickupPoint><code>2</code>")));
    }

    @Test
    void testServiceWithLocations() throws Exception {
        when(repository.findPickupPoints("bname", "fname", "fvalue", null, PickupPointSteps.makeLocationsSet()))
            .thenReturn(PickupPointSteps.getRussianPostPickupPoints());

        mockMvc.perform(post(
            "/ds/getReferencePickupPoints?boolean_flag=bname&enum_flag_name=fname&enum_flag_value=fvalue"
        )
            .contentType(MediaType.APPLICATION_XML)
            .content(FixtureRepository.getReferencePickupPointsRequestWithLocation()))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString("type=\"getReferencePickupPoints\"")))
            .andExpect(content().string(Matchers.containsString("<isError>false</isError>")))
            .andExpect(content().string(Matchers.containsString("<pickupPoint><code>1</code>")))
            .andExpect(content().string(Matchers.containsString("<pickupPoint><code>2</code>")))
            .andExpect(content().string(Matchers.containsString("<pickupPoint><code>3</code>")));
    }
}
