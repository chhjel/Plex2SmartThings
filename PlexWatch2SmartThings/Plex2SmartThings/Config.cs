using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml.Linq;
using System.Xml.XPath;

namespace Plex2SmartThings
{
    static class Config
    {
        public static int CheckInterval { get; private set; }

        public static string PlexStatusUrl { get; private set; }

        public static string Endpoint_AccessToken { get; private set; }
        public static string EndpointUrl_OnPlay { get; private set; }
        public static string EndpointUrl_OnPause { get; private set; }
        public static string EndpointUrl_OnStop { get; private set; }

        public static Dictionary<string, string> IgnoreList { get; private set; }

        public static bool OnlyAllowWhitelisted { get; private set; }
        public static Dictionary<string, string> WhiteList { get; private set; }

        public static List<DelayItem> Delays { get; private set; }
        public class DelayItem
        {
            public string Type { get; set; }
            public string Event { get; set; }
            public int Delay { get; set; }
            public UserStateManager.PlayStates PlayState = UserStateManager.PlayStates.UNKNOWN;

            public DelayItem(string typ, string evnt, int delay)
            {
                Type = typ;
                Event = evnt;
                Delay = delay;

                if (Event == "play") PlayState = UserStateManager.PlayStates.PLAY;
                else if (Event == "pause") PlayState = UserStateManager.PlayStates.PAUSE;
                else if (Event == "stop") PlayState = UserStateManager.PlayStates.STOP;
            }
        }

        /// <summary>
        /// Catch any exception on config load and show the error if any.
        /// </summary>
        /// <returns>True if successfully loaded</returns>
        public static bool TryLoad()
        {
            try
            {
                Load();
                return true;
            }
            catch (Exception ex)
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine("Error in config: "+ex.Message);
                Console.ReadLine();
                return false;
            }
        }

        private static void Load()
        {
            XDocument doc = null;
            doc = XDocument.Load("config.config");
            XElement eConfig = doc.XPathSelectElement("/config");

            //plexCheck
            XElement ePlexCheck = eConfig.Element("plexCheck");
            CheckInterval = int.Parse(ePlexCheck.Attribute("checkInterval").Value);
            PlexStatusUrl = ePlexCheck.Attribute("plexStatusUrl").Value;

            //smartThingsEndpoints
            XElement eSTE = eConfig.Element("smartThingsEndpoints");
            Endpoint_AccessToken = eSTE.Attribute("accessToken").Value;
            EndpointUrl_OnPlay = eSTE.Attribute("onPlay").Value;
            EndpointUrl_OnPause = eSTE.Attribute("onPause").Value;
            EndpointUrl_OnStop = eSTE.Attribute("onStop").Value;

            //Custom delays
            Delays = new List<DelayItem>();
            XElement eDelayList = eConfig.Element("delays");
            foreach (XElement item in eDelayList.Elements())
            {
                Delays.Add(new DelayItem(
                    item.Attribute("type").Value,
                    item.Attribute("event").Value,
                    int.Parse(item.Attribute("delay").Value))
                );
            }

            //ignoreList
            IgnoreList = new Dictionary<string, string>();
            XElement eIgnoreList = eConfig.Element("ignoreList");
            foreach (XElement item in eIgnoreList.Elements())
            {
                IgnoreList.Add(item.Attribute("type").Value, item.Attribute("value").Value);
            }

            //whiteList
            WhiteList = new Dictionary<string, string>();
            XElement eWhiteList = eConfig.Element("whiteList");
            OnlyAllowWhitelisted = (eWhiteList.Attribute("onlyAllowWhitelisted").Value.ToLower().Trim() == "true");
            foreach (XElement item in eIgnoreList.Elements())
            {
                WhiteList.Add(item.Attribute("type").Value, item.Attribute("value").Value);
            }
        }

        public static int GetDelayFor(Plex2SmartThings.UserStateManager.PlayStates state, string mediaType)
        {
            for (int i = 0; i < Delays.Count; i++)
            {
                if (Delays[i].PlayState == state && Delays[i].Type == mediaType) return Delays[i].Delay;
            }
            return 0;
        }

        public static bool IgnoreListContains(string type, string value)
        {
            if (!IgnoreList.ContainsKey(type)) return false;
            else return (IgnoreList[type] == value);
        }

        public static bool WhiteListContains(string type, string value)
        {
            if (!WhiteList.ContainsKey(type)) return false;
            else return (WhiteList[type] == value);
        }

    }
}
