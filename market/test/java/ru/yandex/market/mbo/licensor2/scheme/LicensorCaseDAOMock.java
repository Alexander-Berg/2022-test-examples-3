package ru.yandex.market.mbo.licensor2.scheme;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ayratgdl
 * @date 14.01.18
 */
public class LicensorCaseDAOMock implements LicensorCaseDAO {
    List<LicensorCase> licensorCases = new ArrayList<>();

    @Override
    public void createLicensorCase(LicensorCase licensorCase) {
        licensorCases.add(licensorCase);
    }

    @Override
    public void updateLicensorCase(LicensorCase licensorCase) {
        if (getLicensorCase(licensorCase.getLfp()) == null) {
            throw new RuntimeException(
                "Error: update non-existent licensor case. LFP: " + licensorCase.getLfp() + "."
            );
        }
        deleteLicensorCase(licensorCase.getLfp());
        createLicensorCase(licensorCase);
    }

    @Override
    public void deleteLicensorCase(LFP pattern) {
        licensorCases.removeIf(licensorCase -> licensorCase.getLfp().equals(pattern));
    }

    @Override
    public List<LicensorCase> getAllLicensorCases() {
        return licensorCases.stream().map(LicensorCase::copy).collect(Collectors.toList());
    }

    private LicensorCase getLicensorCase(LFP pattern) {
        return licensorCases.stream()
            .filter(licensorCase -> licensorCase.getLfp().equals(pattern))
            .map(LicensorCase::copy)
            .findFirst()
            .orElse(null);
    }
}
