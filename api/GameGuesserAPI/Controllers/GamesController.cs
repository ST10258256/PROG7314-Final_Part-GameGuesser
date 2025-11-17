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
    var actualGame = await _gameService.GetGameByIdAsync(request.GameId);
    var guessedGame = await _gameService.GetGameByNameAsync(request.GuessName);

    if (actualGame == null || guessedGame == null)
        return NotFound("Actual or guessed game not found");

    var result = new ComparisonResult
    {
        Correct = actualGame.Name.Equals(guessedGame.Name, StringComparison.OrdinalIgnoreCase),
        Matches = new Dictionary<string, bool>()
        {
            { "Genre", actualGame.Genre == guessedGame.Genre },
            { "Platforms", actualGame.Platforms.SequenceEqual(guessedGame.Platforms) },
            { "ReleaseYear", actualGame.ReleaseYear == guessedGame.ReleaseYear },
            { "Developer", actualGame.Developer == guessedGame.Developer },
            { "Publisher", actualGame.Publisher == guessedGame.Publisher },
            { "Budget", actualGame.Budget == guessedGame.Budget },
            { "Saga", actualGame.Saga == guessedGame.Saga },
            { "POV", actualGame.POV == guessedGame.POV }
        }
    };

    return Ok(result);
}


}
