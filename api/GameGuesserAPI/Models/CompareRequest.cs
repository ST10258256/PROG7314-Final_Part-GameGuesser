namespace GameGuesserAPI.Models
{
    public class CompareRequest
    {
        public string GameId { get; set; } = null!;
        public string GuessName { get; set; } = null!;
    }
}
