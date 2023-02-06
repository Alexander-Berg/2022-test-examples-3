package dto.requests.checkouter.checkout;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Buyer {
    private String lastName;
    private String firstName;
    private String middleName;
    private String phone;
    private String email;
    private String ip;
    private boolean dontCall;
    private String uuid;
    private Long businessBalanceId;
    private Long type;

    public Buyer(String phone, String uuid) {
        this.lastName = "последнееимя";
        this.firstName = "первоеимя";
        this.middleName = "среднееимя";
        this.phone = phone;
        this.email = "ymail@y.mail";
        this.ip = "8.8.8.8";
        this.dontCall = true;
        this.uuid = uuid;
        this.businessBalanceId = null;
        this.type = null;
    }
}
