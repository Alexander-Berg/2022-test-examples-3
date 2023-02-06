package ru.yandex.market.mbo.licensor2.scheme;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ayratgdl
 * @date 15.01.18
 */
public class LicensorVendorConstraintDAOMock implements LicensorVendorConstraintDAO {
    List<LicensorVendorConstraint> lvConstraints = new ArrayList<>();

    @Override
    public void createConstraint(LicensorVendorConstraint constraint) {
        lvConstraints.add(constraint);
    }

    @Override
    public void deleteConstraint(LicensorVendorConstraint pattern) {
        lvConstraints.removeIf(c -> LicensorVendorConstraint.keyEquals(c, pattern));
    }

    @Override
    public List<LicensorVendorConstraint> getAllConstraints() {
        return lvConstraints.stream().map(LicensorVendorConstraint::copy).collect(Collectors.toList());
    }
}
