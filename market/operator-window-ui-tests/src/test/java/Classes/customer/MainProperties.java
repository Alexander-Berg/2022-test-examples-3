package ui_tests.src.test.java.Classes.customer;


import java.util.List;
import java.util.Objects;

public class MainProperties {
    private String phone;
    private String email;
    private String fullName;
    private List<String> markers;
    private String statusAuthorization;
    private String registrationDate;
    private String uid;
    private String cashback;

    /**
     * Получить статус авторизации
     * @return
     */
    public String getStatusAuthorization() {
        return statusAuthorization;
    }

    /**
     * Указать статус авторизации
     * @param statusAuthorization
     * @return
     */
    public MainProperties setStatusAuthorization(String statusAuthorization) {
        this.statusAuthorization = statusAuthorization;
        return this;
    }

    /**
     * Получить маркеры клиента
     * @return
     */
    public List<String> getMarkers() {
        return markers;
    }

    /**
     * Задать маркеры клиента
     * @param markers маркеры клиента
     * @return
     */
    public MainProperties setMarkers(List<String> markers) {
        this.markers = markers;
        return this;
    }

    /**
     * Получить номер телефона
     * @return номер телефона
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Указать номер телефона
     * @param phone номер телефона
     * @return
     */
    public MainProperties setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    /**
     * Получить Email
     * @return email
     */
    public String getEmail() {
        return email.toLowerCase();
    }

    /**
     * Указать Email
     * @param email email
     * @return
     */
    public MainProperties setEmail(String email) {
        this.email = email;
        return this;
    }

    /**
     * Получить ФИО
     * @return ФИО
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * указать ФИО
     * @param fullName ФИО
     * @return
     */
    public MainProperties setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    /**
     * Получить дату регистрации
     */
    public String getRegistrationDate() {
        return registrationDate;
    }

    /**
     * Указать дату регистрации
     */
    public MainProperties setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
        return this;
    }

    /**
     * Получить uid
     */
    public String getUid() {
        return uid;
    }

    /**
     * Указать uid
     */
    public MainProperties setUid(String uid) {
        this.uid = uid;
        return this;
    }

    /**
     * Получить данные о кэшбэке
     * @return
     */
    public String getCashback() {
        return cashback;
    }

    /**
     * Задать данные о кэшбэке
     * @param cashback маркеры клиента
     * @return
     */
    public MainProperties setCashback(String cashback) {
        this.cashback = cashback;
        return this;
    }

    @Override
    public String toString() {
        return "MainProperties{" +
                "phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", markers=" + markers +
                ", statusAuthorization='" + statusAuthorization + '\'' +
                ", cashback='" + cashback + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object expectedObject) {
        if (this == expectedObject) return true;
        if (expectedObject == null || getClass() != expectedObject.getClass()) return false;
        MainProperties that = (MainProperties) expectedObject;
        if (this.email!=null){
            if (!that.email.toLowerCase().equals(this.email.toLowerCase())){
                return false;
            }
        }
        if (this.fullName!=null){
            if (!that.fullName.equals(this.fullName)){
                return false;
            }
        }

        if (this.phone!=null){
            if (!that.phone.equals(this.phone)){
                return false;
            }
        }
        if (this.markers!=null){
            if (that.markers.equals(this.markers)){
                return false;
            }
        }
        if (this.statusAuthorization!=null){
            if (!this.statusAuthorization.equals(that.statusAuthorization)){
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(phone, email, fullName);
    }
}
