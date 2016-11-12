using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net;
using System.Security.Permissions;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Web;
using System.Xml.Linq;
using System.Xml.XPath;

namespace Plex2SmartThings
{
    class Program
    {
        public static int DebugLevel = 0;
        private static UserStateManager StateManager;

        static void Main(string[] args)
        {
            

            //TODO: move into service instead of console app?
            Console.Title = "Plex2SmartThings";
            Console.ForegroundColor = ConsoleColor.Cyan;
            Console.WriteLine("---Plex2SmartThings V4.0---");

            Console.ForegroundColor = ConsoleColor.Green;
            
            Console.WriteLine("-Loading config..");
            if (!Config.TryLoad()) return;
            Console.WriteLine(" >Loaded");

            //Set debug level
            DebugLevel = Config.ConsoleDebugLevel;
            Console.WriteLine("-DebugLevel is set to " + DebugLevel);

            Console.WriteLine("-Connecting to plex.. Please stand by..");

            StateManager = new UserStateManager();

            CheckPlexState();

            //Thread thread = new Thread(() => CheckPlexState());
            //thread.Start();

            Console.ReadLine();
            Terminate = true;
            //thread.Abort();

            Console.ForegroundColor = ConsoleColor.Red;
            string terminationMessage = "\n\nTerminating Process..";
            for (int i = 0; i < terminationMessage.Length; i++)
            {
                Console.Write(terminationMessage.Substring(i,1));
                Thread.Sleep(33);
            }
            Thread.Sleep(500);
            System.Environment.Exit(0);
        }

        private static bool notifiedFirst = false;
        public static bool Terminate = false;
        private static void CheckPlexState()
        {
            while (!Terminate)
            {
                if (DebugLevel >= 2) Console.WriteLine("Checking..");

                string raw = SendGetRequest(Config.PlexStatusUrl);
                if (Terminate || raw == null) return;
                StateManager.ParsePlexResult(raw);
                if (Terminate) return;

                //The first check takes a few seconds, notify when it's done
                if (!notifiedFirst)
                {
                    notifiedFirst = true;
                    Console.WriteLine(" >Ready and monitoring.");

                    Console.ForegroundColor = ConsoleColor.Red;
                    Console.WriteLine("\n (Click enter to quit)");
                    Console.ForegroundColor = ConsoleColor.Green;
                }

                System.Threading.Thread.Sleep(TimeSpan.FromSeconds(Config.CheckInterval));
                if (Terminate) return;
            }
        }

        public static string SendGetRequest(string url)
        {
            try
            {
                using (WebClient client = new WebClient())
                {
                    if (DebugLevel >= 2) Console.WriteLine("SendGetRequest: " + url);
                    string result = client.DownloadString(url);
                    if (DebugLevel == 2) Console.WriteLine("Result: " + result);
                    if (DebugLevel >= 2) Debug.WriteLine(result);
                    return result;
                }
            }
            catch (Exception ex)
            {
                if (!Terminate && DebugLevel >= 1) { 
                    Console.WriteLine("Failed to SendGetRequest: " + ex.Message);
                    if (ex.InnerException != null) Console.WriteLine(" -" + ex.InnerException.Message);
                }
                return null;
            }
        }
    }
}
