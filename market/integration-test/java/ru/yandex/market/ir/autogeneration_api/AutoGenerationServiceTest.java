package ru.yandex.market.ir.autogeneration_api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import javax.annotation.Resource;

import com.google.common.io.ByteStreams;
import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.ir.autogeneration_api.util.Conversion;
import ru.yandex.market.ir.http.AutoGenerationApi;
import ru.yandex.market.ir.http.AutoGenerationService;

/**
 * Created by catfish on 10.02.2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:autogeneration-api/beans-test.xml"})
public class AutoGenerationServiceTest {

    private static final long CATEGORY_ID = 91529;         // Спорт и отдых/Велоспорт/Велосипеды
    private static final long VENDOR_ID = 1006808;         // TREK
    private static final long GURU_MODEL_ID = 13042950;    // Silque SSL Womens (2016)

    private static final Random RANDOM = new Random();
    private static final String TITLE = "Autogeneration-API service test-" + RANDOM.nextLong();


    @Resource(name = "autoGenerationService")
    private AutoGenerationService autoGenerationService;


    private void testWriteTickets(boolean assignGuruModelId) {

        AutoGenerationApi.WriteTicketsRequest.Builder writeRequest = AutoGenerationApi.WriteTicketsRequest.newBuilder();

        AutoGenerationApi.Author.Builder author = AutoGenerationApi.Author.newBuilder();
        AutoGenerationApi.WriteTicketInput.Builder ticket = AutoGenerationApi.WriteTicketInput.newBuilder();
        AutoGenerationApi.VendorModel.Builder model = AutoGenerationApi.VendorModel.newBuilder();

        ticket.setTicketType(AutoGenerationApi.TicketType.CREATE);

        model.setCategoryId((int) CATEGORY_ID);
        model.setVendorId((int) VENDOR_ID);
        model.addTitle(Conversion.toLocalizedString(TITLE));

        if (assignGuruModelId) {
            model.setGuruModelId(GURU_MODEL_ID);
        }

        ticket.setVendorModel(model.build());

        writeRequest.setAuthor(author.build());
        writeRequest.addTicket(ticket.build());

        AutoGenerationApi.WriteTicketsResponse writeResponse = autoGenerationService.writeTickets(writeRequest.build());
        AutoGenerationApi.WriteTicketResult writeResult = writeResponse.getResult(0);

        Assert.assertEquals(writeResult.getErrorMessage(), AutoGenerationApi.Status.SUCCESS, writeResult.getStatus());


        writeRequest.clear();

        ticket.setTicketId(writeResult.getTicket().getId());
        ticket.setTicketVersion(writeResult.getTicket().getVersion());

        writeRequest.setAuthor(author.build());
        writeRequest.addTicket(ticket.build());

        writeResponse = autoGenerationService.writeTickets(writeRequest.build());
        writeResult = writeResponse.getResult(0);

        Assert.assertEquals(writeResult.getErrorMessage(), AutoGenerationApi.Status.SUCCESS, writeResult.getStatus());
    }

    @Test
    public void testWriteTickets() {
        testWriteTickets(false);
    }

    @Test
    public void testWriteTicketsWithGuruModel() {
        testWriteTickets(true);
    }

    @Test
    public void testWriteTicketWithImage() {

        AutoGenerationApi.WriteTicketsRequest.Builder writeRequest = AutoGenerationApi.WriteTicketsRequest.newBuilder();

        AutoGenerationApi.Author.Builder author = AutoGenerationApi.Author.newBuilder();
        AutoGenerationApi.WriteTicketInput.Builder ticket = AutoGenerationApi.WriteTicketInput.newBuilder();
        AutoGenerationApi.VendorModel.Builder model = AutoGenerationApi.VendorModel.newBuilder();

        ticket.setTicketType(AutoGenerationApi.TicketType.CREATE);

        model.setCategoryId((int) CATEGORY_ID);
        model.setVendorId((int) VENDOR_ID);
        model.setGuruModelId(GURU_MODEL_ID);
        model.addTitle(Conversion.toLocalizedString(TITLE));
        model.setComment(Conversion.toLocalizedString("test comment"));

        AutoGenerationApi.Image.Builder bigImage = AutoGenerationApi.Image.newBuilder();
        bigImage.setIndex(0);
        bigImage.setSourceUrl("example.org/notebook_xps15_bigpicture.jpg");
        bigImage.setContentType("jpg");
        bigImage.setContent(ByteString.copyFrom(loadPicture("notebook_xps15_bigpicture.jpg")));
        model.addImage(bigImage);

        AutoGenerationApi.Image.Builder xlImage = AutoGenerationApi.Image.newBuilder();
        xlImage.setIndex(1);
        xlImage.setSourceUrl("example.org/notebook_xps15_xlpicture.jpg");
        xlImage.setContentType("jpg");
        xlImage.setContent(ByteString.copyFrom(loadPicture("notebook_xps15_xlpicture.jpg")));
        model.addImage(xlImage);

        ticket.setVendorModel(model.build());

        writeRequest.setAuthor(author.build());
        writeRequest.addTicket(ticket.build());

        AutoGenerationApi.WriteTicketsResponse writeResponse = autoGenerationService.writeTickets(writeRequest.build());
        AutoGenerationApi.WriteTicketResult writeResult = writeResponse.getResult(0);

        Assert.assertEquals(writeResult.getErrorMessage(), AutoGenerationApi.Status.SUCCESS, writeResult.getStatus());
    }

    private byte[] loadPicture(String fileName) {
        try {
            InputStream imageStream = this.getClass().getClassLoader()
                .getResourceAsStream("autogeneration-api/" + fileName);
            return ByteStreams.toByteArray(imageStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
