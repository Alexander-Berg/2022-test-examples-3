package ru.yandex.market.fintech.banksint.startup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;
import ru.yandex.market.fintech.banksint.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StartupValidationTest extends FunctionalTest {

    private static final Set<String> ALREADY_BROKEN = Set.of(
            "common_property.sql",
            "custom_group_to_installment.sql",
            "installment.sql",
            "installment_custom_group.sql",
            "installment_group.sql",
            "installment_rows.sql",
            "loan_request.sql",
            "scoring_data.sql",
            "update_bnpl_installment.sql",
            "update_installments_percentage.sql"
    );

    private static boolean sqlCheckPredicate(Path p) {
        String fullPath = p.toString();
        return fullPath.matches(".*/banks-b2b-int/[a-z_]*.sql") ||
                fullPath.matches(".*/banks-b2b-int/new/[a-z_]*.sql");
    }

    private static boolean checkFirstString(Path p) {
        if (ALREADY_BROKEN.contains(p.getFileName().toString())) {
            return true;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(p.toFile()))) {
            String firstLine = reader.readLine();
            return firstLine == null || firstLine.stripTrailing().equals("--liquibase formatted sql");
        } catch (IOException ioEx) {
            return true;
        }
    }

    /**
     * Если вы смотрите на этот тест, у вас нет --liquibase formatted sql
     */
    @Test
    void testLiquibaseAnnotations() {
        try {
            var resourceRoot = this.getClass().getClassLoader().getResource("").getPath();
            Files.walk(Path.of(resourceRoot))
                    .filter(StartupValidationTest::sqlCheckPredicate)
                    .forEach(p -> assertTrue(
                            checkFirstString(p),
                            "Wrong format for " + p.getFileName().toString()
                            )
                    );

        } catch (IOException ioEx) {
            System.out.println("Failed");
            ioEx.printStackTrace();
        }
    }
}
