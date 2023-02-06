package ru.yandex.market.yql_test.service;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.yql_query_service.service.QryTblService;
import ru.yandex.market.yql_query_service.service.TableNameResolverService;
import ru.yandex.market.yql_test.YqlTablePathConverter;

@Service
@Primary
public class QryTblTestService extends QryTblService {

    private final QuerySuffixesService querySuffixesService;
    private final YqlTablePathConverter yqlTablePathConverter;

    public QryTblTestService(TableNameResolverService tableNameResolverService,
                             QuerySuffixesService querySuffixesService,
                             YqlTablePathConverter yqlTablePathConverter) {
        super(tableNameResolverService);
        this.querySuffixesService = querySuffixesService;
        this.yqlTablePathConverter = yqlTablePathConverter;
    }

    @Override
    public String processTableName(String tableName) {
        if (querySuffixesService.hasSuffixes()) {
            return tableName;
        } else {
            return yqlTablePathConverter.toTestPath(tableName).toString();
        }
    }

    public YPath getTestDir() {
        return yqlTablePathConverter.getTestYtDir();
    }

    @Override
    public String withInline() {
        return " with inline ";
    }
}
