package ru.yandex.market.wms.auth.config.filters;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.filter.IColumnFilter;

public class UserColumnFilter implements IColumnFilter {

    @Override
    public boolean accept(String tableName, Column column) {
        return !(tableName.equalsIgnoreCase("e_user")
                && (column.getColumnName().equalsIgnoreCase("e_user_id") ||
                column.getColumnName().equalsIgnoreCase("user_data_id")
        )) && !(tableName.equalsIgnoreCase("e_sso_user")
                && column.getColumnName().equalsIgnoreCase("e_sso_user_id")
        ) && !(tableName.equalsIgnoreCase("e_sso_user_role")
                && (column.getColumnName().equalsIgnoreCase("e_sso_user_role_id") ||
                column.getColumnName().equalsIgnoreCase("sso_user_id") ||
                column.getColumnName().equalsIgnoreCase("sso_role_id")
        )) && !(tableName.equalsIgnoreCase("user_data")
                && column.getColumnName().equalsIgnoreCase("user_data_id"
        )) && !(tableName.equalsIgnoreCase("user_pref_instance")
                && column.getColumnName().equalsIgnoreCase("user_pref_instance_id"));
    }
}
