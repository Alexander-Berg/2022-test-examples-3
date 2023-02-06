import java.lang.reflect.Field;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

// TODO: 21/02/17 !!!! Временно тут, для определения кривых сетей из-за бага jvm под mac
// TODO: 21/02/17 !!!! Выпилить
// More info: https://shinderuk.at.yandex-team.ru/36
//CHECKSTYLE:OFF
public class TestNetwork {
    //sudo ifconfig awdl0 down
    public static void main(String[] args) throws Exception {
        List<NetworkInterface> netins = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface netin : netins) {
            System.out.println(netin + " " + netin.getIndex());
        }

        Field f = NetworkInterface.class.getDeclaredField("defaultIndex");
        f.setAccessible(true);
        System.out.println("defaultIndex = " + f.get(NetworkInterface.class));
    }
}
