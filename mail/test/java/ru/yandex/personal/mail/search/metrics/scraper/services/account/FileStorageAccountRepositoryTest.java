package ru.yandex.personal.mail.search.metrics.scraper.services.account;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileSystemUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageAccountRepositoryTest {
    private static final String SYS = "sys";
    private static final String ACC = "acc";
    private static final String PART = "credentials";
    private static final AccountProperties PROPS = new AccountProperties();

    private Path baseDir = Files.createTempDirectory("test");
    private ObjectMapper mapper = new ObjectMapper();

    FileStorageAccountRepositoryTest() throws IOException {
    }

    @AfterEach
    void cleanUp() {
        FileSystemUtils.deleteRecursively(baseDir.toFile());
    }

    @Test
    void writeAccount() {
        AccountRepository ar = new FileStorageAccountRepository(baseDir.toString(), mapper);

        AccountDraft draft = new AccountDraft(SYS, ACC, PROPS);
        draft.addCredential(PART, new byte[100]);
        ar.writeAccount(draft);

        assertTrue(Files.exists(baseDir.resolve(SYS).resolve(ACC).resolve(PART)));
    }

    @Test
    void hasNoAccount() {
        AccountRepository ar = new FileStorageAccountRepository(baseDir.toString(), mapper);

        assertFalse(ar.hasAccount(SYS, ACC));
    }

    @Test
    void hasAccount() {
        AccountRepository ar = new FileStorageAccountRepository(baseDir.toString(), mapper);

        AccountDraft draft = new AccountDraft(SYS, ACC, PROPS);
        draft.addCredential(PART, new byte[100]);
        ar.writeAccount(draft);

        assertTrue(ar.hasAccount(SYS, ACC));
    }

    @Test
    void deleteAccount() {
        AccountRepository ar = new FileStorageAccountRepository(baseDir.toString(), mapper);

        AccountDraft draft = new AccountDraft(SYS, ACC, PROPS);
        draft.addCredential(PART, new byte[100]);
        ar.writeAccount(draft);

        ar.deleteAccount(SYS, ACC);

        assertFalse(ar.hasAccount(SYS, ACC));
    }

    @Test
    void deleteNoAccount() {
        AccountRepository ar = new FileStorageAccountRepository(baseDir.toString(), mapper);

        assertThrows(NoSuchElementException.class, () -> ar.deleteAccount(SYS, ACC));
    }

    @Test
    void getPath() {
        AccountRepository ar = new FileStorageAccountRepository(baseDir.toString(), mapper);

        AccountDraft draft = new AccountDraft(SYS, ACC, PROPS);
        draft.addCredential(PART, new byte[100]);
        ar.writeAccount(draft);

        assertEquals(baseDir.resolve(SYS).resolve(ACC), ar.getAccountPath(SYS, ACC));
    }
}
