package utils;

import java.util.HashMap;

// TODO: 16/03/17 IT IS TMP VERSION, NEED IMPROVEMENT

public class FixtureRepository {
    private static FixtureLoader fixtureLoader = new FixtureLoader();

    private FixtureRepository() {
        throw new UnsupportedOperationException();
    }

    public static byte[] getSinglePickupPointListXml() {
        return fixtureLoader.getByteContent("/unit/russianpostpp/RussianSinglePostPickupPonit.xml");
    }

    public static byte[] getCurrencyRatesXml() {
        return fixtureLoader.getByteContent("/unit/CurrencyRates.xml");
    }

    public static byte[] getF103FormSample() {
        return fixtureLoader.getByteContent("/functional/getattacheddocs/f103_sample.pdf");
    }

    public static String getAttachedDocsValidRequest() {
        return fixtureLoader.getFileContent("/functional/getattacheddocs/get_attached_docs_valid_request.xml");
    }

    public static String getReferencePickupPointsEmptyRequest() {
        return fixtureLoader.getFileContent(
            "/functional/getreferencepickuppoints/getReferencePickupPointsEmptyRequest.xml"
        );
    }

    public static String getReferencePickupPointsRequestWithLocation() {
        return fixtureLoader.getFileContent(
            "/functional/getreferencepickuppoints/getReferencePickupPointsRequestWithLocation.xml"
        );
    }

    public static String getReferenceWarehousesRequest() {
        return fixtureLoader.getFileContent("/functional/getreferencewareouses/getReferenceWarehousesRequest.xml");
    }

    public static String getLetterContentSample() {
        return fixtureLoader.getFileContent("/functional/letterbuilder/lettercontent");
    }

    public static String getCancelOrderRequest(String yandexId, String deliveryId) {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("yandexId", yandexId);
        parameters.put("deliveryId", deliveryId);

        return fixtureLoader.getWithReplacing("/functional/cancelorder/ds_request_template.xml", parameters);
    }

    public static String getCancelOrderSuccessResponse() {
        return fixtureLoader.getFileContent("/functional/cancelorder/ds_success_response.xml");

    }

    public static String getEmsLabel() {
        return fixtureLoader.getFileContent("/unit/barcodes/ems_label.txt");
    }

    public static String getRmLabel() {
        return fixtureLoader.getFileContent("/unit/barcodes/rm_label.txt");
    }
}
