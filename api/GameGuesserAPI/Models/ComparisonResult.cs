using System.Collections.Generic;

namespace GameGuesserAPI.Models
{
    public class ComparisonResult
    {
        public bool Correct { get; set; }
        public Dictionary<string, string> Matches { get; set; } = new Dictionary<string, string>();
    }
}
