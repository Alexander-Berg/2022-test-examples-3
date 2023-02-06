package ru.yandex.market.antifraud.orders.tanking;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * @author dzvyagin
 */
@Data
public class TankAmmo {

    private String id;
    private String request;
    private Map<String, String> headers = new HashMap<>();
    private AmmoType ammoType = AmmoType.UNKNOWN;
    private String body;


    public boolean isEmpty(){
        return id == null;
    }

    public void setId(String id){
        this.id = id;
        this.ammoType = AmmoType.fromId(id);
    }


    public int getLength(){
        int textLength = 0;
//        textLength += id.length() + 1;  // + new string symbol
        textLength += request.length() +1;
        for (Map.Entry<String, String> entry : headers.entrySet()){
            textLength += entry.getKey().length();
            textLength += entry.getValue().length();
            textLength += 3; // + delimiter ": " and new string symbol
        }
        textLength += 2; // plus 2 strings delimiter
        if (body != null){
            textLength += body.length() +2;
        }
        return textLength;
    }

}
