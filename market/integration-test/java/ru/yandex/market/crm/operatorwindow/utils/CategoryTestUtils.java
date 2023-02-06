package ru.yandex.market.crm.operatorwindow.utils;

import java.util.Map;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.operatorwindow.jmf.entity.TicketCategory;
import ru.yandex.market.crm.operatorwindow.jmf.entity.TicketCategoryPriority;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.script.ScriptContextVariablesService;
import ru.yandex.market.jmf.utils.Maps;

import static ru.yandex.market.jmf.logic.def.HasTitle.TITLE;

@Component
public class CategoryTestUtils {
    private final BcpService bcpService;
    private final ScriptContextVariablesService scriptContextVariablesService;
    private final DbService dbService;

    public CategoryTestUtils(BcpService bcpService,
                             ScriptContextVariablesService scriptContextVariablesService,
                             DbService dbService) {
        this.bcpService = bcpService;
        this.scriptContextVariablesService = scriptContextVariablesService;
        this.dbService = dbService;
    }

    public TicketCategoryPriority createCategoryPriority(String code, int level) {
        return bcpService.create(TicketCategoryPriority.FQN, Map.of(
                TicketCategoryPriority.CODE, code,
                TITLE, Randoms.string(),
                TicketCategoryPriority.LEVEL, level
        ));
    }

    public TicketCategory createTicketCategory(String brandCode, TicketCategoryPriority categoryPriority) {

        Brand brand = dbService.getByNaturalId(Brand.FQN, Brand.CODE, brandCode);
        scriptContextVariablesService.addContextVariable(
                ScriptContextVariablesService.ContextVariables.CARD_OBJECT,
                brand
        );


        var attrs = Maps.<String, Object>of(
                TicketCategory.CODE, Randoms.string(),
                TicketCategory.TITLE, Randoms.string(),
                TicketCategory.CATEGORY_PRIORITY, categoryPriority);

        return bcpService.create(TicketCategory.FQN, attrs);
    }

    public TicketCategoryPriority getCategoryPriority(String code) {
        return dbService.getByNaturalId(TicketCategoryPriority.FQN, TicketCategoryPriority.CODE, code);
    }
}
