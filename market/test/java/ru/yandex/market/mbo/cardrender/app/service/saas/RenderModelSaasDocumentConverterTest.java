package ru.yandex.market.mbo.cardrender.app.service.saas;

import NMarket.NContentStorageExternal.CsSaasCardOuterClass;
import com.google.protobuf.InvalidProtocolBufferException;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.cardrender.app.model.saas.SaasRenderModelHolder;
import ru.yandex.market.mbo.cs.CsGumofulOuterClass;
import ru.yandex.market.mbo.export.ExportReportModels;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.search.saas.RTYServer;

/**
 * @author apluhin
 * @created 5/23/22
 */
public class RenderModelSaasDocumentConverterTest {

    private RenderModelSaasDocumentConverter renderModelSaasDocumentConverter;

    @Before
    public void setUp() throws Exception {
        renderModelSaasDocumentConverter = new RenderModelSaasDocumentConverter();
    }

    @Test
    public void convertToSaas() throws InvalidProtocolBufferException {
        ExportReportModels.ExportReportModel exportReportModel = buildExportReportModel();
        RTYServer.TMessage.TDocument build =
                renderModelSaasDocumentConverter.convert(new SaasRenderModelHolder(exportReportModel)).build();
        CsSaasCardOuterClass.CsSaasCard liteCard =
                CsSaasCardOuterClass.CsSaasCard.newBuilder().mergeFrom(build.getBodyBytes()).build();

        Assertions.assertThat(liteCard.getTitlesList())
                .isEqualTo(exportReportModel.getTitlesList());
        Assertions.assertThat(liteCard.getDescriptionsList())
                .isEqualTo(exportReportModel.getDescriptionsList());
        Assertions.assertThat(liteCard.getTitleWithoutVendorList())
                .isEqualTo(exportReportModel.getTitleWithoutVendorList());
        Assertions.assertThat(liteCard.getParameterValueHypothesisList())
                .isEqualTo(exportReportModel.getParameterValueHypothesisList());
        Assertions.assertThat(liteCard.getParameterValues(0).getParamId())
                .isEqualTo(exportReportModel.getParameterValues(0).getParamId());
        compareParamValue(liteCard.getParameterValues(0), exportReportModel.getParameterValues(0));
        comparePicture(liteCard.getPictures(0), exportReportModel.getPictures(0));
    }

    private ExportReportModels.ExportReportModel buildExportReportModel() {
        return ExportReportModels.ExportReportModel.newBuilder()
                .setId(1L)
                .addPictures(ExportReportModels.Picture.newBuilder()
                        .setUrl("url").setWidth(10)
                        .setHeight(10)
                        .setUrlOrig("urlOrig")
                        .build())
                .setCsGumoful(CsGumofulOuterClass.CsGumoful.newBuilder().build())
                .addParameterValues(ExportReportModels.ParameterValue.newBuilder()
                        .setParamId(1L)
                        .addStrValue(localizedString("paramValue"))
                        .setBoolValue(true)
                        .setNumericValue("1")
                        .setValueType(MboParameters.ValueType.BOOLEAN)
                        .build())
                .addParameterValueHypothesis(
                        ExportReportModels.ParameterValueHypothesis.newBuilder().setParamId(1L).build())
                .addTitles(localizedString("title"))
                .addDescriptions(localizedString("description"))
                .addTitleWithoutVendor(localizedString("titleWithoutVendor"))
                .build();
    }

    private ExportReportModels.LocalizedString localizedString(String value) {
        return ExportReportModels.LocalizedString.newBuilder().setValue(value).build();
    }

    private void compareParamValue(CsSaasCardOuterClass.ParameterValue csParam,
                                   ExportReportModels.ParameterValue exportParam) {
        Assertions.assertThat(csParam).isEqualToComparingOnlyGivenFields(exportParam,
                "paramId", "boolValue", "numericValue",
                "optionId", "strValueList", "valueType");
    }

    private void comparePicture(CsSaasCardOuterClass.Picture csPicture,
                                   ExportReportModels.Picture exportPicture) {
        Assertions.assertThat(csPicture).isEqualToComparingOnlyGivenFields(exportPicture,
                "url", "width", "height", "urlOrig");
    }
}
