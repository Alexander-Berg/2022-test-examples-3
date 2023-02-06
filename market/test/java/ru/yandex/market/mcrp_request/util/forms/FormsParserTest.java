package ru.yandex.market.mcrp_request.util.forms;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.mcrp_request.DAO.AvatarsResourcesOld;
import ru.yandex.market.mcrp_request.DAO.BareMetalResources;
import ru.yandex.market.mcrp_request.DAO.LogbrokerResources;
import ru.yandex.market.mcrp_request.DAO.MDBResources;
import ru.yandex.market.mcrp_request.DAO.MDSResources;
import ru.yandex.market.mcrp_request.DAO.NirvanaResources;
import ru.yandex.market.mcrp_request.DAO.QloudResources;
import ru.yandex.market.mcrp_request.DAO.RTCResources;
import ru.yandex.market.mcrp_request.DAO.Request;
import ru.yandex.market.mcrp_request.DAO.RequestResources;
import ru.yandex.market.mcrp_request.DAO.RequestResourcesData;
import ru.yandex.market.mcrp_request.DAO.S3ResourcesOld;
import ru.yandex.market.mcrp_request.DAO.SAASResources;
import ru.yandex.market.mcrp_request.DAO.SandboxResources;
import ru.yandex.market.mcrp_request.DAO.YDBResources;
import ru.yandex.market.mcrp_request.DAO.YPResources;
import ru.yandex.market.mcrp_request.DAO.YTResources;
import ru.yandex.market.mcrp_request.clients.AbcApiClient;
import ru.yandex.market.mcrp_request.config.McrpTestConfig;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {McrpTestConfig.class})
public class FormsParserTest {

    @Autowired
    public RTCFormsParser rtcForecasterParser;

    @Autowired
    public MDBFormsParser mdbFormsParser;

    @Autowired
    public MDSFormsParser mdsFormsParser;

    @Autowired
    public YTFormsParser ytFormsParser;

    @Autowired
    public YPFormsParser ypFormsParser;

    @Autowired
    public SandboxFormsParser sandboxFormsParser;

    @Autowired
    public SaaSFormsParser saasFormsParser;

    @Autowired
    public QloudFormsParser qloudFormsParser;

    @Autowired
    public LogbrokerFormsParser logbrokerFormsParser;

    @Autowired
    public NirvanaFormsParser nirvanaFormsParser;

    @Autowired
    public BareMetalFormsParser bareMetalFormsParser;

    @Autowired
    public YDBFormsParser ydbFormsParser;

    @Autowired
    public AbcApiClient abcApiClient;

