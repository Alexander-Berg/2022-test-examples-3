package ru.yandex.market.tpl.internal.controller.dropoff;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.client.dropoff.dto.DropoffCargoCreateCommandDto;
import ru.yandex.market.tpl.client.dropoff.dto.DropoffCargoDto;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.repository.DropoffCargoRepository;
import ru.yandex.market.tpl.internal.controller.BaseTplIntWebTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.core.util.TplCoreTestUtils.OBJECT_GENERATOR;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
class DropoffCargoControllerTest extends BaseTplIntWebTest {

    private final DropoffCargoRepository dropoffCargoRepository;
    private final ObjectMapper tplObjectMapper;

    @Test
    void createCargo_success_v1_for_return() throws Exception {
        var barcode = "1746598";
        String logisticPointIdFrom = "34";
        String logisticPointIdTo = "67";

        var expected = new DropoffCargoDto();
        expected.setBarcode(barcode);
        expected.setLogisticPointIdFrom(logisticPointIdFrom);
        expected.setLogisticPointIdTo(logisticPointIdTo);
        expected.setStatus("CREATED");

        mockMvc.perform(
                        put("/dropoff/cargo/" + barcode)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.format("{\"barcode\":%s, \"logisticPointIdFrom\":%s, " +
                                                "\"logisticPointIdTo\":%s}",
                                        barcode, logisticPointIdFrom, logisticPointIdTo))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(tplObjectMapper.writeValueAsString(expected)));

        Optional<DropoffCargo> resultOpt = dropoffCargoRepository.findByBarcodeAndReferenceIdIsNull(barcode);
        assertThat(resultOpt.isPresent()).isTrue();
        assertResult(expected, resultOpt.get());
    }

    @Test
    void createCargo_success_v2_for_return() throws Exception {
        var barcode = "1746598";

        var command = OBJECT_GENERATOR.nextObject(DropoffCargoCreateCommandDto.class);
        command.setBarcode(barcode);
        command.setReferenceId(null);
        command.setDeclaredCost(null);
        var expected = buildExpectedDto(command);

        mockMvc.perform(
                        put("/dropoff/cargo/" + barcode)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(tplObjectMapper.writeValueAsString(command))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(tplObjectMapper.writeValueAsString(expected)));

        Optional<DropoffCargo> resultOpt = dropoffCargoRepository.findByBarcodeAndReferenceIdIsNull(barcode);
        assertThat(resultOpt.isPresent()).isTrue();
        assertResult(expected, resultOpt.get());
    }

    @Test
    void createCargo_success_v2_for_direct() throws Exception {
        var barcode = "1746598";

        var command = OBJECT_GENERATOR.nextObject(DropoffCargoCreateCommandDto.class);
        command.setBarcode(barcode);
        var expected = buildExpectedDto(command);

        mockMvc.perform(
                        put("/dropoff/cargo/" + command)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(tplObjectMapper.writeValueAsString(command))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(tplObjectMapper.writeValueAsString(expected)));

        Optional<DropoffCargo> resultOpt = dropoffCargoRepository.findByBarcodeAndReferenceId(barcode,
                command.getReferenceId());
        assertThat(resultOpt.isPresent()).isTrue();
        assertResult(expected, resultOpt.get());
    }


    @Test
    void createCargo_success_when_notUniqBarcodesAndDifferentReferences() throws Exception {
        var barcode = "1746598";

        var command = OBJECT_GENERATOR.nextObject(DropoffCargoCreateCommandDto.class);
        command.setBarcode(barcode);
        command.setReferenceId(null);
        var expected1 = buildExpectedDto(command);
        mockMvc.perform(
                        put("/dropoff/cargo/" + command)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(tplObjectMapper.writeValueAsString(command))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(tplObjectMapper.writeValueAsString(expected1)));

        command = OBJECT_GENERATOR.nextObject(DropoffCargoCreateCommandDto.class);
        command.setBarcode(barcode);
        String referenceId2 = "ref1";
        command.setReferenceId(referenceId2);
        var expected2 = buildExpectedDto(command);
        mockMvc.perform(
                        put("/dropoff/cargo/" + command)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(tplObjectMapper.writeValueAsString(command))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(tplObjectMapper.writeValueAsString(expected2)));

        command = OBJECT_GENERATOR.nextObject(DropoffCargoCreateCommandDto.class);
        command.setBarcode(barcode);
        String referenceId3 = "ref2";
        command.setReferenceId(referenceId3);
        var expected3 = buildExpectedDto(command);
        mockMvc.perform(
                        put("/dropoff/cargo/" + command)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(tplObjectMapper.writeValueAsString(command))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(tplObjectMapper.writeValueAsString(expected3)));

        //then
        Optional<DropoffCargo> resultOpt = dropoffCargoRepository.findByBarcodeAndReferenceId(barcode,
                expected1.getReferenceId());
        assertThat(resultOpt.isPresent()).isTrue();
        assertResult(expected1, resultOpt.get());

        resultOpt = dropoffCargoRepository.findByBarcodeAndReferenceId(barcode, expected2.getReferenceId());
        assertThat(resultOpt.isPresent()).isTrue();
        assertResult(expected2, resultOpt.get());

        resultOpt = dropoffCargoRepository.findByBarcodeAndReferenceId(barcode, expected3.getReferenceId());
        assertThat(resultOpt.isPresent()).isTrue();
        assertResult(expected3, resultOpt.get());
    }


    @Test
    void createCargo_invalidRequest() throws Exception {
        var barcode = "1746598";

        mockMvc.perform(
                        put("/dropoff/cargo/")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("")
                )
                .andExpect(status().isNotFound());

        mockMvc.perform(
                        put("/dropoff/cargo/" + barcode)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"logisticPointIdFrom\": \"test\"}")
                )
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        put("/dropoff/cargo/" + barcode)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"logisticPointIdTo\": \"test\"}")
                )
                .andExpect(status().isBadRequest());
    }

    private DropoffCargoDto buildExpectedDto(DropoffCargoCreateCommandDto commandDto) {
        var expected = new DropoffCargoDto();
        expected.setBarcode(commandDto.getBarcode());
        expected.setLogisticPointIdFrom(commandDto.getLogisticPointIdFrom());
        expected.setLogisticPointIdTo(commandDto.getLogisticPointIdTo());
        expected.setStatus("CREATED");
        expected.setReferenceId(commandDto.getReferenceId());
        expected.setDeclaredCost(commandDto.getDeclaredCost());
        return expected;
    }

    private void assertResult(DropoffCargoDto expected, DropoffCargo result) {
        assertThat(result.getBarcode()).isEqualTo(expected.getBarcode());
        assertThat(result.getLogisticPointIdFrom()).isEqualTo(expected.getLogisticPointIdFrom());
        assertThat(result.getLogisticPointIdTo()).isEqualTo(expected.getLogisticPointIdTo());
        assertThat(result.getReferenceId()).isEqualTo(expected.getReferenceId());
        assertThat(result.getDeclaredCost()).isEqualTo(expected.getDeclaredCost());
    }
}
