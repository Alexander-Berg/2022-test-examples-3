package ru.yandex.market.delivery.transport_manager.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.sql.DataSource;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

public class DatabaseSetupTest extends AbstractContextualTest {
    @Autowired
    private DataSource dataSource;

    @Test
    @DatabaseSetup("/repository/movement/movement_test.xml")
    void getMovementTest() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(
                "select external_id, status, weight, volume from movement where id=1");
            ResultSet resultSet = preparedStatement.executeQuery();
            int cnt = 0;
            while (resultSet.next()) {
                cnt++;
                String externalId = resultSet.getString("external_id");
                String status = resultSet.getString("status");
                int weight = resultSet.getInt("weight");
                int volume = resultSet.getInt("volume");

                softly.assertThat(externalId).isEqualTo("movement1");
                softly.assertThat(status).isEqualTo("NEW");
                softly.assertThat(weight).isEqualTo(15);
                softly.assertThat(volume).isEqualTo(2);
            }
            softly.assertThat(cnt).isEqualTo(1);
        }
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_test.xml")
    void isInDbTest() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(
                "select id, status, logistic_point_id, planned_interval_start from transportation_unit");
            ResultSet resultSet = preparedStatement.executeQuery();
            int cnt = 0;
            while (resultSet.next()) {
                cnt++;
                long id = resultSet.getLong("id");
                String status = resultSet.getString("status");
                long logisticPointId = resultSet.getLong("logistic_point_id");
                LocalDateTime plannedDateTime = LocalDateTime.parse(
                    resultSet.getString("planned_interval_start"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                );

                softly.assertThat(id).isEqualTo(1);
                softly.assertThat(status).isEqualTo("NEW");
                softly.assertThat(logisticPointId).isEqualTo(2);
                softly.assertThat(plannedDateTime)
                    .isEqualTo(LocalDateTime.of(2020, 3, 1, 0, 0, 0));
            }
            softly.assertThat(cnt).isEqualTo(1);
        }
    }
}
