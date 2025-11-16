using System.Collections.Generic;

namespace GameGuesserAPI.Models
{
    public class ComparisonResult
    {
        public bool Correct { get; set; }
        public Dictionary<string, bool> Matches { get; set; } = new Dictionary<string, bool>();
    }
}
