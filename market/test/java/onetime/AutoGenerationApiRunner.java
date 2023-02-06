package onetime;

import com.google.common.io.ByteStreams;
import com.google.protobuf.ByteString;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.ir.autogeneration_api.util.Conversion;
import ru.yandex.market.ir.http.AutoGenerationApi;
import ru.yandex.market.ir.http.AutoGenerationServiceStub;

import java.io.IOException;
import java.io.InputStream;

/**
 * Утилита (не авто-тест) для ручной проверки ручек autogeneration-api.
 */
@Ignore
public class AutoGenerationApiRunner {
//    private static final String HOST = "http://aida.yandex.ru:34540/"; // dev
    private static final String HOST = "http://cs-clusterizer01vt.market.yandex.net:34540/"; // testing

    private static final long CATEGORY_ID = 91529;         // Спорт и отдых/Велоспорт/Велосипеды
    private static final long VENDOR_ID = 1006808;         // TREK
    private static final long SOURCE_ID = 86224;
    private static final long REQUEST_ID = 555;

    private static final long TICKET_ID = 5214;
    private static final long VENDOR_MODEL_ID = 100100143829L;
    private static final String VENDOR_MODEL_TITLE = "Пробная модель. vzay";

    private static final long GURU_REQUEST_ID = 777;
    private static final long GURU_CATEGORY_ID = 90701;
    private static final long GURU_VENDOR_ID = 10717642;
    private static final long GURU_MODEL_ID = 1722577841L;
    private static final String GURU_MODEL_TITLE = "105-1200N (1,2 л)";
    private static final String GURU_MODEL_OLD_PICTURE_URL_SOURCE =
      "http://api.export.zoomos.by/img/item/1095352/0.jpg?key=yandex.ru-HknBlpRtyvf";
    private static final String GURU_MODEL_OLD_PICTURE_URL =
        "//avatars.mdst.yandex.net/get-mpic/4138/img_id3694522528599901181.jpeg/orig";


    private static final AutoGenerationServiceStub SERVICE = new AutoGenerationServiceStub();
    static {
        SERVICE.setHost(HOST);
    }

    @Test
    public void newTicket() {
        AutoGenerationApi.WriteTicketsRequest.Builder writeTicketsRequest =
            AutoGenerationApi.WriteTicketsRequest.newBuilder();
        writeTicketsRequest
            .setAuthor(AutoGenerationApi.Author.newBuilder()
                .setId(SOURCE_ID)
            )
            .addTicket(AutoGenerationApi.WriteTicketInput.newBuilder()
                .setTicketType(AutoGenerationApi.TicketType.CREATE)
                .setVendorModel(AutoGenerationApi.VendorModel.newBuilder()
                    .setCategoryId((int) CATEGORY_ID)
                    .setVendorId((int) VENDOR_ID)
                    .addTitle(Conversion.toLocalizedString(VENDOR_MODEL_TITLE))
                )
                .setRequestId(REQUEST_ID)
            );

        AutoGenerationApi.WriteTicketsResponse writeTicketsResponse = SERVICE.writeTickets(writeTicketsRequest.build());
        for (AutoGenerationApi.WriteTicketResult r : writeTicketsResponse.getResultList()) {
            System.out.println(r);
            System.out.println(r.getErrorMessage());
        }
    }

    @Test
    public void readTicket() {
        AutoGenerationApi.ReadTicketsResponse response = SERVICE.readTickets(
            AutoGenerationApi.ReadTicketsRequest.newBuilder().addTicketId(TICKET_ID).build());
        System.out.println(response.getTicket(0));
    }

