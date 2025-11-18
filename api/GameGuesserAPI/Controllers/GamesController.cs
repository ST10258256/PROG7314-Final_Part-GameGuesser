using Microsoft.AspNetCore.Mvc;
using GameGuesserAPI.Models;
using GameGuesserAPI.Services;

[ApiController]
[Route("api/[controller]")]
public class GamesController : ControllerBase
{
    private readonly GameService _gameService;
    private static Dictionary<string, int> GameClueIndex = new Dictionary<string, int>();


    public GamesController(GameService gameService)
    {
        _gameService = gameService;
    }

    [HttpGet("random")]
    public async Task<IActionResult> GetRandomGame()
    {
        var game = await _gameService.GetRandomGameAsync();
        return Ok(new
        {
            game.Id,
            game.Name,
            game.CoverImageUrl,
            game.Keywords
        });
    }

    [HttpPost("guess")]
    public async Task<IActionResult> SubmitGuess(string gameId, string guess)
    {
        var game = await _gameService.GetGameByIdAsync(gameId);
        if (game == null) return NotFound("Game not found.");

        if (guess.Equals(game.Name, StringComparison.OrdinalIgnoreCase))
        {
            if (GameClueIndex.ContainsKey(gameId))
                GameClueIndex[gameId] = 0;

            return Ok(new { correct = true, message = "Correct guess!" });
        }

        int clueIndex = GameClueIndex.ContainsKey(gameId) ? GameClueIndex[gameId] : 0;
        string clue = clueIndex < game.Clues.Count ? game.Clues[clueIndex] : "No more clues available.";
        GameClueIndex[gameId] = clueIndex + 1;

        return Ok(new { correct = false, hint = clue });
    }

    [HttpGet]
    public async Task<IActionResult> GetAllGames()
    {
        var gameNames = await _gameService.GetAllGameNamesAsync();
        return Ok(gameNames);
    }

    [HttpGet("{id}")]
    public async Task<IActionResult> GetGameById(string id)
    {
        var game = await _gameService.GetGameByIdAsync(id);
        if (game == null) return NotFound();
        return Ok(game);
    }

    [HttpGet("full")]
    public async Task<IActionResult> GetAllGamesFull()
    {
        try
        {
            var games = await _gameService.GetAllGamesAsync();
            return Ok(games);
        }
        catch (Exception ex)
        {
            return StatusCode(500, ex.Message);
        }
    }

[HttpPost("compare")]
public async Task<IActionResult> CompareGame([FromBody] CompareRequest request)
{
    // Get the actual game by ID
    var actualGame = await _gameService.GetGameByIdAsync(request.GameId);
    if (actualGame == null)
        return NotFound("Actual game not found");

    // Initialize the result
    var result = new ComparisonResult
    {
        Correct = actualGame.Name.Equals(request.GuessName, StringComparison.OrdinalIgnoreCase),
        Matches = new Dictionary<string, string>()
    };

    // Helper to compare string lists
    string CompareLists(List<string> actual, List<string> guess)
    {
        var actualLower = actual.Select(a => a.ToLower()).ToList();
        var guessLower = guess.Select(g => g.ToLower()).ToList();

        if (guessLower.All(actualLower.Contains) && actualLower.All(guessLower.Contains))
            return "exact";
        else if (guessLower.Any(actualLower.Contains))
            return "partial";
        else
            return "none";
    }

    if (request.GuessedReleaseYear.HasValue)
    {
        int guessedYear = request.GuessedReleaseYear.Value;

        if (guessedYear == actualGame.ReleaseYear)
            result.Matches["ReleaseYear"] = "exact";
        else if (guessedYear > actualGame.ReleaseYear)
            result.Matches["ReleaseYear"] = "higher";
        else
            result.Matches["ReleaseYear"] = "lower";
    }
    else
    {
        result.Matches["ReleaseYear"] = "none"; // no year info
    }

    // ---- Other comparisons ----
    result.Matches["Genre"] = actualGame.Genre.Equals(request.GuessName, StringComparison.OrdinalIgnoreCase) ? "exact" : "none";
    result.Matches["Platforms"] = CompareLists(actualGame.Platforms, new List<string> { request.GuessName }); // adapt as needed
    result.Matches["Developer"] = actualGame.Developer.Equals(request.GuessName, StringComparison.OrdinalIgnoreCase) ? "exact" : "none";
    result.Matches["Publisher"] = actualGame.Publisher.Equals(request.GuessName, StringComparison.OrdinalIgnoreCase) ? "exact" : "none";
    result.Matches["Budget"] = actualGame.Budget.Equals(request.GuessName, StringComparison.OrdinalIgnoreCase) ? "exact" : "none";
    result.Matches["Saga"] = actualGame.Saga.Equals(request.GuessName, StringComparison.OrdinalIgnoreCase) ? "exact" : "none";
    result.Matches["POV"] = actualGame.POV.Equals(request.GuessName, StringComparison.OrdinalIgnoreCase) ? "exact" : "none";

    return Ok(result);
}




}
