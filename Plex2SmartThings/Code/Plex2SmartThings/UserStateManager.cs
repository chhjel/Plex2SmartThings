using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Web;
using System.Xml.Linq;
using System.Xml.XPath;

namespace Plex2SmartThings
{
    /// <summary>
    /// Manages a list of the current states per player
    /// </summary>
    class UserStateManager
    {
        public enum PlayStates { UNKNOWN = 0, PLAY = 1, PAUSE = 2, STOP = 3 }
        private List<InstancePlayState> PlayerStates = new List<InstancePlayState>();
        public List<string> ActivePlayerNames = new List<string>();

        public void ParsePlexResult(string rawXml)
        {
            ActivePlayerNames.Clear();

            //Loop through plex results
            try { 
                XDocument doc = null;
                doc = XDocument.Parse(rawXml);
                XElement e = doc.XPathSelectElement("/MediaContainer");
                foreach (XElement video in e.Elements())
                {
                    ParseVideoElement(video);
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine("Failed to parse xml from Plex: " + ex.Message);
                return;
            }

            //Now check for any expired events = stopped
            for (int i = 0; i < PlayerStates.Count; i++)
            {
                if (!ActivePlayerNames.Contains(PlayerStates[i].IPAdd))
                {
                    PlayerStates[i].OnStateRetrieved(PlayStates.STOP, PlayerStates[i].Type);
                    if (PlayerStates[i].RequestListRemoval)
                    {
                        PlayerStates[i].AbortThreads = true;
                        PlayerStates.RemoveAt(i);
                        i--;
                    }
                }
            }
        }




        private void ParseVideoElement(XElement video)
        {
            //Parse the raw data into a InstancePlayState object
            UserStateManager.InstancePlayState playerData = new UserStateManager.InstancePlayState();
            if (!playerData.Parse(video)) return;

            //Notify state update to existing data if any
            bool isInList = false;
            for (int i = 0; i < PlayerStates.Count; i++) {
                if (PlayerStates[i].IPAdd == playerData.IPAdd) {
                    isInList = true;
                    PlayerStates[i].OnStateRetrieved(playerData.CurrentState, playerData.Type);
                    break;
                }
            }

            //No existing player with that name => add it to list
            if (!isInList)
            {
                PlayerStates.Add(playerData);
                //Forced state change after we just added it, since the state is the same as when it was parsed
                playerData.OnStateRetrieved(playerData.CurrentState, playerData.Type, true);
            }

            //Add player to the active list
            ActivePlayerNames.Add(playerData.IPAdd);
        }

        public class InstancePlayState
        {
            public PlayStates CurrentState { get; set; }
            public string PlayerName { get; set; }
            public string UserName { get; set; }
            public string Type { get; set; } //Movie, episode etc. Whatever plex puts in the /Video/@type attribute
			public string IPAdd { get; set; } //IP Address of player

            public bool RequestListRemoval { get; private set; }

            /// <summary>
            /// Parses the raw data into this object.
            /// </summary>
            /// <param name="raw"></param>
            /// <returns>true if the parse was a success.</returns>
            public bool Parse(XElement video)
            {
                //Parse values
                UserName = video.Element("User").Attribute("title").Value;
                PlayerName = video.Element("Player").Attribute("title").Value;
                Type = video.Attribute("type").Value;
				IPAdd = video.Element("Player").Attribute("address").Value;

                if (!CanProcess(UserName, PlayerName, Type)) return false;

                //Parse state
                string stateStr = video.Element("Player").Attribute("state").Value;
                CurrentState = PlayStates.UNKNOWN;
                if (stateStr == "playing")
                {
                    CurrentState = PlayStates.PLAY;
                }
                else if (stateStr == "paused")
                {
                    CurrentState = PlayStates.PAUSE;
                }

                return true;
            }

            /// <summary>
            /// Checks if the state has changed and notifies the smartthings endpoint.
            /// </summary>
            /// <param name="state"></param>
            /// <param name="forced">Overrides the check vs previous state</param>
            public void OnStateRetrieved(PlayStates state, string mediaType, bool forced=false)
            {
				if (Program.DebugLevel >= 2) Console.WriteLine(UserName + "@'" + PlayerName + "' " + Type + "=> StateRetrieved(" + state.ToString() + ", " + mediaType + ")");

                //Something weird has happened.. => abort
                if (state == PlayStates.UNKNOWN) return;
                //No new state or type => abort unless it's a forced update
                else if (state == CurrentState && mediaType == Type && !forced) return;
                //We got a new state/media
                else
                {
                    CurrentState = state;
                    Type = mediaType;
                }

				if (Program.DebugLevel >= 1) Console.WriteLine(UserName + "@'" + PlayerName + "' (" + IPAdd + ") Changed state to: " + state.ToString() + ", mediaType is " + mediaType);

                //Select the correct endpoint
                Delay = Config.GetDelayFor(CurrentState, Type);

                if (Delay > 0)
                {
                    if (Program.DebugLevel >= 1) Console.WriteLine(" >Delay: " + Delay);
                }
                else
                {
                    string endpoint = CreateEndpointUrl();
                    Program.SendGetRequest(endpoint);

                    if (CurrentState == PlayStates.STOP) RequestListRemoval = true;
                }
                
                if (stateDelayThread == null)
                {
                    stateDelayThread = new Thread(() => ProcessStateDelay());
                    stateDelayThread.Start();
                }
            }

            public string GetFirstTenCharacters(string s)
            {
                // This says "If string s is less than 10 characters, return s.
                // Otherwise, return the first 10 characters of s."
                return (s.Length < 10) ? s : s.Substring(0, 10);
            }

            private Thread stateDelayThread;
            private int Delay;
            public bool AbortThreads = false;
            private void ProcessStateDelay()
            {
                while (!AbortThreads && !Program.Terminate)
                {
                    if (Delay >= 1)
                    {
                        Delay--;
                        if (Delay <= 0)
                        {
                            string endpoint = CreateEndpointUrl();
                            if (Program.DebugLevel >= 1) Console.WriteLine("Sent delayed state change: " + CurrentState.ToString());
                            Program.SendGetRequest(endpoint);

                            if (CurrentState == PlayStates.STOP) RequestListRemoval = true;
                        }
                    }

                    Thread.Sleep(1000);
                    if (Program.Terminate || AbortThreads) return;
                }
            }

            private string CreateEndpointUrl()
            {
                string endpoint = "";
                if (CurrentState == PlayStates.PLAY) endpoint = Config.EndpointUrl_OnPlay;
                else if (CurrentState == PlayStates.PAUSE) endpoint = Config.EndpointUrl_OnPause;
                else if (CurrentState == PlayStates.STOP) endpoint = Config.EndpointUrl_OnStop;

                string PlayerNameShort = this.GetFirstTenCharacters(PlayerName);

                //Prepare for first param, just in case there is already added some params in the url in the config file
                if (!endpoint.Contains("?")) endpoint += "?";
                else endpoint += "&";

                //Add the GET parameters to the request
                endpoint += "access_token=" + HttpUtility.UrlEncode(Config.Endpoint_AccessToken);
                endpoint += "&player=" + HttpUtility.UrlEncode(PlayerNameShort);
                endpoint += "&user=" + HttpUtility.UrlEncode(UserName);
                endpoint += "&type=" + HttpUtility.UrlEncode(Type);
				endpoint += "&ipadd=" + HttpUtility.UrlEncode(IPAdd);

                return endpoint;
            }

            /// <summary>
            /// Check that values are not blank, and check vs ignore/whitelists.
            /// </summary>
            /// <param name="user"></param>
            /// <param name="player"></param>
            /// <returns></returns>
			private bool CanProcess(string user, string player, string type)
            {
                //Both blank => not allowed
                if (String.IsNullOrEmpty(user) && String.IsNullOrEmpty(player)) return false;

                //Whitelist have top priority
                if (Config.WhiteListContains("user", user)) return true;
                else if (Config.WhiteListContains("player", player)) return true;
                else if (Config.WhiteListContains("type", type)) return true;

                if (Config.OnlyAllowWhitelisted) return false;

                //Check ignore list
                if (Config.IgnoreListContains("user", user)) return false;
                else if (Config.IgnoreListContains("player", player)) return false;
                else if (Config.IgnoreListContains("type", type)) return false;

                //Not on whitelist or ignorelist, and OnlyAllowWhitelisted is false
                return true;
            }

        }

    }
}
