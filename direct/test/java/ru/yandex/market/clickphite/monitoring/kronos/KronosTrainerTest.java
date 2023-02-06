package ru.yandex.market.clickphite.monitoring.kronos;

import com.google.common.base.Joiner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.clickhouse.ClickHouseSource;
import ru.yandex.market.clickhouse.ClickhouseTemplate;
import ru.yandex.market.clickhouse.HttpResultRow;
import ru.yandex.market.clickhouse.HttpRowCallbackHandler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"/>
 * @date 29/10/15
 */
@Ignore
public class KronosTrainerTest {

    private ClickhouseTemplate clickhouseTemplate;

    @Before
    public void setUp() throws Exception {
        /*
        ssh -f -N -L 8123:health-house.market.yandex.net:8123 public01e.market.yandex.net
         */
        clickhouseTemplate = new ClickhouseTemplate(new ClickHouseSource("localhost", "market", "ro"));
    }

    @Test
    public void testKronos() throws Exception {
        List<double[]> trainPoints = new ArrayList<>();
        double[] data = getPoints("today() -1");
        double[] fackup = getPoints("'2015-10-08'");
//        trainPoints.add(data);
//        trainPoints.add(getPoints("today() -1"));
//        trainPoints.add(getPoints("today() -2"));
        trainPoints.add(getPoints("today() -3"));
        trainPoints.add(getPoints("today() -4"));

        trainPoints.add(getPoints("today() -7"));
        trainPoints.add(getPoints("today() -8"));
        trainPoints.add(getPoints("today() -9"));
        trainPoints.add(getPoints("today() -10"));
        trainPoints.add(getPoints("today() -11"));

        trainPoints.add(getPoints("today() -15"));
        trainPoints.add(getPoints("today() -22"));
        trainPoints.add(getPoints("today() -29"));
        KronosModel model = KronosTrainer.estimateConfidenceIntervals(trainPoints, 3, 0.35);

        BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/andreevdm/tmp/kronos/money"));
        out("data", data, writer);
        out("fackup", fackup, writer);
        out("extTop", model.getExtTop(), writer);
        out("top", model.getTop(), writer);
        out("mean", model.getMean(), writer);
        out("average", model.getAverage(), writer);
        out("bottom", model.getBottom(), writer);
        out("extBottom", model.getExtBottom(), writer);
        writer.close();

    }

    private void out(String name, double[] array, BufferedWriter writer) throws IOException {
        System.out.println(name + " = [" + Joiner.on(", ").join(Arrays.stream(array).iterator()) + "];");
        writer.write(name + " = [" + Joiner.on(", ").join(Arrays.stream(array).iterator()) + "];");
        writer.newLine();

    }


    private double[] getPoints(String date) {
        double[] points = new double[288];
//        double[] points = new double[144];
        Arrays.fill(points, Double.NaN);
        for (int i = 0; i < points.length; i++) {
            points[i] = Double.NaN;
        }


        clickhouseTemplate.query(
//            "select ts, chips as value, (countIf(filter = 0) - countIf(filter > 0)) / 1000 as chips " +
            "select ts, chips as value, (sumIf(price, filter = 0) - sumIf(price, filter > 0)) / 1000 as chips " +
                "from mstat_clicks where date = " + date + " and state != 3 " +
                "group by toHour(toDateTime(timestamp)) * 12 + intDiv(toMinute(toDateTime(timestamp)), 5) as ts " +
                "order by ts",
            new HttpRowCallbackHandler() {
                @Override
                public void processRow(HttpResultRow rs) throws SQLException {
                    points[rs.getInt("ts")] = rs.getDouble("value");
                }
            }
        );
        return points;


    }
}

