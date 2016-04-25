/*
        MIT License

        Copyright (c) [2016] [Brandon Garlock]

        Permission is hereby granted, free of charge, to any person obtaining a copy
        of this software and associated documentation files (the "Software"), to deal
        in the Software without restriction, including without limitation the rights
        to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
        copies of the Software, and to permit persons to whom the Software is
        furnished to do so, subject to the following conditions:

        The above copyright notice and this permission notice shall be included in all
        copies or substantial portions of the Software.

        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
        IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
        FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
        AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
        LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
        OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
        SOFTWARE.
*/

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.StringReader;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.File;
import java.lang.ClassLoader;

import fi.iki.elonen.NanoHTTPD;
import org.openqa.selenium.WebDriver;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

public class FirefoxConsoleExport extends NanoHTTPD {
    private WebDriver driver;
    private ArrayList<LogEntry> logEntries = new ArrayList<LogEntry>();
    public FirefoxConsoleExport(int port) throws IOException {
        super(port);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            ClassLoader classLoader = FirefoxConsoleExport.class.getClassLoader();
            DesiredCapabilities cap = new DesiredCapabilities();
            cap.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
            FirefoxProfile fp = new FirefoxProfile();
            File extensionToInstall =  new File(classLoader.getResource("firebug-2.0.16-fx.xpi").getFile());
            File extension2 = new File(classLoader.getResource("consoleExport-0.5b5.xpi").getFile());

            fp.addExtension(extensionToInstall);
            fp.addExtension(extension2);

            fp.setPreference("extensions.firebug.currentVersion", "2.0");
            fp.setPreference("extensions.firebug.console.enableSites", "true");
            fp.setPreference("extensions.firebug.net.enableSites", "true");
            fp.setPreference("extensions.firebug.script.enableSites", "true");
            fp.setPreference("extensions.firebug.allPagesActivation", "on");
            fp.setPreference("extensions.firebug.consoleexport.active", "true");
            fp.setPreference("extensions.firebug.consoleexport.serverURL", "http://127.0.0.1:9999");

            cap.setCapability(FirefoxDriver.PROFILE, fp);
            driver  = new FirefoxDriver(cap);

    }

    public WebDriver getDriver(){
        return driver;
    }

    public ArrayList<LogEntry> getLogEntries(){
        return logEntries;
    }


    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> xmlBody = new HashMap<String, String>();
        try {
            session.parseBody(xmlBody);
            String xml = xmlBody.get("postData") ;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            Element rootElement = document.getDocumentElement();
            String logType= getElementValue("class",rootElement);
            String logLevel = getElementValue("cat",rootElement);
            String logMessage = getElementValue("msg", rootElement);
            String url = getElementValue("href",rootElement);
            LogEntry logEntry = new LogEntry(logType,logLevel,logMessage,url);
            logEntries.add(logEntry);
       } catch (fi.iki.elonen.NanoHTTPD.ResponseException responseException) {
            System.err.println("Error generating http response.-"+responseException.getMessage());
        }
        catch (java.io.IOException ioException) {
            System.err.println("Did not get a response body from Client-"+ioException.getMessage());
        }
        catch (javax.xml.parsers.ParserConfigurationException xmlParseException){
            System.err.println("Unable to parse xml. ConsoleExport is not returning correct xml."+xmlParseException.getMessage());

        }
        catch(org.xml.sax.SAXException unexpectedXMLException){
            System.err.println("Unable to find msg element"+unexpectedXMLException.getMessage());
        }


        return newFixedLengthResponse("");
    }

    protected String getElementValue(String tagName, Element element) {
        NodeList list = element.getElementsByTagName(tagName);
        if (list != null && list.getLength() > 0) {
            NodeList subList = list.item(0).getChildNodes();

            if (subList != null && subList.getLength() > 0) {
                return subList.item(0).getNodeValue();
            }
        }

        return null;
    }
}