    @Test
    public void newTicketForEditGuru() {
        AutoGenerationApi.WriteTicketsRequest.Builder writeTicketsRequest =
            AutoGenerationApi.WriteTicketsRequest.newBuilder();
        writeTicketsRequest
            .setAuthor(AutoGenerationApi.Author.newBuilder()
                .setId(SOURCE_ID)
            )
            .addTicket(AutoGenerationApi.WriteTicketInput.newBuilder()
                .setTicketType(AutoGenerationApi.TicketType.UPDATE)
                .setVendorModel(AutoGenerationApi.VendorModel.newBuilder()
                    .setCategoryId((int) GURU_CATEGORY_ID)
                    .setVendorId((int) GURU_VENDOR_ID)
                    .setGuruModelId(GURU_MODEL_ID)
                    .addTitle(Conversion.toLocalizedString(GURU_MODEL_TITLE))

                    .addImage(AutoGenerationApi.Image.newBuilder()
                        .setContentType("image/jpeg")
                        .setExistingContentUrl(GURU_MODEL_OLD_PICTURE_URL)
                        .setSourceUrl(GURU_MODEL_OLD_PICTURE_URL_SOURCE)
                    )
                )
                .setRequestId(GURU_REQUEST_ID)
            );

        AutoGenerationApi.WriteTicketsResponse writeTicketsResponse = SERVICE.writeTickets(writeTicketsRequest.build());
        for (AutoGenerationApi.WriteTicketResult r : writeTicketsResponse.getResultList()) {
            System.out.println(r);
            System.out.println(r.getErrorMessage());
        }
    }

    @Test
    public void editTicketPictures() {
        AutoGenerationApi.ReadTicketsResponse response = SERVICE.readTickets(
            AutoGenerationApi.ReadTicketsRequest.newBuilder().addTicketId(TICKET_ID).build());
        AutoGenerationApi.Ticket ticket = response.getTicket(0);

        AutoGenerationApi.WriteTicketsRequest.Builder writeTicketsRequest =
            AutoGenerationApi.WriteTicketsRequest.newBuilder();
        writeTicketsRequest
            .setAuthor(AutoGenerationApi.Author.newBuilder()
                .setId(SOURCE_ID)
            )
            .addTicket(AutoGenerationApi.WriteTicketInput.newBuilder()
                .setTicketType(AutoGenerationApi.TicketType.CREATE)
                .setTicketId(TICKET_ID)
                .setTicketVersion(ticket.getVersion()) // меняется при каждом запросе
                .setVendorModel(AutoGenerationApi.VendorModel.newBuilder()
                    .setCategoryId((int) CATEGORY_ID)
                    .setVendorId((int) VENDOR_ID)
                    .addTitle(Conversion.toLocalizedString(VENDOR_MODEL_TITLE))

                    .addImage(AutoGenerationApi.Image.newBuilder()
                        .setContentType("image/jpeg")
                        .setContent(loadPicture("a.jpg"))
                        .setSourceUrl("000.ru/a.jpg")
                    )
                    .addImage(AutoGenerationApi.Image.newBuilder()
                        .setContentType("image/jpeg")
                        .setContent(loadPicture("b.jpg"))
                        .setSourceUrl("000.ru/b.jpg")
                    )
                    .addImage(AutoGenerationApi.Image.newBuilder()
                        .setContentType("image/jpeg")
                        .setContent(loadPicture("c.jpg"))
                        .setSourceUrl("000.ru/c.jpg")
                    )
                    .addImage(AutoGenerationApi.Image.newBuilder()
                        .setContentType("image/jpeg")
                        .setContent(loadPicture("d.jpg"))
                        .setSourceUrl("000.ru/d.jpg")
                    )

                )
            );

        AutoGenerationApi.WriteTicketsResponse writeTicketsResponse = SERVICE.writeTickets(writeTicketsRequest.build());
        for (AutoGenerationApi.WriteTicketResult r : writeTicketsResponse.getResultList()) {
            System.out.println(r);
            System.out.println(r.getErrorMessage());
        }

    }

    private ByteString loadPicture(String fileName) {
        try {
            InputStream imageStream = this.getClass().getClassLoader()
                .getResourceAsStream(fileName);
            return ByteString.copyFrom(ByteStreams.toByteArray(imageStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
