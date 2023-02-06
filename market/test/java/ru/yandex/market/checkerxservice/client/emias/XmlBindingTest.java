package ru.yandex.market.checkerxservice.client.emias;

import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.checkerxservice.TestUtils;
import ru.yandex.market.checkerxservice.chekservice.emias.erecipeservice.v1.types.DirectoryItem;
import ru.yandex.market.checkerxservice.chekservice.emias.erecipeservice.v1.types.GetPrescriptionsResponseType;
import ru.yandex.market.checkerxservice.chekservice.emias.erecipeservice.v1.types.Medicine;
import ru.yandex.market.checkerxservice.chekservice.emias.erecipeservice.v1.types.PrescriptionItem;
import ru.yandex.market.checkerxservice.chekservice.emias.erecipeservice.v1.types.Qty;
import ru.yandex.market.checkerxservice.chekservice.emias.erecipeservice.v1.types.jaxbcustom.XMLGregorianCalendarAdapter;

public class XmlBindingTest {
    private static final XMLGregorianCalendarAdapter xmlDateAdapter = new XMLGregorianCalendarAdapter();

    @Test
    public void unmarshalGetPrescriptionResponse() {
        InputStream responseXmlStream = TestUtils.getResourceStream("xml/prescription_response_1.xml");

        //создание объекта UnMarshaller, который выполняет сериализацию
        JAXBContext context;
        GetPrescriptionsResponseType response = null;
        try {
            context = JAXBContext.newInstance(GetPrescriptionsResponseType.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            // сама сериализация
            response = (GetPrescriptionsResponseType) unmarshaller.unmarshal(responseXmlStream);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        GetPrescriptionsResponseType expected = null;
        try {
            expected = createTestResponse();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
        // На самом деле тест не совсем честный и учитывает ещё и реализацию equals() GetPrescriptionsResponseType
        // и классов внутри него
        Assert.assertEquals(expected, response);
    }

    private static GetPrescriptionsResponseType createTestResponse() throws Exception {
        GetPrescriptionsResponseType response = new GetPrescriptionsResponseType();
        response.setRequestGUID("46c6c89f-534a-410d-b247-c0fad82fc809");
        response.setResponseGUID("b50044a2-8f20-45fb-9d96-b7d1d2031c64");
        PrescriptionItem item = new PrescriptionItem();
        item.setControlType("normal");
        item.setExpirationDate(xmlDateAdapter.unmarshal("2023-01-24"));
        item.setPrescriptedByTradename(false);
        item.setPrescriptionDate(xmlDateAdapter.unmarshal("2022-01-24"));
        item.setPrescriptionNumber("00Д4519354490");
        item.setPrescriptionType("commercial");
        item.setRp("Acetylsalicylici acidi tab. obduct. 100 mg");
        item.setSigna("Применять  в течение 14 дней, 1 раз в день по 100 мг");
        item.setValidity("1 год");
        item.setERecipe(true);

        Qty qty = new Qty();
        qty.setDosage("100 мг");
        qty.setNumero("14");
        qty.setUnit("таблетки");
        item.setQty(qty);

        Medicine medicine = new Medicine();
        medicine.setDrugsShortName("Аспирин Кардио табл. п/об. к/раств. 100 мг");
        medicine.setFormName("таблетки покрытые кишечнорастворимой оболочкой");
        medicine.setINNName("Ацетилсалициловая кислота");
        DirectoryItem dir1 = new DirectoryItem();
        dir1.setDirCode("EMIAS_MNN");
        dir1.setDrugID("86");
        DirectoryItem dir2 = new DirectoryItem();
        dir2.setDirCode("PRESCRIPTION_MODE");
        dir2.setDirValue("unknown");
        medicine.setDirectory(List.of(dir1, dir2));

        item.setMedicineInfo(medicine);

        response.setPrescription(List.of(item));

        return response;
    }
}
