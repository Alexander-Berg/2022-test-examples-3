namespace TestApp;

public class Pinger
{
    public static string Pong(string? ping) => ping == "ping" ? "pong" : "not pong";
}
