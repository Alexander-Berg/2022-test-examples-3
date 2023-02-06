package ru.yandex.market.pharmatestshop.domain.cart.buyer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Buyer {

   private Long uid;
   private String id;
   private String lastName;
   private String firstName;
   private String middleName;
   private String email;
   private String phone;


}
