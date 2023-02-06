package toolkit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class OperationsHelper {
    private OperationsHelper() {
    }

    public static boolean checkMultiplyTimes(Runnable func, int count) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < count; i++) {
                futures.add(CompletableFuture.runAsync(func));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            return true;
        } catch (ExecutionException | InterruptedException e) {
            return false;
        }
    }
}
