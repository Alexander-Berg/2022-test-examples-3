package ru.yandex.market.delivery.transport_manager.service.health.product;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

class RegisterHealthControllerTest extends AbstractContextualTest {
    private static final String OK = "0;OK";

    @Autowired
    RegisterHealthChecker registerHealthChecker;

    @BeforeEach
    void setUp() {
        clock.setFixed(
            LocalDateTime.of(2020, 9, 8, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
    }

    @Test
    @DatabaseSetup("/repository/health/register/transportation_cnbm_register_sent.xml")
    void checkRegistersSent_registersSent_ok() {
        softly.assertThat(registerHealthChecker.checkOldRegistersSent()).isEqualTo(OK);
    }

    @Test
    @DatabaseSetup("/repository/health/register/transportation_cnbm_no_external_id.xml")
    void checkRegistersSent_noExternalId_error() {
        softly.assertThat(registerHealthChecker.checkOldRegistersSent()).isEqualTo(
            "2;" + String.format(RegisterHealthChecker.MESSAGE_NO_EXTERNAL_ID_DETECTED, 1, List.of(6))
        );
    }

    @Test
    @DatabaseSetup("/repository/health/register/transportation_cnbm_register_not_sent.xml")
    void checkRegistersSent_registerNotSent_error() {
        softly.assertThat(registerHealthChecker.checkOldRegistersSent()).isEqualTo(
            "2;" + String.format(RegisterHealthChecker.MESSAGE_NOT_FINAL_DETECTED, 1, List.of(6))
        );
    }

    @Test
    @DatabaseSetup("/repository/health/register/transportation_cnbm_2_failed_registers.xml")
    void checkRegistersSent_twoRegisters_registerNotSent_And_noExternalId_error() {
        softly.assertThat(registerHealthChecker.checkOldRegistersSent()).isEqualTo(
            "2;" + String.format(RegisterHealthChecker.MESSAGE_NOT_FINAL_DETECTED, 1, List.of(7)) +
                String.format(RegisterHealthChecker.MESSAGE_NO_EXTERNAL_ID_DETECTED, 1, List.of(6))
        );
    }

    @Test
    @DatabaseSetup("/repository/health/register/transportation_not_final_register_not_sent.xml")
    void checkRegistersSent_transportationNonFinal_registerNotSent_ok() {
        softly.assertThat(registerHealthChecker.checkOldRegistersSent()).isEqualTo(OK);
    }

    @Test
    void checkRegistersSent_noTransportations_ok() {
        softly.assertThat(registerHealthChecker.checkOldRegistersSent()).isEqualTo(OK);
    }

    @Test
    @DatabaseSetup("/repository/health/register/transportation_deleted_register_not_sent.xml")
    void checkRegistersSent_transportationDeleted_registerNotSent_ok() {
        softly.assertThat(registerHealthChecker.checkOldRegistersSent()).isEqualTo(OK);
    }

    @Test
    @DatabaseSetup("/repository/health/register/transportation_new_scheme_register_not_sent.xml")
    void checkRegistersSentNewSchemeIgnored() {
        softly.assertThat(registerHealthChecker.checkOldRegistersSent()).isEqualTo(OK);
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/health/register/transportation_cnbm_register_sent.xml",
            "/repository/health/register/outbound_register.xml"
        }
    )
    void checkOldSchemeOutboundIsNotChecked() {
        softly.assertThat(registerHealthChecker.checkOldRegistersSent()).isEqualTo(OK);
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/health/register/transportation_data.xml",
            "/repository/health/register/transportation_outbound_plan_sent.xml"
        })
    void checkOutboundPlanSentOk() {
        softly.assertThat(registerHealthChecker.checkNewSchemeOutboundPlanSent()).isEqualTo(OK);
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/health/register/transportation_data.xml",
            "/repository/health/register/transportation_outbound_plan_not_sent.xml"
        })
    void checkOutboundPlanNotSent() {
        softly.assertThat(registerHealthChecker.checkNewSchemeOutboundPlanSent()).isEqualTo(
            "2;" + String.format(RegisterHealthChecker.MESSAGE_NOT_FINAL_DETECTED, 1, List.of(6))
        );
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/health/register/transportation_data.xml",
            "/repository/health/register/transportation_outbound_plan_no_external_id.xml"
        })
    void checkOutboundPlanNoExternalId() {
        softly.assertThat(registerHealthChecker.checkNewSchemeOutboundPlanSent()).isEqualTo(
            "2;" + String.format(RegisterHealthChecker.MESSAGE_NO_EXTERNAL_ID_DETECTED, 1, List.of(6))
        );
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/health/register/transportation_data.xml",
            "/repository/health/register/transportation_inbound_plan_sent.xml"
        }
    )
    void checkInboundPlanSentOk() {
        softly.assertThat(registerHealthChecker.checkNewSchemeInboundPlanSent()).isEqualTo(OK);
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/health/register/transportation_data.xml",
            "/repository/health/register/transportation_inbound_plan_not_sent.xml"
        }
    )
    void checkInboundPlanNotSent() {
        softly.assertThat(registerHealthChecker.checkNewSchemeInboundPlanSent()).isEqualTo(
            "2;" + String.format(RegisterHealthChecker.MESSAGE_NOT_FINAL_DETECTED, 1, List.of(6))
        );
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/health/register/transportation_data.xml",
            "/repository/health/register/transportation_inbound_plan_not_sent_no_method.xml"
        }
    )
    void checkInboundPlanNotSentNoMethod() {
        softly.assertThat(registerHealthChecker.checkNewSchemeInboundPlanSent()).isEqualTo(OK);
    }

    @Test
    @DatabaseSetup("/repository/health/register/return_registers_not_sent.xml")
    void checkReturnRegistersNotSent() {
        softly.assertThat(registerHealthChecker.checkReturnRegistersSent())
            .startsWith("2;Some registers were not sent to ABO")
            .contains(
                "[TM id:1, Document id:doc-1]",
                "[TM id:2, Document id:doc-2]"
            )
            .doesNotContain("doc-3", "doc-4", "doc-5", "doc-6", "doc-7", "doc-8", "doc-9");
    }

    @Test
    @DatabaseSetup("/repository/health/register/register_documents_not_ready.xml")
    void checkDocumentsWereNotReady() {
        softly.assertThat(registerHealthChecker.checkDocumentsReady())
            .startsWith("2;Documents were not sent to s3")
            .contains(
                "TM id: 1"
            );
    }

    @Test
    @DatabaseSetup("/repository/health/register/register_documents_for_inbound.xml")
    void checkDocumentsOkInboundIgnored() {
        softly.assertThat(registerHealthChecker.checkDocumentsReady())
                .startsWith("0;OK");
    }
}