    @Test
    public void postDataParser() {
        //test data
        MultiValueMap<String, String> formMap = new LinkedMultiValueMap<>();
        formMap.add("field_20", "{\"question\": {\"slug\": \"service_name_vla\", \"group_id\": 394511, \"id\": " +
                "395116, \"label\": {\"ru\": \"\\u041d\\u0430\\u0437\\u0432\\u0430\\u043d\\u0438\\u0435 " +
                "\\u0441\\u0435\\u0440\\u0432\\u0438\\u0441\\u043e\\u0432\"}, \"type\": {\"id\": 1, " +
                "\"is_allow_choices\": false, \"is_could_be_used_in_conditions\": true, \"is_read_only\": false, " +
                "\"slug\": \"answer_short_text\", \"name\": " +
                "\"\\u041a\\u043e\\u0440\\u043e\\u0442\\u043a\\u0438\\u0439 \\u043e\\u0442\\u0432\\u0435\\u0442\", " +
                "\"icon\": \"icon-italic\", \"kind\": \"generic\", \"admin_preview\": \"input\", \"allow_settings\": " +
                "[\"param_help_text\", \"param_hint_data_source\", \"param_initial\", \"param_is_hidden\", " +
                "\"param_is_required\", \"param_max\", \"param_min\", \"param_slug\"], \"required_settings\": [], " +
                "\"is_allow_widgets\": false, \"validator_types\": [{\"id\": 1, \"name\": " +
                "\"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}, {\"id\": 2, \"name\": " +
                "\"\\u0412\\u0430\\u043b\\u0438\\u0434\\u0430\\u0446\\u0438\\u044f " +
                "\\u0434\\u0440\\u043e\\u0431\\u043d\\u044b\\u0445 \\u0447\\u0438\\u0441\\u0435\\u043b\", \"slug\": " +
                "\"decimal\", \"is_external\": false}, {\"id\": 1003, \"name\": " +
                "\"\\u0412\\u0430\\u043b\\u0438\\u0434\\u0430\\u0446\\u0438\\u044f \\u0418\\u041d\\u041d\", \"slug\":" +
                " \"inn\", \"is_external\": false}, {\"id\": 1004, \"name\": " +
                "\"\\u0412\\u0430\\u043b\\u0438\\u0434\\u0430\\u0446\\u0438\\u044f " +
                "\\u0440\\u0443\\u0441\\u0441\\u043a\\u0438\\u0445 \\u0431\\u0443\\u043a\\u0432\", \"slug\": " +
                "\"russian\", \"is_external\": false}, {\"id\": 1037, \"name\": " +
                "\"\\u0412\\u0430\\u043b\\u0438\\u0434\\u0430\\u0446\\u0438\\u044f " +
                "\\u0447\\u0435\\u0440\\u0435\\u0437 \\u0440\\u0435\\u0433\\u0443\\u043b\\u044f\\u0440\\u043d\\u044b" +
                "\\u0435 \\u0432\\u044b\\u0440\\u0430\\u0436\\u0435\\u043d\\u0438\\u044f\", \"slug\": \"regexp\", " +
                "\"is_external\": false}]}}, \"value\": null}");
        formMap.add("field_3", "{\"question\": {\"slug\": \"cpu_sas\", \"group_id\": 394521, \"id\": 394517, " +
                "\"label\": {\"ru\": \"CPU\"}, \"type\": {\"id\": 31, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": true, \"is_read_only\": false, \"slug\": \"answer_number\", " +
                "\"name\": \"\\u041f\\u043e\\u043b\\u0435 \\u0432\\u0432\\u043e\\u0434\\u0430 " +
                "\\u0446\\u0438\\u0444\\u0440\", \"icon\": \"icon-italic\", \"kind\": \"generic\", \"admin_preview\":" +
                " \"payment\", \"allow_settings\": [\"param_help_text\", \"param_initial\", \"param_is_hidden\", " +
                "\"param_is_required\", \"param_max\", \"param_min\", \"param_slug\"], \"required_settings\": " +
                "[\"param_max\", \"param_min\"], \"is_allow_widgets\": false, \"validator_types\": [{\"id\": 1, " +
                "\"name\": \"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}}, \"value\": null}");
        formMap.add("field_21", "{\"question\": {\"slug\": \"answer_group_158246\", \"group_id\": null, \"id\": " +
                "394522, \"label\": {\"ru\": \"MAN\"}, \"type\": {\"id\": 1040, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": false, \"is_read_only\": true, \"slug\": \"answer_group\", " +
                "\"name\": \"\\u0413\\u0440\\u0443\\u043f\\u043f\\u0430 " +
                "\\u0432\\u043e\\u043f\\u0440\\u043e\\u0441\\u043e\\u0432\", \"icon\": \"icon-quote-left\", \"kind\":" +
                " \"generic\", \"admin_preview\": \"answer_group\", \"allow_settings\": [\"param_help_text\"], " +
                "\"required_settings\": [], \"is_allow_widgets\": false, \"validator_types\": []}}, \"value\": " +
                "\"MAN\\nCPU - 1\\nRAM - 1\\n\\nMAN\\nCPU - 3\\nRAM - 2\"}");
        formMap.add("field_4", "{\"question\": {\"slug\": \"cpu_man\", \"group_id\": 394522, \"id\": 394527, " +
                "\"label\": {\"ru\": \"CPU\"}, \"type\": {\"id\": 31, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": true, \"is_read_only\": false, \"slug\": \"answer_number\", " +
                "\"name\": \"\\u041f\\u043e\\u043b\\u0435 \\u0432\\u0432\\u043e\\u0434\\u0430 " +
                "\\u0446\\u0438\\u0444\\u0440\", \"icon\": \"icon-italic\", \"kind\": \"generic\", \"admin_preview\":" +
                " \"payment\", \"allow_settings\": [\"param_help_text\", \"param_initial\", \"param_is_hidden\", " +
                "\"param_is_required\", \"param_max\", \"param_min\", \"param_slug\"], \"required_settings\": " +
                "[\"param_max\", \"param_min\"], \"is_allow_widgets\": false, \"validator_types\": [{\"id\": 1, " +
                "\"name\": \"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}}, \"value\": {\"cpu_man__1\": \"3\", \"cpu_man__0\": \"1\"}}");
        formMap.add("field_22", "{\"question\": {\"slug\": \"reason\", \"group_id\": null, \"id\": 394506, \"label\":" +
                " {\"ru\": \"\\u041e\\u0431\\u043e\\u0441\\u043d\\u043e\\u0432\\u0430\\u043d\\u0438\\u0435 " +
                "\\u0437\\u0430\\u043a\\u0430\\u0437\\u0430\"}, \"type\": {\"id\": 2, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": true, \"is_read_only\": false, \"slug\": \"answer_long_text\", " +
                "\"name\": \"\\u0414\\u043b\\u0438\\u043d\\u043d\\u044b\\u0439 \\u043e\\u0442\\u0432\\u0435\\u0442\"," +
                " \"icon\": \"icon-text-height\", \"kind\": \"generic\", \"admin_preview\": \"textarea\", " +
                "\"allow_settings\": [\"param_help_text\", \"param_hint_data_source\", \"param_initial\", " +
                "\"param_is_hidden\", \"param_is_required\", \"param_max\", \"param_min\", \"param_slug\"], " +
                "\"required_settings\": [], \"is_allow_widgets\": false, \"validator_types\": [{\"id\": 1, \"name\": " +
                "\"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}}, \"value\": \"Test\"}");
        formMap.add("field_1", "{\"question\": {\"slug\": \"answer_statement_394509\", \"group_id\": null, \"id\": " +
                "394509, \"label\": {\"ru\": \"\\u0417\\u0430\\u043a\\u0430\\u0437 " +
                "\\u0440\\u0435\\u0441\\u0443\\u0440\\u0441\\u043e\\u0432 \\u0434\\u043e \\u043c\\u0430\\u044f 2022 " +
                "\\u0433\\u043e\\u0434\\u0430 \\u043f\\u043e\\u0434 \\u043d\\u043e\\u0432\\u044b\\u0435 " +
                "\\u0437\\u0430\\u043f\\u0443\\u0441\\u043a\\u0438 \\u0438 " +
                "\\u043f\\u0440\\u043e\\u0435\\u043a\\u0442\\u044b. \\u0420\\u0430\\u0441\\u0447\\u0435\\u0442\\u044b" +
                " \\u0438 \\u0437\\u0430\\u043a\\u0430\\u0437\\u044b \\u043f\\u043e " +
                "\\u043e\\u0440\\u0433\\u0430\\u043d\\u0438\\u0447\\u0435\\u0441\\u043a\\u043e\\u043c\\u0443 " +
                "\\u0440\\u043e\\u0441\\u0442\\u0443 \\u0441\\u043e\\u0431\\u0438\\u0440\\u0430\\u0435\\u043c " +
                "\\u043e\\u0442\\u0434\\u0435\\u043b\\u044c\\u043d\\u043e" +
                ".\\n\\u0412\\u043e\\u043f\\u0440\\u043e\\u0441\\u044b: https://t" +
                ".me/joinchat/AzJR3BIszvRZrHAdeZ_1Xg\"}, \"type\": {\"id\": 28, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": false, \"is_read_only\": true, \"slug\": \"answer_statement\", " +
                "\"name\": \"\\u0421\\u043e\\u043e\\u0431\\u0449\\u0435\\u043d\\u0438\\u0435\", \"icon\": " +
                "\"icon-quote-left\", \"kind\": \"generic\", \"admin_preview\": \"statement\", \"allow_settings\": " +
                "[\"param_help_text\", \"param_is_section_header\"], \"required_settings\": [], \"is_allow_widgets\":" +
                " false, \"validator_types\": []}}, \"value\": null}");
        formMap.add("field_23", "{\"question\": {\"slug\": \"resps\", \"group_id\": null, \"id\": 394507, \"type\": " +
                "{\"id\": 3, \"is_allow_choices\": true, \"is_could_be_used_in_conditions\": true, \"is_read_only\": " +
                "false, \"slug\": \"answer_choices\", \"name\": \"\\u0421\\u043f\\u0438\\u0441\\u043e\\u043a\", " +
                "\"icon\": \"icon-list-ul\", \"kind\": \"generic\", \"admin_preview\": \"list\", \"allow_settings\": " +
                "[\"param_data_source\", \"param_data_source_params\", \"param_help_text\", \"param_initial\", " +
                "\"param_is_allow_multiple_choice\", \"param_is_allow_other\", \"param_is_disabled_init_item\", " +
                "\"param_is_hidden\", \"param_is_lecture_labels_as_links\", \"param_is_random_choices_position\", " +
                "\"param_is_required\", \"param_modify_choices\", \"param_slug\", \"param_suggest_choices\"], " +
                "\"required_settings\": [], \"is_allow_widgets\": true, \"validator_types\": [{\"id\": 1, \"name\": " +
                "\"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}, \"choices\": {}, \"label\": {\"ru\": " +
                "\"\\u041e\\u0442\\u0432\\u0435\\u0442\\u0441\\u0442\\u0432\\u0435\\u043d\\u043d\\u044b\\u0435\"}}, " +
                "\"choice_id\": null, \"value\": \"Some Body (somebody)\"}");
        formMap.add("field_24", "{\"question\": {\"slug\": \"market_color\", \"group_id\": null, \"id\": 394529, " +
                "\"type\": {\"id\": 3, \"is_allow_choices\": true, \"is_could_be_used_in_conditions\": true, " +
                "\"is_read_only\": false, \"slug\": \"answer_choices\", \"name\": " +
                "\"\\u0421\\u043f\\u0438\\u0441\\u043e\\u043a\", \"icon\": \"icon-list-ul\", \"kind\": \"generic\", " +
                "\"admin_preview\": \"list\", \"allow_settings\": [\"param_data_source\", " +
                "\"param_data_source_params\", \"param_help_text\", \"param_initial\", " +
                "\"param_is_allow_multiple_choice\", \"param_is_allow_other\", \"param_is_disabled_init_item\", " +
                "\"param_is_hidden\", \"param_is_lecture_labels_as_links\", \"param_is_random_choices_position\", " +
                "\"param_is_required\", \"param_modify_choices\", \"param_slug\", \"param_suggest_choices\"], " +
                "\"required_settings\": [], \"is_allow_widgets\": true, \"validator_types\": [{\"id\": 1, \"name\": " +
                "\"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}, \"choices\": {\"\\u0411\\u0435\\u043b\\u044b\\u0439\": {\"id\": 567962, " +
                "\"survey_question_id\": 394529, \"position\": 1, \"slug\": \"263246\", \"is_hidden\": false, " +
                "\"label_image\": null, \"label\": \"\\u0411\\u0435\\u043b\\u044b\\u0439\"}, " +
                "\"\\u0413\\u043e\\u043b\\u0443\\u0431\\u043e\\u0439\": {\"id\": 569023, \"survey_question_id\": " +
                "394529, \"position\": 4, \"slug\": \"569023\", \"is_hidden\": false, \"label_image\": null, " +
                "\"label\": \"\\u0413\\u043e\\u043b\\u0443\\u0431\\u043e\\u0439\"}, " +
                "\"\\u0421\\u0438\\u043d\\u0438\\u0439 (beru)\": {\"id\": 567958, \"survey_question_id\": 394529, " +
                "\"position\": 2, \"slug\": \"263247\", \"is_hidden\": false, \"label_image\": null, \"label\": " +
                "\"\\u0421\\u0438\\u043d\\u0438\\u0439 (beru)\"}, " +
                "\"\\u0418\\u043d\\u0444\\u0440\\u0430\\u0441\\u0442\\u0440\\u0443\\u043a\\u0442\\u0443\\u0440\\u0430" +
                "\": {\"id\": 567948, \"survey_question_id\": 394529, \"position\": 3, \"slug\": \"263250\", " +
                "\"is_hidden\": false, \"label_image\": null, \"label\": " +
                "\"\\u0418\\u043d\\u0444\\u0440\\u0430\\u0441\\u0442\\u0440\\u0443\\u043a\\u0442\\u0443\\u0440\\u0430" +
                "\"}}, \"label\": {\"ru\": \"\\u0426\\u0432\\u0435\\u0442 " +
                "\\u043c\\u0430\\u0440\\u043a\\u0435\\u0442\\u0430\"}}, \"choice_id\": 567962, \"value\": " +
                "\"\\u0411\\u0435\\u043b\\u044b\\u0439\"}");
        formMap.add("field_2", "{\"question\": {\"slug\": \"cpu_vla\", \"group_id\": 394511, \"id\": 394513, " +
                "\"label\": {\"ru\": \"CPU\"}, \"type\": {\"id\": 31, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": true, \"is_read_only\": false, \"slug\": \"answer_number\", " +
                "\"name\": \"\\u041f\\u043e\\u043b\\u0435 \\u0432\\u0432\\u043e\\u0434\\u0430 " +
                "\\u0446\\u0438\\u0444\\u0440\", \"icon\": \"icon-italic\", \"kind\": \"generic\", \"admin_preview\":" +
                " \"payment\", \"allow_settings\": [\"param_help_text\", \"param_initial\", \"param_is_hidden\", " +
                "\"param_is_required\", \"param_max\", \"param_min\", \"param_slug\"], \"required_settings\": " +
                "[\"param_max\", \"param_min\"], \"is_allow_widgets\": false, \"validator_types\": [{\"id\": 1, " +
                "\"name\": \"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}}, \"value\": null}");
        formMap.add("field_25", "{\"question\": {\"slug\": \"answer_choices_158570\", \"group_id\": null, \"id\": " +
                "394530, \"type\": {\"id\": 3, \"is_allow_choices\": true, \"is_could_be_used_in_conditions\": true, " +
                "\"is_read_only\": false, \"slug\": \"answer_choices\", \"name\": " +
                "\"\\u0421\\u043f\\u0438\\u0441\\u043e\\u043a\", \"icon\": \"icon-list-ul\", \"kind\": \"generic\", " +
                "\"admin_preview\": \"list\", \"allow_settings\": [\"param_data_source\", " +
                "\"param_data_source_params\", \"param_help_text\", \"param_initial\", " +
                "\"param_is_allow_multiple_choice\", \"param_is_allow_other\", \"param_is_disabled_init_item\", " +
                "\"param_is_hidden\", \"param_is_lecture_labels_as_links\", \"param_is_random_choices_position\", " +
                "\"param_is_required\", \"param_modify_choices\", \"param_slug\", \"param_suggest_choices\"], " +
                "\"required_settings\": [], \"is_allow_widgets\": true, \"validator_types\": [{\"id\": 1, \"name\": " +
                "\"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}, \"choices\": " +
                "{\"\\u0420\\u0430\\u0432\\u043d\\u043e\\u043c\\u0435\\u0440\\u043d\\u043e \\u0432 " +
                "\\u0442\\u0435\\u0447\\u0435\\u043d\\u0438\\u0435 \\u0433\\u043e\\u0434\\u0430\": {\"id\": 567961, " +
                "\"survey_question_id\": 394530, \"position\": 1, \"slug\": \"263251\", \"is_hidden\": false, " +
                "\"label_image\": null, \"label\": \"\\u0420\\u0430\\u0432\\u043d\\u043e\\u043c\\u0435\\u0440\\u043d" +
                "\\u043e \\u0432 \\u0442\\u0435\\u0447\\u0435\\u043d\\u0438\\u0435 \\u0433\\u043e\\u0434\\u0430\"}, " +
                "\"\\u041c\\u0430\\u0439\": {\"id\": 567957, \"survey_question_id\": 394530, \"position\": 2, " +
                "\"slug\": \"263252\", \"is_hidden\": false, \"label_image\": null, \"label\": " +
                "\"\\u041c\\u0430\\u0439\"}, \"\\u0410\\u0432\\u0433\\u0443\\u0441\\u0442\": {\"id\": 567953, " +
                "\"survey_question_id\": 394530, \"position\": 3, \"slug\": \"263253\", \"is_hidden\": false, " +
                "\"label_image\": null, \"label\": \"\\u0410\\u0432\\u0433\\u0443\\u0441\\u0442\"}, " +
                "\"\\u041d\\u043e\\u044f\\u0431\\u0440\\u044c\": {\"id\": 567950, \"survey_question_id\": 394530, " +
                "\"position\": 4, \"slug\": \"263254\", \"is_hidden\": false, \"label_image\": null, \"label\": " +
                "\"\\u041d\\u043e\\u044f\\u0431\\u0440\\u044c\"}}, \"label\": {\"ru\": " +
                "\"\\u041a\\u043e\\u0433\\u0434\\u0430 \\u043d\\u0443\\u0436\\u043d\\u0430 " +
                "\\u043f\\u043e\\u0441\\u0442\\u0430\\u0432\\u043a\\u0430\"}}, \"choice_id\": 567950, \"value\": " +
                "\"\\u041d\\u043e\\u044f\\u0431\\u0440\\u044c\"}");
        formMap.add("field_26", "{\"question\": {\"slug\": \"answer_long_text_159605\", \"group_id\": null, \"id\": " +
                "394531, \"label\": {\"ru\": \"\\u0427\\u0442\\u043e " +
                "\\u0441\\u043b\\u0443\\u0447\\u0438\\u0442\\u0441\\u044f, \\u0435\\u0441\\u043b\\u0438 " +
                "\\u0437\\u0430\\u043a\\u0430\\u0437 \\u0431\\u0443\\u0434\\u0435\\u0442 " +
                "\\u043e\\u0442\\u043a\\u043b\\u043e\\u043d\\u0451\\u043d \\u0438\\u043b\\u0438 " +
                "\\u0443\\u043c\\u0435\\u043d\\u044c\\u0448\\u0435\\u043d, \\u0438 \\u043a\\u0430\\u043a " +
                "\\u044d\\u0442\\u043e \\u043f\\u043e\\u0432\\u043b\\u0438\\u044f\\u0435\\u0442 \\u043d\\u0430 " +
                "\\u0441\\u0435\\u0440\\u0432\\u0438\\u0441?\"}, \"type\": {\"id\": 2, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": true, \"is_read_only\": false, \"slug\": \"answer_long_text\", " +
                "\"name\": \"\\u0414\\u043b\\u0438\\u043d\\u043d\\u044b\\u0439 \\u043e\\u0442\\u0432\\u0435\\u0442\"," +
                " \"icon\": \"icon-text-height\", \"kind\": \"generic\", \"admin_preview\": \"textarea\", " +
                "\"allow_settings\": [\"param_help_text\", \"param_hint_data_source\", \"param_initial\", " +
                "\"param_is_hidden\", \"param_is_required\", \"param_max\", \"param_min\", \"param_slug\"], " +
                "\"required_settings\": [], \"is_allow_widgets\": false, \"validator_types\": [{\"id\": 1, \"name\": " +
                "\"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}}, \"value\": \"null\"}");
        formMap.add("field_7", "{\"question\": {\"slug\": \"ram_man\", \"group_id\": 394522, \"id\": 394524, " +
                "\"label\": {\"ru\": \"RAM\"}, \"type\": {\"id\": 31, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": true, \"is_read_only\": false, \"slug\": \"answer_number\", " +
                "\"name\": \"\\u041f\\u043e\\u043b\\u0435 \\u0432\\u0432\\u043e\\u0434\\u0430 " +
                "\\u0446\\u0438\\u0444\\u0440\", \"icon\": \"icon-italic\", \"kind\": \"generic\", \"admin_preview\":" +
                " \"payment\", \"allow_settings\": [\"param_help_text\", \"param_initial\", \"param_is_hidden\", " +
                "\"param_is_required\", \"param_max\", \"param_min\", \"param_slug\"], \"required_settings\": " +
                "[\"param_max\", \"param_min\"], \"is_allow_widgets\": false, \"validator_types\": [{\"id\": 1, " +
                "\"name\": \"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}}, \"value\": {\"ram_man__0\": \"1\", \"ram_man__1\": \"2\"}}");
        formMap.add("field_8", "{\"question\": {\"slug\": \"abc_service_id\", \"group_id\": null, \"id\": 394537, " +
                "\"type\": {\"id\": 3, \"is_allow_choices\": true, \"is_could_be_used_in_conditions\": true, " +
                "\"is_read_only\": false, \"slug\": \"answer_choices\", \"name\": " +
                "\"\\u0421\\u043f\\u0438\\u0441\\u043e\\u043a\", \"icon\": \"icon-list-ul\", \"kind\": \"generic\", " +
                "\"admin_preview\": \"list\", \"allow_settings\": [\"param_data_source\", " +
                "\"param_data_source_params\", \"param_help_text\", \"param_initial\", " +
                "\"param_is_allow_multiple_choice\", \"param_is_allow_other\", \"param_is_disabled_init_item\", " +
                "\"param_is_hidden\", \"param_is_lecture_labels_as_links\", \"param_is_random_choices_position\", " +
                "\"param_is_required\", \"param_modify_choices\", \"param_slug\", \"param_suggest_choices\"], " +
                "\"required_settings\": [], \"is_allow_widgets\": true, \"validator_types\": [{\"id\": 1, \"name\": " +
                "\"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}, \"choices\": {}, \"label\": {\"ru\": \"ABC " +
                "\\u0441\\u0435\\u0440\\u0432\\u0438\\u0441\"}}, \"choice_id\": null, \"value\": " +
                "\"\\u042d\\u043a\\u0441\\u043f\\u043b\\u0443\\u0430\\u0442\\u0430\\u0446\\u0438\\u044f " +
                "\\u041c\\u0430\\u0440\\u043a\\u0435\\u0442\\u0430\"}");
        formMap.add("field_5", "{\"question\": {\"slug\": \"ram_vla\", \"group_id\": 394511, \"id\": 394516, " +
                "\"label\": {\"ru\": \"RAM\"}, \"type\": {\"id\": 31, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": true, \"is_read_only\": false, \"slug\": \"answer_number\", " +
                "\"name\": \"\\u041f\\u043e\\u043b\\u0435 \\u0432\\u0432\\u043e\\u0434\\u0430 " +
                "\\u0446\\u0438\\u0444\\u0440\", \"icon\": \"icon-italic\", \"kind\": \"generic\", \"admin_preview\":" +
                " \"payment\", \"allow_settings\": [\"param_help_text\", \"param_initial\", \"param_is_hidden\", " +
                "\"param_is_required\", \"param_max\", \"param_min\", \"param_slug\"], \"required_settings\": " +
                "[\"param_max\", \"param_min\"], \"is_allow_widgets\": false, \"validator_types\": [{\"id\": 1, " +
                "\"name\": \"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}}, \"value\": null}");
        formMap.add("field_6", "{\"question\": {\"slug\": \"ram_sas\", \"group_id\": 394521, \"id\": 394520, " +
                "\"label\": {\"ru\": \"RAM\"}, \"type\": {\"id\": 31, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": true, \"is_read_only\": false, \"slug\": \"answer_number\", " +
                "\"name\": \"\\u041f\\u043e\\u043b\\u0435 \\u0432\\u0432\\u043e\\u0434\\u0430 " +
                "\\u0446\\u0438\\u0444\\u0440\", \"icon\": \"icon-italic\", \"kind\": \"generic\", \"admin_preview\":" +
                " \"payment\", \"allow_settings\": [\"param_help_text\", \"param_initial\", \"param_is_hidden\", " +
                "\"param_is_required\", \"param_max\", \"param_min\", \"param_slug\"], \"required_settings\": " +
                "[\"param_max\", \"param_min\"], \"is_allow_widgets\": false, \"validator_types\": [{\"id\": 1, " +
                "\"name\": \"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}}, \"value\": null}");
        formMap.add("field_9", "{\"question\": {\"slug\": \"dc\", \"group_id\": null, \"id\": 394510, \"type\": " +
                "{\"id\": 3, \"is_allow_choices\": true, \"is_could_be_used_in_conditions\": true, \"is_read_only\": " +
                "false, \"slug\": \"answer_choices\", \"name\": \"\\u0421\\u043f\\u0438\\u0441\\u043e\\u043a\", " +
                "\"icon\": \"icon-list-ul\", \"kind\": \"generic\", \"admin_preview\": \"list\", \"allow_settings\": " +
                "[\"param_data_source\", \"param_data_source_params\", \"param_help_text\", \"param_initial\", " +
                "\"param_is_allow_multiple_choice\", \"param_is_allow_other\", \"param_is_disabled_init_item\", " +
                "\"param_is_hidden\", \"param_is_lecture_labels_as_links\", \"param_is_random_choices_position\", " +
                "\"param_is_required\", \"param_modify_choices\", \"param_slug\", \"param_suggest_choices\"], " +
                "\"required_settings\": [], \"is_allow_widgets\": true, \"validator_types\": [{\"id\": 1, \"name\": " +
                "\"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}, \"choices\": {\"MAN\": {\"id\": 567955, \"survey_question_id\": 394510, " +
                "\"position\": 3, \"slug\": \"262986\", \"is_hidden\": false, \"label_image\": null, \"label\": " +
                "\"MAN\"}, \"SAS\": {\"id\": 567959, \"survey_question_id\": 394510, \"position\": 2, \"slug\": " +
                "\"262985\", \"is_hidden\": false, \"label_image\": null, \"label\": \"SAS\"}, \"VLA\": {\"id\": " +
                "567963, \"survey_question_id\": 394510, \"position\": 1, \"slug\": \"262984\", \"is_hidden\": false," +
                " \"label_image\": null, \"label\": \"VLA\"}}, \"label\": {\"ru\": \"DC\"}}, \"choice_id\": 567955, " +
                "\"value\": \"MAN\"}");
        formMap.add("field_10", "{\"question\": {\"slug\": \"ssd_vla\", \"group_id\": 394511, \"id\": 394514, " +
                "\"label\": {\"ru\": \"SSD\"}, \"type\": {\"id\": 31, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": true, \"is_read_only\": false, \"slug\": \"answer_number\", " +
                "\"name\": \"\\u041f\\u043e\\u043b\\u0435 \\u0432\\u0432\\u043e\\u0434\\u0430 " +
                "\\u0446\\u0438\\u0444\\u0440\", \"icon\": \"icon-italic\", \"kind\": \"generic\", \"admin_preview\":" +
                " \"payment\", \"allow_settings\": [\"param_help_text\", \"param_initial\", \"param_is_hidden\", " +
                "\"param_is_required\", \"param_max\", \"param_min\", \"param_slug\"], \"required_settings\": " +
                "[\"param_max\", \"param_min\"], \"is_allow_widgets\": false, \"validator_types\": [{\"id\": 1, " +
                "\"name\": \"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}}, \"value\": {\"ssd_vla__0\": null, \"ssd_vla__1\": \"10\"}}");
        formMap.add("field_11", "{\"question\": {\"slug\": \"ssd_sas\", \"group_id\": 394521, \"id\": 394518, " +
                "\"label\": {\"ru\": \"SSD\"}, \"type\": {\"id\": 31, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": true, \"is_read_only\": false, \"slug\": \"answer_number\", " +
                "\"name\": \"\\u041f\\u043e\\u043b\\u0435 \\u0432\\u0432\\u043e\\u0434\\u0430 " +
                "\\u0446\\u0438\\u0444\\u0440\", \"icon\": \"icon-italic\", \"kind\": \"generic\", \"admin_preview\":" +
                " \"payment\", \"allow_settings\": [\"param_help_text\", \"param_initial\", \"param_is_hidden\", " +
                "\"param_is_required\", \"param_max\", \"param_min\", \"param_slug\"], \"required_settings\": " +
                "[\"param_max\", \"param_min\"], \"is_allow_widgets\": false, \"validator_types\": [{\"id\": 1, " +
                "\"name\": \"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}}, \"value\": null}");
        formMap.add("field_12", "{\"question\": {\"slug\": \"ssd_man\", \"group_id\": 394522, \"id\": 394526, " +
                "\"label\": {\"ru\": \"SSD\"}, \"type\": {\"id\": 31, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": true, \"is_read_only\": false, \"slug\": \"answer_number\", " +
                "\"name\": \"\\u041f\\u043e\\u043b\\u0435 \\u0432\\u0432\\u043e\\u0434\\u0430 " +
                "\\u0446\\u0438\\u0444\\u0440\", \"icon\": \"icon-italic\", \"kind\": \"generic\", \"admin_preview\":" +
                " \"payment\", \"allow_settings\": [\"param_help_text\", \"param_initial\", \"param_is_hidden\", " +
                "\"param_is_required\", \"param_max\", \"param_min\", \"param_slug\"], \"required_settings\": " +
                "[\"param_max\", \"param_min\"], \"is_allow_widgets\": false, \"validator_types\": [{\"id\": 1, " +
                "\"name\": \"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}}, \"value\": null}");
        formMap.add("field_13", "{\"question\": {\"slug\": \"answer_group_158232\", \"group_id\": null, \"id\": " +
                "394511, \"label\": {\"ru\": \"VLA\"}, \"type\": {\"id\": 1040, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": false, \"is_read_only\": true, \"slug\": \"answer_group\", " +
                "\"name\": \"\\u0413\\u0440\\u0443\\u043f\\u043f\\u0430 " +
                "\\u0432\\u043e\\u043f\\u0440\\u043e\\u0441\\u043e\\u0432\", \"icon\": \"icon-quote-left\", \"kind\":" +
                " \"generic\", \"admin_preview\": \"answer_group\", \"allow_settings\": [\"param_help_text\"], " +
                "\"required_settings\": [], \"is_allow_widgets\": false, \"validator_types\": []}}, \"value\": " +
                "\"[\\u041e\\u0442\\u0432\\u0435\\u0442\\u043e\\u0432 \\u043d\\u0430 " +
                "\\u0433\\u0440\\u0443\\u043f\\u043f\\u0443 \\u043d\\u0435 " +
                "\\u043d\\u0430\\u0439\\u0434\\u0435\\u043d\\u043e]\"}");
        formMap.add("field_14", "{\"question\": {\"slug\": \"hdd_vla\", \"group_id\": 394511, \"id\": 394515, " +
                "\"label\": {\"ru\": \"HDD\"}, \"type\": {\"id\": 31, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": true, \"is_read_only\": false, \"slug\": \"answer_number\", " +
                "\"name\": \"\\u041f\\u043e\\u043b\\u0435 \\u0432\\u0432\\u043e\\u0434\\u0430 " +
                "\\u0446\\u0438\\u0444\\u0440\", \"icon\": \"icon-italic\", \"kind\": \"generic\", \"admin_preview\":" +
                " \"payment\", \"allow_settings\": [\"param_help_text\", \"param_initial\", \"param_is_hidden\", " +
                "\"param_is_required\", \"param_max\", \"param_min\", \"param_slug\"], \"required_settings\": " +
                "[\"param_max\", \"param_min\"], \"is_allow_widgets\": false, \"validator_types\": [{\"id\": 1, " +
                "\"name\": \"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}}, \"value\": null}");
        formMap.add("field_15", "{\"question\": {\"slug\": \"hdd_sas\", \"group_id\": 394521, \"id\": 394519, " +
                "\"label\": {\"ru\": \"HDD\"}, \"type\": {\"id\": 31, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": true, \"is_read_only\": false, \"slug\": \"answer_number\", " +
                "\"name\": \"\\u041f\\u043e\\u043b\\u0435 \\u0432\\u0432\\u043e\\u0434\\u0430 " +
                "\\u0446\\u0438\\u0444\\u0440\", \"icon\": \"icon-italic\", \"kind\": \"generic\", \"admin_preview\":" +
                " \"payment\", \"allow_settings\": [\"param_help_text\", \"param_initial\", \"param_is_hidden\", " +
                "\"param_is_required\", \"param_max\", \"param_min\", \"param_slug\"], \"required_settings\": " +
                "[\"param_max\", \"param_min\"], \"is_allow_widgets\": false, \"validator_types\": [{\"id\": 1, " +
                "\"name\": \"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}}, \"value\": null}");
        formMap.add("field_16", "{\"question\": {\"slug\": \"hdd_man\", \"group_id\": 394522, \"id\": 394525, " +
                "\"label\": {\"ru\": \"HDD\"}, \"type\": {\"id\": 31, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": true, \"is_read_only\": false, \"slug\": \"answer_number\", " +
                "\"name\": \"\\u041f\\u043e\\u043b\\u0435 \\u0432\\u0432\\u043e\\u0434\\u0430 " +
                "\\u0446\\u0438\\u0444\\u0440\", \"icon\": \"icon-italic\", \"kind\": \"generic\", \"admin_preview\":" +
                " \"payment\", \"allow_settings\": [\"param_help_text\", \"param_initial\", \"param_is_hidden\", " +
                "\"param_is_required\", \"param_max\", \"param_min\", \"param_slug\"], \"required_settings\": " +
                "[\"param_max\", \"param_min\"], \"is_allow_widgets\": false, \"validator_types\": [{\"id\": 1, " +
                "\"name\": \"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}]}}, \"value\": null}");
        formMap.add("field_17", "{\"question\": {\"slug\": \"answer_group_158245\", \"group_id\": null, \"id\": " +
                "394521, \"label\": {\"ru\": \"SAS\"}, \"type\": {\"id\": 1040, \"is_allow_choices\": false, " +
                "\"is_could_be_used_in_conditions\": false, \"is_read_only\": true, \"slug\": \"answer_group\", " +
                "\"name\": \"\\u0413\\u0440\\u0443\\u043f\\u043f\\u0430 " +
                "\\u0432\\u043e\\u043f\\u0440\\u043e\\u0441\\u043e\\u0432\", \"icon\": \"icon-quote-left\", \"kind\":" +
                " \"generic\", \"admin_preview\": \"answer_group\", \"allow_settings\": [\"param_help_text\"], " +
                "\"required_settings\": [], \"is_allow_widgets\": false, \"validator_types\": []}}, \"value\": " +
                "\"[\\u041e\\u0442\\u0432\\u0435\\u0442\\u043e\\u0432 \\u043d\\u0430 " +
                "\\u0433\\u0440\\u0443\\u043f\\u043f\\u0443 \\u043d\\u0435 " +
                "\\u043d\\u0430\\u0439\\u0434\\u0435\\u043d\\u043e]\"}");
        formMap.add("field_18", "{\"question\": {\"slug\": \"answer_short_text_158247\", \"group_id\": 394521, " +
                "\"id\": 394523, \"label\": {\"ru\": \"\\u041d\\u0430\\u0437\\u0432\\u0430\\u043d\\u0438\\u0435 " +
                "\\u0441\\u0435\\u0440\\u0432\\u0438\\u0441\\u043e\\u0432\"}, \"type\": {\"id\": 1, " +
                "\"is_allow_choices\": false, \"is_could_be_used_in_conditions\": true, \"is_read_only\": false, " +
                "\"slug\": \"answer_short_text\", \"name\": " +
                "\"\\u041a\\u043e\\u0440\\u043e\\u0442\\u043a\\u0438\\u0439 \\u043e\\u0442\\u0432\\u0435\\u0442\", " +
                "\"icon\": \"icon-italic\", \"kind\": \"generic\", \"admin_preview\": \"input\", \"allow_settings\": " +
                "[\"param_help_text\", \"param_hint_data_source\", \"param_initial\", \"param_is_hidden\", " +
                "\"param_is_required\", \"param_max\", \"param_min\", \"param_slug\"], \"required_settings\": [], " +
                "\"is_allow_widgets\": false, \"validator_types\": [{\"id\": 1, \"name\": " +
                "\"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}, {\"id\": 2, \"name\": " +
                "\"\\u0412\\u0430\\u043b\\u0438\\u0434\\u0430\\u0446\\u0438\\u044f " +
                "\\u0434\\u0440\\u043e\\u0431\\u043d\\u044b\\u0445 \\u0447\\u0438\\u0441\\u0435\\u043b\", \"slug\": " +
                "\"decimal\", \"is_external\": false}, {\"id\": 1003, \"name\": " +
                "\"\\u0412\\u0430\\u043b\\u0438\\u0434\\u0430\\u0446\\u0438\\u044f \\u0418\\u041d\\u041d\", \"slug\":" +
                " \"inn\", \"is_external\": false}, {\"id\": 1004, \"name\": " +
                "\"\\u0412\\u0430\\u043b\\u0438\\u0434\\u0430\\u0446\\u0438\\u044f " +
                "\\u0440\\u0443\\u0441\\u0441\\u043a\\u0438\\u0445 \\u0431\\u0443\\u043a\\u0432\", \"slug\": " +
                "\"russian\", \"is_external\": false}, {\"id\": 1037, \"name\": " +
                "\"\\u0412\\u0430\\u043b\\u0438\\u0434\\u0430\\u0446\\u0438\\u044f " +
                "\\u0447\\u0435\\u0440\\u0435\\u0437 \\u0440\\u0435\\u0433\\u0443\\u043b\\u044f\\u0440\\u043d\\u044b" +
                "\\u0435 \\u0432\\u044b\\u0440\\u0430\\u0436\\u0435\\u043d\\u0438\\u044f\", \"slug\": \"regexp\", " +
                "\"is_external\": false}]}}, \"value\": null}");
        formMap.add("field_19", "{\"question\": {\"slug\": \"service_name_man\", \"group_id\": 394522, \"id\": " +
                "394528, \"label\": {\"ru\": \"\\u041d\\u0430\\u0437\\u0432\\u0430\\u043d\\u0438\\u0435 " +
                "\\u0441\\u0435\\u0440\\u0432\\u0438\\u0441\\u043e\\u0432\"}, \"type\": {\"id\": 1, " +
                "\"is_allow_choices\": false, \"is_could_be_used_in_conditions\": true, \"is_read_only\": false, " +
                "\"slug\": \"answer_short_text\", \"name\": " +
                "\"\\u041a\\u043e\\u0440\\u043e\\u0442\\u043a\\u0438\\u0439 \\u043e\\u0442\\u0432\\u0435\\u0442\", " +
                "\"icon\": \"icon-italic\", \"kind\": \"generic\", \"admin_preview\": \"input\", \"allow_settings\": " +
                "[\"param_help_text\", \"param_hint_data_source\", \"param_initial\", \"param_is_hidden\", " +
                "\"param_is_required\", \"param_max\", \"param_min\", \"param_slug\"], \"required_settings\": [], " +
                "\"is_allow_widgets\": false, \"validator_types\": [{\"id\": 1, \"name\": " +
                "\"\\u0412\\u043d\\u0435\\u0448\\u043d\\u0438\\u0439 " +
                "\\u0432\\u0430\\u043b\\u0438\\u0434\\u0430\\u0442\\u043e\\u0440\", \"slug\": \"external\", " +
                "\"is_external\": true}, {\"id\": 2, \"name\": " +
                "\"\\u0412\\u0430\\u043b\\u0438\\u0434\\u0430\\u0446\\u0438\\u044f " +
                "\\u0434\\u0440\\u043e\\u0431\\u043d\\u044b\\u0445 \\u0447\\u0438\\u0441\\u0435\\u043b\", \"slug\": " +
                "\"decimal\", \"is_external\": false}, {\"id\": 1003, \"name\": " +
                "\"\\u0412\\u0430\\u043b\\u0438\\u0434\\u0430\\u0446\\u0438\\u044f \\u0418\\u041d\\u041d\", \"slug\":" +
                " \"inn\", \"is_external\": false}, {\"id\": 1004, \"name\": " +
                "\"\\u0412\\u0430\\u043b\\u0438\\u0434\\u0430\\u0446\\u0438\\u044f " +
                "\\u0440\\u0443\\u0441\\u0441\\u043a\\u0438\\u0445 \\u0431\\u0443\\u043a\\u0432\", \"slug\": " +
                "\"russian\", \"is_external\": false}, {\"id\": 1037, \"name\": " +
                "\"\\u0412\\u0430\\u043b\\u0438\\u0434\\u0430\\u0446\\u0438\\u044f " +
                "\\u0447\\u0435\\u0440\\u0435\\u0437 \\u0440\\u0435\\u0433\\u0443\\u043b\\u044f\\u0440\\u043d\\u044b" +
                "\\u0435 \\u0432\\u044b\\u0440\\u0430\\u0436\\u0435\\u043d\\u0438\\u044f\", \"slug\": \"regexp\", " +
                "\"is_external\": false}]}}, \"value\": null}");

        MultiValueMap<String, String> expectedFormMap = new LinkedMultiValueMap<>();
        expectedFormMap.addAll("service_name_vla", Collections.singletonList(null));
        expectedFormMap.addAll("cpu_sas", Collections.singletonList(null));
        expectedFormMap.addAll("answer_group_158246", Collections.singletonList("MAN\nCPU - 1\nRAM - 1\n\nMAN\nCPU - " +
                "3\nRAM - 2"));
        expectedFormMap.addAll("cpu_man", Arrays.asList("1", "3"));
        expectedFormMap.addAll("reason", Collections.singletonList("Test"));
        expectedFormMap.addAll("answer_statement_394509", Collections.singletonList(null));
        expectedFormMap.addAll("resps", Collections.singletonList("Some Body (somebody)"));
        expectedFormMap.addAll("market_color", Collections.singletonList("Белый"));
        expectedFormMap.addAll("cpu_vla", Collections.singletonList(null));
        expectedFormMap.addAll("answer_choices_158570", Collections.singletonList("Ноябрь"));
        expectedFormMap.addAll("answer_long_text_159605", Collections.singletonList(null));
        expectedFormMap.addAll("ram_man", Arrays.asList("1", "2"));
        expectedFormMap.addAll("abc_service_id", Collections.singletonList("Эксплуатация Маркета"));
        expectedFormMap.addAll("ram_vla", Collections.singletonList(null));
        expectedFormMap.addAll("ram_sas", Collections.singletonList(null));
        expectedFormMap.addAll("dc", Collections.singletonList("MAN"));
        expectedFormMap.addAll("ssd_vla", Arrays.asList(null, "10"));
        expectedFormMap.addAll("ssd_sas", Collections.singletonList(null));
        expectedFormMap.addAll("ssd_man", Collections.singletonList(null));
        expectedFormMap.addAll("answer_group_158232", Collections.singletonList("[Ответов на группу не найдено]"));
        expectedFormMap.addAll("hdd_vla", Collections.singletonList(null));
        expectedFormMap.addAll("hdd_sas", Collections.singletonList(null));
        expectedFormMap.addAll("hdd_man", Collections.singletonList(null));
        expectedFormMap.addAll("answer_group_158245", Collections.singletonList("[Ответов на группу не найдено]"));
        expectedFormMap.addAll("answer_short_text_158247", Collections.singletonList(null));
        expectedFormMap.addAll("service_name_man", Collections.singletonList(null));

        //test case
        Assert.assertEquals(expectedFormMap, rtcForecasterParser.parseFormMap(formMap));
    }

