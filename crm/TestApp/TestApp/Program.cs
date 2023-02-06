using System.Reflection;
using TestApp;

Console.WriteLine("very important feature");
Console.WriteLine("hotfix!");
if (args.Length > 0 && args.Contains("--version"))
{
    Console.WriteLine("Version: " + Assembly.GetExecutingAssembly().GetName().Version);
    return;
}

Console.WriteLine("press ctrl+c to exit");

while (true)
{
    Console.WriteLine();
    Console.Write("Write 'ping': ");
    var input = Console.ReadLine();
    Console.WriteLine(Pinger.Pong(input));
}
