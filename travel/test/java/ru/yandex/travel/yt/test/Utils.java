package ru.yandex.travel.yt.test;

public class Utils {
    public static <T extends Throwable> T assertThrows(Class<T> expectedException, Runnable runnable) {
        String message = null;
        try {
            runnable.run();
        }
        catch (Throwable t) {
            if (expectedException.isInstance(t)) {
                return (T)t;
            }
            else {
                message = String.format("Unexpected exception: expected %s, got %s",
                        expectedException.getSimpleName(),
                        t.getClass().getSimpleName());
            }
        }
        if (message == null) {
            message = "Expected exception was not thrown";
        }
        throw new AssertionError(message);
    }
}
