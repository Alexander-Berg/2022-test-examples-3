import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.NoPackage")
class AuthenticationManager {

    private final int timeToLive;
    Map<String, int[]> map = new HashMap<>();

    public AuthenticationManager(int timeToLive) {
        this.timeToLive = timeToLive;
    }

    public void generate(String tokenId, int currentTime) {
        map.put(tokenId, new int[]{currentTime, currentTime + timeToLive});
    }

    public void renew(String tokenId, int currentTime) {
        if (map.containsKey(tokenId) && map.get(tokenId)[1] > currentTime) {
            map.computeIfPresent(tokenId, (key, value) -> new int[]{currentTime, currentTime + timeToLive});
        }
    }

    public int countUnexpiredTokens(int currentTime) {
        map = map.entrySet().stream().filter(el -> currentTime < el.getValue()[1]).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return map.values().size();
    }
}

/**
 * Your AuthenticationManager object will be instantiated and called as such:
 * AuthenticationManager obj = new AuthenticationManager(timeToLive);
 * obj.generate(tokenId,currentTime);
 * obj.renew(tokenId,currentTime);
 * int param_3 = obj.countUnexpiredTokens(currentTime);
 */
