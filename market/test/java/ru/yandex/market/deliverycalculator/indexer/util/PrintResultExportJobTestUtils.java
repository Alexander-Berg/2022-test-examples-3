package ru.yandex.market.deliverycalculator.indexer.util;

import java.sql.ResultSet;
import java.util.function.Function;

import com.googlecode.protobuf.format.JsonFormat;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.util.functional.ExceptionfulFunction;
import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.storage.util.StorageUtils;

/**
 * Класс для распечатки результатов работы джобы.
 */
public final class PrintResultExportJobTestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrintResultExportJobTestUtils.class);

    private PrintResultExportJobTestUtils() {
        throw new UnsupportedOperationException();
    }

    public static void print(TransactionTemplate transactionTemplate,
                             DeliveryCalcProtos.FeedDeliveryOptionsResp message,
                             String... tables) {
        LOGGER.info(JsonFormat.printToString(message));
        StorageUtils.doInEntityManager(transactionTemplate, entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {
                for (String table : tables) {
                    ResultSet rs = connection.prepareStatement("select * from " + table).executeQuery();
                    StringBuilder builder = new StringBuilder();
                    builder.append(table).append("\n");
                    int columnCount = rs.getMetaData().getColumnCount();
                    print(builder, columnCount, throwingFunctionWrapper(v -> rs.getMetaData().getColumnName(v)));
                    builder.append("\n");
                    while (rs.next()) {
                        print(builder, columnCount, throwingFunctionWrapper(rs::getString));
                    }
                    builder.append("\n");
                    LOGGER.info(builder.toString());
                }
            });
        });
    }

    static <T, R> Function<T, R> throwingFunctionWrapper(ExceptionfulFunction<T, R, Exception> throwingFunction) {
        return i -> {
            try {
                return throwingFunction.apply(i);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        };
    }


    private static void print(StringBuilder builder, int count, Function<Integer, String> function) {
        for (int i = 1; i <= count; i++) {
            builder.append(function.apply(i));
            if (i != count) {
                builder.append(",");
            }
        }
        builder.append("\n");
    }
}
