package ru.yandex.market.load.admin.promoWriter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.Promo.Promo;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.YTreeObjectSerializerFactory;
import ru.yandex.market.load.admin.AbstractFunctionalTest;
import ru.yandex.market.load.admin.entity.OfferSelectResponse;
import ru.yandex.market.load.admin.entity.PromoConfiguration;
import ru.yandex.market.load.admin.entity.PromoRow;
import ru.yandex.market.load.admin.service.FlashPromoBuilder;
import ru.yandex.yt.ytclient.proxy.TableReader;
import ru.yandex.yt.ytclient.proxy.TableWriter;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.proxy.YtCluster;
import ru.yandex.yt.ytclient.proxy.request.ReadTable;
import ru.yandex.yt.ytclient.proxy.request.WriteTable;
import ru.yandex.yt.ytclient.rpc.RpcCredentials;

@Disabled
public class PromoWriterTest extends AbstractFunctionalTest
{
    @Value("promo.clusters")
    private List<String> ytClusters = List.of();
    final private List<String> offers = List.of();
    @Value("promo.table")
    private String table = "";
    @Value("yt.token")
    private String ytToken = "";
    @Value("yt.user")
    private String ytUser;


    private YtClient getReadClient() {
        return YtClient.builder()
                .setClusters(ytClusters.stream()
                        .map(YtCluster::new)
                        .collect(Collectors.toList()))
                .setRpcCredentials(new RpcCredentials(ytUser, ytToken))
                .build();
    }


    private List<YtClient> getWriteClients() {
        final RpcCredentials rpcCredentials = new RpcCredentials(ytUser, ytToken);
        return ytClusters.stream().map(YtCluster::new)
                .map(Collections::singletonList)
                .map(cluster -> YtClient.builder().setClusters(cluster))
                .map(builder -> builder.setRpcCredentials(rpcCredentials))
                .map(YtClient.Builder::build)
                .toList();
    }


    @Test
    public void test() throws Exception {
        final YPath tablePath = YPath.simple(table);

        final List<PromoRow> rows = getPromoRows(tablePath);

        final ArrayList<OfferSelectResponse> offerSelectResponses = new ArrayList<>();
        for (String offer : offers) {
            try (final FileInputStream stream = new FileInputStream(offer);
                 final InputStreamReader streamReader = new InputStreamReader(stream);
                 final BufferedReader bufferedReader = new BufferedReader(streamReader)
            ) {
                while (bufferedReader.ready()) {
                    final String tskvLine = bufferedReader.readLine();
                    final String[] split = tskvLine.split("\t");
                    if (split.length > 0 && split[0].equals("tskv")) {
                        final OfferSelectResponse response = OfferSelectResponse.builder().build();
                        for (String s : split) {
                            final String[] strings = s.split("=");
                            switch (strings[0]) {
                                case "feedId" -> response.setFeedId(Integer.parseInt(strings[1]));
                                case "offerId" -> response.setOfferId(strings[1]);
                            }
                        }

                        offerSelectResponses.add(response);
                    }
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }


        System.out.println(offerSelectResponses.size());

        final List<OfferSelectResponse> offerSelectResponses1 = offerSelectResponses.stream().toList();


        final FlashPromoBuilder flashPromoBuilder = new FlashPromoBuilder();
        final LocalDateTime startDate = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        final LocalDateTime endDate = startDate.plusDays(5);
        final Optional<PromoRow> flashPromo = flashPromoBuilder.build(offerSelectResponses1,
                PromoConfiguration.Config.Promos.builder()
                        .blueFlash(
                                PromoConfiguration.Config.Promos.BlueFlash.builder()
                                        .id("#load-testing-flash")
                                        .price(100)
                                        .build()
                        )
                        .build(),
                startDate
        );

        if (flashPromo.isEmpty()) {
            return;
        }
        writeRows(tablePath, List.of(flashPromo.get()), rows);
    }

    private void writeRows(YPath tablePath, List<PromoRow> newRows, List<PromoRow> oldRows) throws IOException {
        final Map<String, PromoRow> newRowsById =
                newRows.stream().collect(Collectors.toMap(r -> r.getPromo().getShopPromoId(), Function.identity()));
        final Map<String, Set<PromoRow>> oldRowsById = oldRows
                .stream().collect(Collectors.toMap(r -> r.getPromo().getShopPromoId(),
                Collections::singleton, (set1, set2) -> {
                    final HashSet<PromoRow> promoRows = new HashSet<>(set1);
                    promoRows.addAll(set2);
                    return promoRows;
                }));
        final ArrayList<PromoRow> promoRows = new ArrayList<>();

        for (Map.Entry<String, PromoRow> stringSetEntry : newRowsById.entrySet()) {
            final PromoRow value = stringSetEntry.getValue();
            promoRows.add(value);
            final Set<PromoRow> oldRows1 = oldRowsById.get(stringSetEntry.getKey());
            if (oldRows1 != null){
                final long startDate = value.getPromo().getStartDate();
                for (PromoRow promoRow : oldRows1) {
                    final Promo.PromoDetails promo = promoRow.getPromo();
                    if (promo.getEndDate() > startDate){
                        promoRow.setPromo(
                                Promo.PromoDetails.newBuilder(promo)
                                        .setEndDate(startDate)
                                        .build()
                        );
                    }
                    promoRows.add(promoRow);
                }
                oldRowsById.remove(stringSetEntry.getKey());
            }
        }

        for (Set<PromoRow> value : oldRowsById.values()) {
            promoRows.addAll(value);
        }


        final List<PromoRow> filteredRows =
                promoRows.stream().filter(promoRow -> promoRow.getPromo().getStartDate() > LocalDateTime.now()
                        .toEpochSecond(ZoneOffset.UTC))
                .toList();

        final WriteTable<PromoRow> writeTable = new WriteTable<>(tablePath,
                YTreeObjectSerializerFactory.forClass(PromoRow.class));
        final List<YtClient> writeClients = getWriteClients();
        for (YtClient writeClient :
                writeClients) {
            final TableWriter<PromoRow> writer = writeClient.writeTable(writeTable).join();
            writer.readyEvent().join();
            writer.write(filteredRows);
            writer.close().join();
        }
    }

    private List<PromoRow> getPromoRows(YPath tablePath) throws Exception {
        final ReadTable<PromoRow> promoRowReadTable = new ReadTable<>(tablePath,
                YTreeObjectSerializerFactory.forClass(PromoRow.class));

        final YtClient readClient = getReadClient();
        final TableReader<PromoRow> reader = readClient.readTable(promoRowReadTable).join();
        reader.readyEvent().join();
        final List<PromoRow> rows = reader.read();
        reader.close().join();
        return rows;
    }
}
