package ru.yandex.market.volva.tank;

import java.io.PrintWriter;
import java.util.List;

import ru.yandex.market.volva.tank.generators.RequestGenerator;
import ru.yandex.market.volva.tank.generators.SimpleRequestGenerator;
import ru.yandex.market.volva.tank.headers.AcceptEncodingHeaderFactory;
import ru.yandex.market.volva.tank.headers.AcceptHeaderFactory;
import ru.yandex.market.volva.tank.headers.AcceptLanguageFactory;
import ru.yandex.market.volva.tank.headers.ContentTypeHeaderFactory;
import ru.yandex.market.volva.tank.headers.HostHeaderFactory;


/**
 * @author dzvyagin
 */
public class AmmoGenerator {

    private static final int BULLETS_TO_GENERATE = 10_000;

    public static void main(String[] args) {
        String fileName = "volva_ammo.txt";
        new AmmoGenerator().generate(fileName);
    }



    public void generate(String file){
        RequestGenerator generator = new SimpleRequestGenerator(List.of(
                new AcceptEncodingHeaderFactory(),
                new AcceptLanguageFactory(),
                new AcceptHeaderFactory(),
                new HostHeaderFactory(),
                new ContentTypeHeaderFactory()
        ));
        try (PrintWriter printWriter = new PrintWriter(file, "UTF-8")){
            for (int i = 0; i < BULLETS_TO_GENERATE; i++){
                TankAmmo ammo = generator.generate();
                printWriter.println(ammo.getLength() + ammo.getId());
                printWriter.println(ammo.getRequest());
                ammo.getHeaders().forEach((k,v) -> printWriter.println(k + ": " + v));
                if (ammo.getBody() != null){
                    printWriter.println();
                    printWriter.println(ammo.getBody());
                }
                printWriter.println();
                printWriter.println();
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        System.out.println(file);
    }
}
