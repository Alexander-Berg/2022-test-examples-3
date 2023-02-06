namespace TestApp.Tests;

[TestFixture]
[Parallelizable(ParallelScope.All)]
public class PingerTests
{
    public class PingTests
    {
        [Test]
        public void Should_ReturnPong_When_InputIsPing()
        {
            Assert.That(Pinger.Pong("ping"), Is.EqualTo("pong"));
        }

        [TestCase("", ExpectedResult = "not pong")]
        [TestCase(null, ExpectedResult = "not pong")]
        [TestCase("asdasdas", ExpectedResult = "not pong")]
        public string Should_ReturnNotPong_When_InputIsNotPing(string? input) => Pinger.Pong(input);
    }
}
