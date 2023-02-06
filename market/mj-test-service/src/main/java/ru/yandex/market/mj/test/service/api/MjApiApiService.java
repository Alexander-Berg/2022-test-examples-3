package ru.yandex.market.mj.test.service.api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.mj.generated.server.api.MjApiApiDelegate;

@Component
public class MjApiApiService implements MjApiApiDelegate {
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public ResponseEntity<String> getStringGet(String key) {
        String value = jdbcTemplate.query("SELECT value FROM test WHERE key = '" + key + "'",
                new ResultSetExtractor<String>() {
                    @Override
                    public String extractData(ResultSet rs) throws SQLException, DataAccessException {
                        while (rs.next()) {
                            return rs.getString(1);
                        }
                        return null;
                    }
                }
        );

        if (value == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(value);
    }

    @Override
    public ResponseEntity<String> helloWorldGet(String name) {
        return ResponseEntity.ok(String.format("Hello %s!", name));
    }

    @Override
    public ResponseEntity<String> putStringPost(String key, String value) {
        Map<String, String> params = new HashMap<>();

        params.put("key", key);
        params.put("value", value);

        int rowsUpdated = jdbcTemplate.update(
                "INSERT INTO test (key, value) VALUES (:key, :value)",
                params
        );

        if (rowsUpdated == 0) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok(value);
    }
}
