package tools;

import java.time.LocalDateTime;

public class Date {

    public static String getCurrentDateTime() {
        return LocalDateTime.now().toString();
    }
}
