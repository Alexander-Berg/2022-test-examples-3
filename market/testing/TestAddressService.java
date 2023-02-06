package ru.yandex.market.pers.notify.testing;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestAddressService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<String> getTestUUIDs() {
        return jdbcTemplate.queryForList("SELECT UUID FROM TEST_UUID", String.class);
    }

    public List<String> getTestEmails() {
        return jdbcTemplate.queryForList("SELECT EMAIL FROM TEST_EMAIL", String.class);
    }

    public List<Long> getTestUids() {
        return jdbcTemplate.queryForList("SELECT UID FROM TEST_UID", Long.class);
    }

    public void addTestEmail(String email) {
        jdbcTemplate.update("INSERT IGNORE INTO TEST_EMAIL (EMAIL) VALUES (?)", email);
    }

    public void addTestUUID(String uuid) {
        jdbcTemplate.update("INSERT IGNORE INTO TEST_UUID (UUID) VALUES (?)", uuid);
    }

    public void addTestUid(Long uid) {
        jdbcTemplate.update("INSERT IGNORE INTO TEST_UID (UID) VALUES (?)", uid);
    }

    public void deleteTestEmail(String email) {
        jdbcTemplate.update("DELETE FROM TEST_EMAIL WHERE EMAIL = ?", email);
    }

    public void deleteTestUUID(String uuid) {
        jdbcTemplate.update("DELETE FROM TEST_UUID WHERE UUID = ?", uuid);
    }

    public void deleteTestUid(Long uid) {
        jdbcTemplate.update("DELETE FROM TEST_UID WHERE UUID = ?", uid);
    }

}
