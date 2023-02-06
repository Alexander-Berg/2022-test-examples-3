package ui_tests.src.test.java.Classes;

import java.util.List;
import java.util.Objects;

public class Employee {

    private String alias;
    private String title;

    private List<String> ou;
    private List<String> roles;
    private List<String> services;
    private List<String> teams;
    Boolean voximplantEnabled;

    /**
     * Получить псевдоним
     *
     * @return
     */
    public String getAlias() {
        return alias;
    }

    /**
     * указать псевдоним
     *
     * @param alias
     * @return
     */
    public Employee setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    /**
     * Получить Название
     *
     * @return
     */
    public String getTitle() {
        return title;
    }

    /**
     * указать Название
     *
     * @param title
     * @return
     */
    public Employee setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Получить подразделение
     *
     * @return
     */
    public List<String> getOu() {
        return ou;
    }

    /**
     * указать подразделение
     *
     * @param ou
     * @return
     */
    public Employee setOu(List<String> ou) {
        this.ou = ou;
        return this;
    }

    /**
     * Получить Роли
     *
     * @return
     */
    public List<String> getRoles() {
        return roles;
    }

    /**
     * Указать Роли
     *
     * @param roles
     * @return
     */
    public Employee setRoles(List<String> roles) {
        this.roles = roles;
        return this;
    }

    /**
     * Получить Очереди
     *
     * @return
     */
    public List<String> getServices() {
        return services;
    }

    /**
     * указать Очереди
     *
     * @param services
     * @return
     */
    public Employee setServices(List<String> services) {
        this.services = services;
        return this;
    }

    /**
     * Получить Линии
     *
     * @return
     */
    public List<String> getTeams() {
        return teams;
    }

    /**
     * Указать линии
     *
     * @param teams
     * @return
     */
    public Employee setTeams(List<String> teams) {
        this.teams = teams;
        return this;
    }

    /**
     * Подключена ли телефония
     *
     * @return
     */
    public boolean isVoximplantEnabled() {
        return voximplantEnabled;
    }

    /**
     * указать флаг подключения телефонии
     *
     * @param voximplantEnabled
     * @return
     */
    public Employee setVoximplantEnabled(boolean voximplantEnabled) {
        this.voximplantEnabled = voximplantEnabled;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Employee employee = (Employee) o;
        if (this.alias != null) {
            if (!this.alias.equals(employee.alias)) {
                return false;
            }
        }
        if (this.title != null) {
            if (!this.title.equals(employee.title)) {
                return false;
            }
        }
        if (this.voximplantEnabled != null) {
            if (!this.voximplantEnabled == employee.voximplantEnabled) {
                return false;
            }
        }

        if (this.ou != null) {
            if (!this.ou.containsAll(employee.ou)) {
                return false;
            }
        }

        if (this.roles != null) {
            if (!this.roles.containsAll(employee.roles)) {
                return false;
            }
        }

        if (this.services != null) {
            if (!this.services.containsAll(employee.services)) {
                return false;
            }
        }

        if (this.teams != null) {
            if (!this.teams.containsAll(employee.teams)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(alias, title, ou, roles, services, teams, voximplantEnabled);
    }

    @Override
    public String toString() {
        return "Employee{" +
                "alias='" + alias + '\'' +
                ", title='" + title + '\'' +
                ", ou=" + ou +
                ", roles=" + roles +
                ", services=" + services +
                ", teams=" + teams +
                ", voximplantEnabled=" + voximplantEnabled +
                '}';
    }
}
