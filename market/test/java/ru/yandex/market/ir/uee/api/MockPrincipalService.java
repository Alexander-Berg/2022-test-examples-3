package ru.yandex.market.ir.uee.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import ru.yandex.market.ir.uee.repository.UserRepo;
import ru.yandex.market.ir.uee.api.service.PrincipalService;
import ru.yandex.market.ir.uee.jooq.generated.tables.records.StaffUserRecord;

import static ru.yandex.market.ir.uee.jooq.generated.Tables.STAFF_USER;

@Service
@Primary
public class MockPrincipalService implements PrincipalService {
    @Autowired
    UserRepo userRepo;

    @Override
    public boolean isModerator() {
        return true;
    }

    @Override
    public String getPrincipalName() {
        return "test";
    }

    @Override
    public StaffUserRecord getPrincipalRecord() {
        StaffUserRecord any = userRepo.findAny(STAFF_USER);
        if (any == null) {
            any = new StaffUserRecord().setLogin(getPrincipalName()).setEnableNotifications(true);
            return userRepo.insertRecord(STAFF_USER, any);
        }
        return any;
    }

    @Override
    public Integer getPrincipalUserId() {
        return getPrincipalRecord().getId();
    }

    @Override
    public List<String> getRoles() {
        return List.of("admin");
    }

    @Override
    public List<String> getRoles(String username) {
        return List.of("admin");
    }
}