    @Test
    public void headerParser() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-FoRm-Id", "123456");
        headers.put("X-dElIvErY-ID", "54007217fca71b3439f35624");
        headers.put("x-form-answer-id", "13579");
        headers.put("X-Yandex-Login", "somebody");
        headers.put("X-Mcrp-Preorder-Id", "test-preorder");
        headers.put("X-Uknown-Header", "justheader");

        Map<String, String> expectedFormInfo = new HashMap<>();
        expectedFormInfo.put("form_id", "123456");
        expectedFormInfo.put("form_delivery_id", "54007217fca71b3439f35624");
        expectedFormInfo.put("form_answer_id", "13579");
        expectedFormInfo.put("creator_login", "somebody");
        expectedFormInfo.put("preorder_id", "test-preorder");

        Assert.assertEquals(expectedFormInfo, rtcForecasterParser.parseFormHeaders(headers));
    }

    @Test
    public void RTCResourceParser() {
        MultiValueMap<String, String> formMap = new LinkedMultiValueMap<>();
        formMap.addAll("servicename_vla", Arrays.asList(null, "fakevlaservice"));
        formMap.addAll("cpu_vla", Arrays.asList("10.1", "20.5"));
        formMap.addAll("ram_vla", Arrays.asList("512", "512"));
        formMap.addAll("ssd_vla", Arrays.asList("50", null));
        formMap.addAll("hdd_vla", Collections.singletonList(null));

        formMap.addAll("servicename_sas", Collections.singletonList("fakesasservice"));
        formMap.addAll("cpu_sas", Collections.singletonList("12"));
        formMap.addAll("ram_sas", Collections.singletonList("1024"));
        formMap.addAll("ssd_sas", Collections.singletonList(null));
        formMap.addAll("hdd_sas", Collections.singletonList(null));

        formMap.addAll("unknown_field", Collections.singletonList(null));

        RTCResources.Builder rtcResourceBuilder = new RTCResources.Builder();

        rtcResourceBuilder.setServiceName("VLA", 1, "fakevlaservice");
        rtcResourceBuilder.setCPUCores("VLA", 0, (float) 10.1);
        rtcResourceBuilder.setCPUCores("VLA", 1, (float) 20.5);
        rtcResourceBuilder.setRAMGb("VLA", 0, 512);
        rtcResourceBuilder.setRAMGb("VLA", 1, 512);
        rtcResourceBuilder.setSSDGb("VLA", 0, 50);

        rtcResourceBuilder.setServiceName("SAS", 0, "fakesasservice");
        rtcResourceBuilder.setCPUCores("SAS", 0, 12);
        rtcResourceBuilder.setRAMGb("SAS", 0, 1024);

        Assert.assertEquals(rtcResourceBuilder.build(), rtcForecasterParser.parseResources(formMap));
    }

    @Test
    public void MDBResourceParser() {
        MultiValueMap<String, String> formMap = new LinkedMultiValueMap<>();
        formMap.addAll("cpu", Collections.singletonList("15"));
        formMap.addAll("ram", Collections.singletonList("2048"));
        formMap.addAll("ssd", Collections.singletonList(null));
        formMap.addAll("hdd", Collections.singletonList("500"));
        formMap.addAll("mdbcloud", Collections.singletonList("redis"));

        MDBResources.Builder mdbResourceBuilder = new MDBResources.Builder();

        mdbResourceBuilder.setCPUCores("REDIS", 0, 15);
        mdbResourceBuilder.setRAMGb("REDIS", 0, 2048);
        mdbResourceBuilder.setHDDGb("REDIS", 0, 500);

        Assert.assertEquals(mdbResourceBuilder.build(), mdbFormsParser.parseResources(formMap));
    }

    @Test
    public void YTResourceParser() {
        MultiValueMap<String, String> formMap = new LinkedMultiValueMap<>();
        formMap.addAll("accounts_hahn", Arrays.asList(null, "fakehahnaccount1"));
        formMap.addAll("cpu_hahn", Arrays.asList("8.1", "40"));
        formMap.addAll("ssd_hahn", Arrays.asList("80", null));
        formMap.addAll("hdd_hahn", Collections.singletonList(null));
        formMap.addAll("dintableram_hahn", Arrays.asList(null, "100"));
        formMap.addAll("dintablenodes_hahn", Arrays.asList("5", "5"));

        formMap.addAll("accounts_senecasas", Collections.singletonList("fakesenecasasaccount"));
        formMap.addAll("cpu_senecasas", Collections.singletonList("12"));
        formMap.addAll("ssd_senecasas", Collections.singletonList(null));
        formMap.addAll("hdd_senecasas", Collections.singletonList(null));
        formMap.addAll("dintableram_senecasas", Collections.singletonList(null));
        formMap.addAll("dintablenodes_senecasas", Collections.singletonList("6"));
        formMap.addAll("pools_senecasas", Collections.singletonList("testpool"));

        formMap.addAll("network_markov", Collections.singletonList("26"));

        formMap.addAll("unknown_field", Collections.singletonList(null));

        YTResources.Builder ytResourceBuilder = new YTResources.Builder();

        ytResourceBuilder.setAccounts("Hahn", 1, "fakehahnaccount1");
        ytResourceBuilder.setCPUCores("Hahn", 0, (float) 8.1);
        ytResourceBuilder.setCPUCores("Hahn", 1, 40);
        ytResourceBuilder.setSSDGb("Hahn", 0, 80);
        ytResourceBuilder.setDinTableRAMGb("Hahn", 1, 100);
        ytResourceBuilder.setDinTableNodes("Hahn", 0, 5);
        ytResourceBuilder.setDinTableNodes("Hahn", 1, 5);

        ytResourceBuilder.setAccounts("Seneca-SAS", 0, "fakesenecasasaccount");
        ytResourceBuilder.setCPUCores("Seneca-SAS", 0, 12);
        ytResourceBuilder.setDinTableNodes("Seneca-SAS", 0, 6);
        ytResourceBuilder.setPools("Seneca-SAS", 0, "testpool");

        ytResourceBuilder.setNetworkBps("Markov", 0, 26);

        Assert.assertEquals(ytResourceBuilder.build(), ytFormsParser.parseResources(formMap));
    }

    @Test
    public void MDSResourceParser() {
        MultiValueMap<String, String> formMap = new LinkedMultiValueMap<>();
        formMap.addAll("storage_type", Collections.singletonList("mDs"));
        formMap.addAll("servicename", Collections.singletonList(null));
        formMap.addAll("hdd", Collections.singletonList("451"));

        MDSResources.Builder mdsResourceBuilder = new MDSResources.Builder();

        mdsResourceBuilder.setStorageGb("MDS", 0, 451);

        Assert.assertEquals(mdsResourceBuilder.build(), mdsFormsParser.parseResources(formMap));
    }

    @Test
    public void SandboxResourceParser() {
        MultiValueMap<String, String> formMap = new LinkedMultiValueMap<>();
        formMap.addAll("cpu", Collections.singletonList("45"));
        formMap.addAll("ram", Collections.singletonList(null));
        formMap.addAll("servicename", Collections.singletonList("sandboxresource"));
        formMap.addAll("segment", Collections.singletonList("LinuxYP"));
        formMap.addAll("ssd", Collections.singletonList("120"));
        formMap.addAll("hdd", Collections.singletonList("344"));

        SandboxResources.Builder sandboxResourceBuilder = new SandboxResources.Builder();

        sandboxResourceBuilder.setCPUCores("LINUXYP", 0, 45);
        sandboxResourceBuilder.setSSDGb("LINUXYP", 0, 120);
        sandboxResourceBuilder.setHDDGb("LINUXYP", 0, 344);

        Assert.assertEquals(sandboxResourceBuilder.build(), sandboxFormsParser.parseResources(formMap));
    }

    @Test
    public void YPResourceParser() {
        MultiValueMap<String, String> formMap = new LinkedMultiValueMap<>();
        formMap.addAll("servicename_man", Arrays.asList(null, "fakemanypservice"));
        formMap.addAll("cpu_man", Arrays.asList("8.2", "23.7"));
        formMap.addAll("ram_man", Arrays.asList("1024", "1024"));
        formMap.addAll("ssd_man", Arrays.asList("60", null));
        formMap.addAll("hdd_man", Collections.singletonList(null));

        formMap.addAll("servicename_sas", Collections.singletonList("fakesasypservice"));
        formMap.addAll("cpu_sas", Collections.singletonList("10"));
        formMap.addAll("ram_sas", Collections.singletonList("512"));
        formMap.addAll("ssd_sas", Collections.singletonList(null));
        formMap.addAll("hdd_sas", Collections.singletonList(null));

        formMap.addAll("unknown_field", Collections.singletonList(null));

        YPResources.Builder ypResourceBuilder = new YPResources.Builder();

        ypResourceBuilder.setServiceName("MAN", 1, "fakemanypservice");
        ypResourceBuilder.setCPUCores("MAN", 0, (float) 8.2);
        ypResourceBuilder.setCPUCores("MAN", 1, (float) 23.7);
        ypResourceBuilder.setRAMGb("MAN", 0, 1024);
        ypResourceBuilder.setRAMGb("MAN", 1, 1024);
        ypResourceBuilder.setSSDGb("MAN", 0, 60);

        ypResourceBuilder.setServiceName("SAS", 0, "fakesasypservice");
        ypResourceBuilder.setCPUCores("SAS", 0, 10);
        ypResourceBuilder.setRAMGb("SAS", 0, 512);

        Assert.assertEquals(ypResourceBuilder.build(), ypFormsParser.parseResources(formMap));
    }

    @Test
    public void SAASResourceParser() {
        MultiValueMap<String, String> formMap = new LinkedMultiValueMap<>();
        formMap.addAll("servicename_sas", Arrays.asList(null, "fakesassaasservice"));
        formMap.addAll("cpu_sas", Arrays.asList("15.9", "6.0"));
        formMap.addAll("ram_sas", Arrays.asList("256", "512"));
        formMap.addAll("ssd_sas", Arrays.asList("65.1", null));
        formMap.addAll("hdd_sas", Collections.singletonList(null));

        formMap.addAll("servicename_vla", Collections.singletonList("fakevlasaasservice"));
        formMap.addAll("cpu_vla", Collections.singletonList("19"));
        formMap.addAll("ram_vla", Collections.singletonList("2048"));
        formMap.addAll("ssd_vla", Collections.singletonList(null));
        formMap.addAll("hdd_vla", Collections.singletonList(null));

        formMap.addAll("unknown_field", Collections.singletonList(null));

        SAASResources.Builder saasResourceBuilder = new SAASResources.Builder();

        saasResourceBuilder.setServiceName("SAS", 1, "fakesassaasservice");
        saasResourceBuilder.setCPUCores("SAS", 0, (float) 15.9);
        saasResourceBuilder.setCPUCores("SAS", 1, (float) 6.0);
        saasResourceBuilder.setRAMGb("SAS", 0, 256);
        saasResourceBuilder.setRAMGb("SAS", 1, 512);
        saasResourceBuilder.setSSDGb("SAS", 0, (float) 65.1);

        saasResourceBuilder.setServiceName("VLA", 0, "fakevlasaasservice");
        saasResourceBuilder.setCPUCores("VLA", 0, 19);
        saasResourceBuilder.setRAMGb("VLA", 0, 2048);

        Assert.assertEquals(saasResourceBuilder.build(), saasFormsParser.parseResources(formMap));
    }

    @Test
    public void QloudResourceParser() {
        MultiValueMap<String, String> formMap = new LinkedMultiValueMap<>();
        formMap.addAll("servicename_vla", Arrays.asList(null, "fakevlaqloudservice"));
        formMap.addAll("cpu_vla", Arrays.asList("34.9", "16"));
        formMap.addAll("ram_vla", Arrays.asList("1024", "70.78"));
        formMap.addAll("ssd_vla", Arrays.asList("65.1", null));
        formMap.addAll("hdd_vla", Collections.singletonList(null));

        formMap.addAll("servicename_man", Collections.singletonList("fakemanqloudservice"));
        formMap.addAll("cpu_man", Collections.singletonList("9.9"));
        formMap.addAll("ram_man", Collections.singletonList("1111"));
        formMap.addAll("ssd_man", Collections.singletonList(null));
        formMap.addAll("hdd_man", Collections.singletonList(null));

        formMap.addAll("unknown_field", Collections.singletonList(null));

        QloudResources.Builder qloudResourceBuilder = new QloudResources.Builder();

        qloudResourceBuilder.setServiceName("VLA", 1, "fakevlaqloudservice");
        qloudResourceBuilder.setCPUCores("VLA", 0, (float) 34.9);
        qloudResourceBuilder.setCPUCores("VLA", 1, 16);
        qloudResourceBuilder.setRAMGb("VLA", 0, 1024);
        qloudResourceBuilder.setRAMGb("VLA", 1, (float) 70.78);
        qloudResourceBuilder.setSSDGb("VLA", 0, (float) 65.1);

        qloudResourceBuilder.setServiceName("MAN", 0, "fakemanqloudservice");
        qloudResourceBuilder.setCPUCores("MAN", 0, (float) 9.9);
        qloudResourceBuilder.setRAMGb("MAN", 0, 1111);

        Assert.assertEquals(qloudResourceBuilder.build(), qloudFormsParser.parseResources(formMap));
    }


    @Test
    public void LogbrokerResourceParser() {
        MultiValueMap<String, String> formMap = new LinkedMultiValueMap<>();
        formMap.addAll("servicename_man", Arrays.asList(null, "fakemanlogbrokerservice"));
        formMap.addAll("writespeed_man", Arrays.asList("100.81", "800.1"));

        formMap.addAll("servicename_sas", Collections.singletonList("fakesaslogbrokerservice"));

        formMap.addAll("unknown_field", Collections.singletonList(null));

        LogbrokerResources.Builder logbrokerResourceBuilder = new LogbrokerResources.Builder();

        logbrokerResourceBuilder.setServiceName("MAN", 1, "fakemanlogbrokerservice");
        logbrokerResourceBuilder.setWriteThroughputBinary("MAN", 0, (float) 100.81);
        logbrokerResourceBuilder.setWriteThroughputBinary("MAN", 1, (float) 800.1);

        logbrokerResourceBuilder.setServiceName("SAS", 0, "fakesaslogbrokerservice");

        Assert.assertEquals(logbrokerResourceBuilder.build(), logbrokerFormsParser.parseResources(formMap));
    }

    @Test
    public void nirvanaResourceParser() {
        MultiValueMap<String, String> formMap = new LinkedMultiValueMap<>();
        formMap.addAll("cpu_nirvana", Collections.singletonList("12"));
        formMap.addAll("gpu_vla", Arrays.asList("10", "20"));

        NirvanaResources.Builder nirvanaResourcesBuilder = new NirvanaResources.Builder();
        nirvanaResourcesBuilder.setCPUCores("NIRVANA", 0, 12);
        nirvanaResourcesBuilder.setGPUCards("VLA", 0, 10);
        nirvanaResourcesBuilder.setGPUCards("VLA", 1, 20);

        Assert.assertEquals(nirvanaResourcesBuilder.build(), nirvanaFormsParser.parseResources(formMap));
    }

    @Test
    public void bareMetalResourceParser() {
        MultiValueMap<String, String> formMap = new LinkedMultiValueMap<>();
        formMap.addAll("servertype_vla", Collections.singletonList("Node 3.0 / Intel Xeon 6230"));
        formMap.addAll("server_vla", Collections.singletonList("12"));
        formMap.addAll("servertype_sas", Collections.singletonList("Другая"));
        formMap.addAll("servermanualtype_sas", Collections.singletonList("Xeon 2660v2"));
        formMap.addAll("server_sas", Collections.singletonList("4"));
        formMap.addAll("shelf_vla", Arrays.asList("10", "20"));
        formMap.addAll("shelfconf_vla", Collections.singletonList("2U Storage 3.0/28 * 10 TB HDD"));
        formMap.addAll("hddsize_sas", Arrays.asList("4.5", "10"));
        formMap.addAll("hdd_sas", Arrays.asList("12", "65"));
        formMap.addAll("ssdsatasize_man", Collections.singletonList("12"));
        formMap.addAll("ssdsata_man", Collections.singletonList("1"));
        formMap.addAll("ssdnvmesize_man", Collections.singletonList("1.8"));
        formMap.addAll("ssdnvme_man", Collections.singletonList("4"));

        BareMetalResources.Builder bareMetalResourcesBuilder = new BareMetalResources.Builder();
        bareMetalResourcesBuilder.setServerType("VLA", 0, "Node 3.0 / Intel Xeon 6230");
        bareMetalResourcesBuilder.setServerCount("VLA", 0, 12);
        bareMetalResourcesBuilder.setShelfCount("VLA", 0, 10);
        bareMetalResourcesBuilder.setShelfCount("VLA", 1, 20);
        bareMetalResourcesBuilder.setShelfType("VLA", 0, "2U Storage 3.0/28 * 10 TB HDD");
        bareMetalResourcesBuilder.setServerType("SAS", 0, "Xeon 2660v2");
        bareMetalResourcesBuilder.setServerCount("SAS", 0, 4);
        bareMetalResourcesBuilder.setHDDMinSizeGb("SAS", 0, (float) 4.5);
        bareMetalResourcesBuilder.setHDDMinSizeGb("SAS", 1, 10);
        bareMetalResourcesBuilder.setHDDCount("SAS", 0, 12);
        bareMetalResourcesBuilder.setHDDCount("SAS", 1, 65);
        bareMetalResourcesBuilder.setSSDSataMinSizeGb("MAN", 0, 12);
        bareMetalResourcesBuilder.setSSDSataCount("MAN", 0, 1);
        bareMetalResourcesBuilder.setSSDNvmeMinSizeGb("MAN", 0, (float) 1.8);
        bareMetalResourcesBuilder.setSSDNvmeCount("MAN", 0, 4);

        Assert.assertEquals(bareMetalResourcesBuilder.build(), bareMetalFormsParser.parseResources(formMap));
    }

    @Test
    public void RequestParser() throws IOException, InterruptedException {
        class TestResources implements RequestResources {
            public TestResources fromJsonString(String jsonString) {
                return new TestResources();
            }

            @Override
            public String toJsonString() {
                return "";
            }

            @Override
            public String toPrettyJsonString() {
                return "";
            }

            @Override
            public String getKind() {
                return "TEST";
            }

            @Override
            public RequestResources migrate() {
                return this;
            }
        }

        Mockito.when(abcApiClient.getAbcSlugByName("fake_abc_service")).thenReturn("fake_abc_service_slug");

        MultiValueMap<String, String> formMap = new LinkedMultiValueMap<>();
        formMap.addAll("abc_service_id", Collections.singletonList("fake_abc_service"));
        formMap.addAll("request_type", Collections.singletonList("NEW_SERVICE"));
        formMap.addAll("goal", Collections.singletonList("https://ya.ru"));
        formMap.addAll("name", Collections.singletonList("Request name"));
        formMap.addAll("reason", Collections.singletonList("Very important reason"));
        formMap.addAll("deadline", Collections.singletonList("today"));
        formMap.addAll("platform", Collections.singletonList("blue"));
        formMap.addAll("resps", Collections.singletonList("No Body (nobody), somebody"));
        formMap.addAll("whatifno", Collections.singletonList("nothing"));

        Map<String, String> formInfo = new HashMap<>();
        formInfo.put("form_id", "8888");
        formInfo.put("form_delivery_id", "5b3d47bb663a4c4e91eccd64");
        formInfo.put("form_answer_id", "123");
        formInfo.put("creator_login", "nobody");
        formInfo.put("preorder_id", "new-age-preorder");

        TestResources testResources = new TestResources();

        Request.Builder expectedRequestBuilder = new Request.Builder(
                "new-age-preorder",
                "fake_abc_service_slug",
                "TEST",
                "nobody",
                RequestResourcesData.fromRequestResources("today", testResources), Collections.singletonList("nobody")
        );
        expectedRequestBuilder.setGeneratedBy("forms_test");
        expectedRequestBuilder.setSourceMeta(new AbstractFormsParser.FormMeta(
                "8888",
                "123",
                "5b3d47bb663a4c4e91eccd64",
                "NEW_SERVICE",
                "https://ya.ru"
        ).toJsonString());
        expectedRequestBuilder.setReason("Very important reason");
        expectedRequestBuilder.setWhatIfNo("nothing");

        Request expectedRequest = expectedRequestBuilder.build();
        Request request = rtcForecasterParser.parseRequest(formMap, formInfo, testResources);

        Assert.assertEquals(expectedRequest.getAbc(), request.getAbc());
        Assert.assertEquals(expectedRequest.getCloud(), request.getCloud());
        Assert.assertEquals(expectedRequest.getPlatform(), request.getPlatform());
        Assert.assertEquals(expectedRequest.getCreatedBy(), request.getCreatedBy());
        Assert.assertEquals(expectedRequest.getResources(), request.getResources());
        Assert.assertEquals(expectedRequest.getDeadline(), request.getDeadline());
        Assert.assertEquals(expectedRequest.getReason(), request.getReason());
        Assert.assertEquals(expectedRequest.getGeneratedBy(), request.getGeneratedBy());
        Assert.assertEquals(expectedRequest.getSourceMeta(), request.getSourceMeta());
        Assert.assertEquals(expectedRequest.getResps(), request.getResps());
        Assert.assertEquals(expectedRequest.getWhatifno(), request.getWhatifno());
    }

    @Test
    public void YDBResourceParser() {
        MultiValueMap<String, String> formMap = new LinkedMultiValueMap<>();
        formMap.addAll("storage", Collections.singletonList("100"));
        formMap.addAll("cpu", Collections.singletonList("5"));

        YDBResources.Builder ydbResourceBuilder = new YDBResources.Builder();

        ydbResourceBuilder.setStorageGb("Default", 0, 100);
        ydbResourceBuilder.setCPUCores("Default", 0, 5);

        Assert.assertEquals(ydbResourceBuilder.build(), ydbFormsParser.parseResources(formMap));
    }
}
